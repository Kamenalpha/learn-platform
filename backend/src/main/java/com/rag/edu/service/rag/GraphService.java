package com.rag.edu.service.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.common.BizException;
import com.rag.edu.dto.KbDtos.GraphData;
import com.rag.edu.dto.KbDtos.GraphEdge;
import com.rag.edu.dto.KbDtos.GraphNode;
import com.rag.edu.entity.Course;
import com.rag.edu.entity.DocChunk;
import com.rag.edu.mapper.CourseMapper;
import com.rag.edu.mapper.DocChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识图谱:用大模型从课程分块中抽取知识点关键词,
 * 以关键词出现频次构建节点,同一分块内共同出现构建边,结果缓存于 Redis。
 */
@Slf4j
@Service
public class GraphService {

    private static final String PROMPT = """
            你是数字媒体技术专业的助教。下面编号列出了某课程的部分知识块文本,
            请为每个知识块抽取2-4个最重要的知识点关键词(专业术语,2-8个字)。
            只输出 JSON,格式:{"chunks":[{"id":1,"keywords":["词1","词2"]}, ...]},不要输出任何其他内容。
            """;

    private static final int MAX_CHUNKS = 60;          // 参与构建的最大分块数
    private static final int MAX_NODES = 30;           // 图谱节点上限
    private static final int MAX_EDGES = 80;           // 图谱边上限
    private static final Duration CACHE_TTL = Duration.ofHours(2);

    private final CourseMapper courseMapper;
    private final DocChunkMapper chunkMapper;
    private final ChatClient chatClient;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public GraphService(CourseMapper courseMapper, DocChunkMapper chunkMapper,
                        ChatModel chatModel, StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.courseMapper = courseMapper;
        this.chunkMapper = chunkMapper;
        this.chatClient = ChatClient.builder(chatModel).build();
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public GraphData build(Long courseId, boolean refresh) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        String cacheKey = "rag:graph:" + courseId;
        if (!refresh) {
            String cached = redis.opsForValue().get(cacheKey);
            if (cached != null) {
                return deserialize(cached);
            }
        }

        List<DocChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<DocChunk>()
                        .inSql(DocChunk::getDocId,
                               "SELECT doc_id FROM course_document WHERE course_id = " + courseId)
                        .last("LIMIT " + MAX_CHUNKS));
        if (chunks.isEmpty()) {
            throw new BizException("该课程暂无已解析的知识块,请先上传课件");
        }

        // 每块编号送入大模型抽取关键词(单次调用,控制上下文长度)
        StringBuilder material = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            material.append(i + 1).append(". ")
                    .append(abbreviate(chunks.get(i).getContent(), 200)).append('\n');
        }
        String raw;
        try {
            raw = chatClient.prompt().system(PROMPT).user(material.toString()).call().content();
        } catch (Exception e) {
            throw new BizException("大模型调用失败: " + e.getMessage());
        }

        Map<String, Integer> freq = new LinkedHashMap<>();
        List<List<String>> perChunk = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(stripFences(raw));
            JsonNode arr = root.path("chunks");
            for (JsonNode node : arr) {
                List<String> keywords = new ArrayList<>();
                node.path("keywords").forEach(k -> {
                    String kw = k.asText().trim();
                    if (!kw.isEmpty() && kw.length() <= 20) {
                        keywords.add(kw);
                        freq.merge(kw, 1, Integer::sum);
                    }
                });
                perChunk.add(keywords);
            }
        } catch (Exception e) {
            throw new BizException("知识点抽取结果解析失败,请重试");
        }

        // 节点:按频次取前 N;边:同一分块内关键词共现
        List<String> top = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(MAX_NODES).map(Map.Entry::getKey).toList();
        List<GraphNode> nodes = top.stream()
                .map(k -> new GraphNode(k, k, freq.get(k)))
                .toList();
        Map<String, Integer> nodeSet = new HashMap<>();
        top.forEach(k -> nodeSet.put(k, 1));

        Map<String, Integer> edgeMap = new HashMap<>();
        for (List<String> keywords : perChunk) {
            for (int i = 0; i < keywords.size(); i++) {
                for (int j = i + 1; j < keywords.size(); j++) {
                    String a = keywords.get(i), b = keywords.get(j);
                    if (a.equals(b) || !nodeSet.containsKey(a) || !nodeSet.containsKey(b)) {
                        continue;
                    }
                    String key = a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
                    edgeMap.merge(key, 1, Integer::sum);
                }
            }
        }
        List<GraphEdge> edges = edgeMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(MAX_EDGES)
                .map(e -> {
                    String[] parts = e.getKey().split("\\|");
                    return new GraphEdge(parts[0], parts[1], e.getValue());
                })
                .toList();

        GraphData graph = new GraphData(nodes, edges);
        redis.opsForValue().set(cacheKey, serialize(graph), CACHE_TTL);
        return graph;
    }

    private static String stripFences(String s) {
        int start = s.indexOf('{');
        int end = s.lastIndexOf('}');
        return start >= 0 && end > start ? s.substring(start, end + 1) : s;
    }

    private String serialize(GraphData graph) {
        try {
            return objectMapper.writeValueAsString(graph);
        } catch (Exception e) {
            return "{}";
        }
    }

    private GraphData deserialize(String json) {
        try {
            return objectMapper.readValue(json, GraphData.class);
        } catch (Exception e) {
            return new GraphData(List.of(), List.of());
        }
    }

    private static String abbreviate(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max));
    }
}
