package com.rag.edu.service.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.common.BizException;
import com.rag.edu.dto.ChatDtos.AskResp;
import com.rag.edu.dto.ChatDtos.Source;
import com.rag.edu.dto.KbDtos.KbConfig;
import com.rag.edu.entity.DocChunk;
import com.rag.edu.entity.QaRecord;
import com.rag.edu.mapper.DocChunkMapper;
import com.rag.edu.mapper.QaRecordMapper;
import com.rag.edu.service.KbConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 问答链路:问题向量化检索 -> Top-K召回 -> 组装上下文 -> 大模型生成 -> 引用溯源
 */
@Slf4j
@Service
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            你是“数媒课程知识库助手”,服务于数字媒体技术专业的学生,回答必须严格依据给出的知识上下文。
            规则:
            1. 只使用【知识上下文】中的内容回答问题,不要编造;
            2. 回答中在对应结论处标注引用编号,如 [1]、[2],编号与上下文中来源一一对应;
            3. 若上下文不足以回答,请明确说明“知识库中暂无相关内容”,并给出学习建议;
            4. 回答使用简体中文,条理清晰,可用列表分点;涉及概念时先给定义,再展开解释。
            """;

    private static final int HISTORY_TURNS = 3;      // 携带的最近对话轮数
    private static final long HISTORY_TTL_HOURS = 24;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final KbConfigService kbConfigService;
    private final StringRedisTemplate redis;
    private final QaRecordMapper qaRecordMapper;
    private final DocChunkMapper chunkMapper;
    private final ObjectMapper objectMapper;

    public ChatService(ChatModel chatModel, VectorStore vectorStore, KbConfigService kbConfigService,
                       StringRedisTemplate redis, QaRecordMapper qaRecordMapper,
                       DocChunkMapper chunkMapper, ObjectMapper objectMapper) {
        this.chatClient = ChatClient.builder(chatModel).build();
        this.vectorStore = vectorStore;
        this.kbConfigService = kbConfigService;
        this.redis = redis;
        this.qaRecordMapper = qaRecordMapper;
        this.chunkMapper = chunkMapper;
        this.objectMapper = objectMapper;
    }

    public AskResp ask(Long userId, String sessionId, String question) {
        long start = System.currentTimeMillis();
        KbConfig cfg = kbConfigService.get();

        // 1. 相似度检索:问题 -> 向量,召回 Top-K 知识块
        List<Document> hits = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(cfg.topK() == null ? 5 : cfg.topK())
                .similarityThreshold(cfg.similarityThreshold() == null ? 0.5 : cfg.similarityThreshold())
                .build());
        if (hits == null) {
            hits = List.of();
        }

        // 2. 组装引用来源与知识上下文
        List<Source> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        int no = 1;
        for (Document hit : hits) {
            Map<String, Object> meta = hit.getMetadata();
            Long docId = parseLong(meta.get("docId"));
            Integer page = parsePage(meta.get("page"));
            String text = hit.getText() == null ? "" : hit.getText();
            sources.add(new Source(docId, str(meta.get("docTitle")), page,
                    findChunkId(docId, meta.get("vectorId")), hit.getScore(), abbreviate(text, 200)));
            context.append('[').append(no++).append("] ")
                    .append(str(meta.get("docTitle")))
                    .append(page != null ? "(第" + page + "页)" : "")
                    .append('\n').append(text).append("\n\n");
        }

        // 3. 携带最近多轮对话历史(Redis),支持上下文追问
        String history = loadHistory(sessionId);
        String userMessage = (history.isEmpty() ? "" : "【对话历史】\n" + history + "\n")
                + "【知识上下文】\n" + (context.isEmpty() ? "(无)" : context)
                + "【问题】" + question + "\n请依据以上规则回答。";

        // 4. 大模型生成
        String answer;
        try {
            answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT + (cfg.promptSuffix() == null ? "" : "\n" + cfg.promptSuffix()))
                    .user(userMessage)
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("大模型调用失败", e);
            throw new BizException("大模型调用失败,请检查 API Key 与网络: " + e.getMessage());
        }

        // 5. 保存对话上下文(Redis)与问答记录(MySQL)
        saveHistory(sessionId, question, answer);
        long elapsed = System.currentTimeMillis() - start;
        QaRecord record = new QaRecord();
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setQuestion(question);
        record.setAnswer(answer);
        record.setReference(toJson(sources));
        record.setElapsedMs(elapsed);
        qaRecordMapper.insert(record);

        return new AskResp(record.getRecordId(), answer, sources, elapsed);
    }

    private String loadHistory(String sessionId) {
        List<String> list = redis.opsForList().range(historyKey(sessionId), -HISTORY_TURNS * 2L, -1);
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String item : list) {
            try {
                Map<?, ?> map = objectMapper.readValue(item, Map.class);
                boolean isUser = "user".equals(map.get("role"));
                sb.append(isUser ? "用户: " : "助手: ").append(map.get("content")).append('\n');
            } catch (Exception ignored) {
            }
        }
        return sb.toString();
    }

    private void saveHistory(String sessionId, String question, String answer) {
        String key = historyKey(sessionId);
        try {
            redis.opsForList().rightPush(key, objectMapper.writeValueAsString(
                    Map.of("role", "user", "content", question)));
            redis.opsForList().rightPush(key, objectMapper.writeValueAsString(
                    Map.of("role", "assistant", "content", answer)));
            redis.opsForList().trim(key, -HISTORY_TURNS * 2L, -1);
            redis.expire(key, Duration.ofHours(HISTORY_TTL_HOURS));
        } catch (Exception e) {
            log.warn("保存对话上下文失败: {}", e.getMessage());
        }
    }

    private Long findChunkId(Long docId, Object vectorId) {
        if (docId == null || vectorId == null) {
            return null;
        }
        DocChunk chunk = chunkMapper.selectOne(new LambdaQueryWrapper<DocChunk>()
                .eq(DocChunk::getDocId, docId)
                .eq(DocChunk::getVectorId, vectorId.toString())
                .last("LIMIT 1"));
        return chunk == null ? null : chunk.getChunkId();
    }

    private String toJson(List<Source> sources) {
        try {
            return objectMapper.writeValueAsString(sources);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String historyKey(String sessionId) {
        return "rag:chat:hist:" + sessionId;
    }

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    private static Long parseLong(Object o) {
        try {
            return o == null ? null : Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parsePage(Object o) {
        String s = str(o);
        return s.isEmpty() ? null : Integer.valueOf(s);
    }

    private static String abbreviate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
