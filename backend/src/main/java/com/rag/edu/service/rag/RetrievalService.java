package com.rag.edu.service.rag;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.common.BizException;
import com.rag.edu.dto.KbDtos.KbConfig;
import com.rag.edu.mapper.DocChunkMapper;
import com.rag.edu.service.CourseAccessService;
import com.rag.edu.config.RagProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 统一检索入口:向量召回(Chroma) + 关键词召回(MySQL ngram 全文索引) 双路,
 * RRF 倒数排名融合,可选 BGE-Reranker(兼容 /v1/rerank)精排。
 * 问答(chat)、出题(exam)共用,保证检索策略改动只动这一处。
 */
@Slf4j
@Service
public class RetrievalService {

    /** RRF 融合常数(只按名次不看原始分数,避免向量分与BM25分量纲不统一) */
    private static final int RRF_K = 60;
    /** 单路粗召回上限 */
    private static final int LEG_LIMIT = 20;

    private final VectorStore vectorStore;
    private final DocChunkMapper chunkMapper;
    private final CourseAccessService courseAccessService;
    private final RagProperties props;
    private final ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    public RetrievalService(VectorStore vectorStore, DocChunkMapper chunkMapper,
                            CourseAccessService courseAccessService, RagProperties props,
                            ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.chunkMapper = chunkMapper;
        this.courseAccessService = courseAccessService;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    /** 检索入口:双路召回 -> 权限过滤 -> RRF 融合 -> (可选)重排 -> 截断 topK */
    public List<Document> retrieve(String query, List<Long> courseIds, Long userId, KbConfig cfg) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        int topK = cfg.topK() == null ? 5 : cfg.topK();
        double threshold = cfg.similarityThreshold() == null ? 0.5 : cfg.similarityThreshold();
        boolean rerank = Boolean.TRUE.equals(cfg.rerankEnabled())
                && props.getRerankApiKey() != null && !props.getRerankApiKey().isBlank();

        // 1. 向量路(语义,保持原有相似度阈值语义;失败即阻止全库回退)
        List<Document> vectorHits;
        try {
            String expr = "courseId in ['" + courseIds.stream()
                    .map(String::valueOf).collect(java.util.stream.Collectors.joining("', '")) + "']";
            vectorHits = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query).topK(LEG_LIMIT).similarityThreshold(threshold)
                    .filterExpression(expr).build());
        } catch (Exception e) {
            log.error("限定课程向量检索失败,已阻止全库回退", e);
            throw new BizException("知识库检索失败,请稍后重试");
        }
        if (vectorHits == null) {
            vectorHits = List.of();
        }

        // 2. 关键词路(MySQL ngram;索引缺失时降级为仅向量)
        List<Map<String, Object>> keywordRows = List.of();
        try {
            keywordRows = chunkMapper.keywordSearch(query, courseIds, LEG_LIMIT);
        } catch (Exception e) {
            log.warn("关键词检索不可用(未建全文索引?),降级为仅向量检索: {}", e.getMessage());
        }
        if (keywordRows == null) {
            keywordRows = List.of();
        }

        // 3. 权限过滤(资料级召回后校验,越权内容不进入后续任何阶段)
        List<Document> vectorOk = filterAllowed(vectorHits, userId);
        List<KeywordHit> keywordOk = new ArrayList<>();
        for (Map<String, Object> row : keywordRows) {
            Long docId = toLong(row.get("resourceId"));
            if (docId != null && courseAccessService.canReadResource(docId, userId)) {
                keywordOk.add(new KeywordHit(docId,
                        toLong(row.get("chunkId")), str(row.get("docTitle")),
                        toInt(row.get("pageNum")), str(row.get("vectorId")),
                        str(row.get("content")), toDouble(row.get("kwScore"))));
            }
        }

        // 4. RRF 融合:同一分块在两路各得一个名次,名次靠前则融合分高
        Map<String, RankedDoc> fused = new LinkedHashMap<>();
        rankInto(fused, vectorOk.stream().map(d -> (Rankable) VectorRank.of(d)).toList());
        rankInto(fused, keywordOk.stream().map(k -> (Rankable) k).toList());
        List<RankedDoc> ordered = new ArrayList<>(fused.values());
        ordered.sort((a, b) -> Double.compare(b.rrfScore(), a.rrfScore()));

        // 5. 可选重排精排(失败降级为融合顺序,不阻断问答)
        List<RankedDoc> finalDocs = ordered;
        if (rerank && !ordered.isEmpty()) {
            List<RankedDoc> reranked = rerank(query, ordered, topK);
            if (reranked != null) {
                finalDocs = reranked;
            }
        }
        return finalDocs.stream().limit(topK).map(RankedDoc::toDocument).toList();
    }

    /** 调试接口:返回每一阶段的中间结果(关键词分/向量分/融合分/重排分) */
    public List<Map<String, Object>> debug(String query, List<Long> courseIds, Long userId, KbConfig cfg) {
        return retrieveDebug(query, courseIds, userId, cfg);
    }

    private List<Map<String, Object>> retrieveDebug(String query, List<Long> courseIds, Long userId, KbConfig cfg) {
        if (courseIds == null || courseIds.isEmpty()) {
            return List.of();
        }
        double threshold = cfg.similarityThreshold() == null ? 0.5 : cfg.similarityThreshold();
        boolean rerank = Boolean.TRUE.equals(cfg.rerankEnabled())
                && props.getRerankApiKey() != null && !props.getRerankApiKey().isBlank();

        List<Document> vectorHits = List.of();
        try {
            String expr = "courseId in ['" + courseIds.stream()
                    .map(String::valueOf).collect(java.util.stream.Collectors.joining("', '")) + "']";
            vectorHits = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query).topK(LEG_LIMIT).similarityThreshold(threshold)
                    .filterExpression(expr).build());
        } catch (Exception e) {
            log.warn("调试:向量检索失败 {}", e.getMessage());
        }
        List<Map<String, Object>> keywordRows;
        try {
            keywordRows = chunkMapper.keywordSearch(query, courseIds, LEG_LIMIT);
        } catch (Exception e) {
            keywordRows = List.of();
        }

        List<Document> vectorOk = filterAllowed(vectorHits, userId);
        List<KeywordHit> keywordOk = new ArrayList<>();
        for (Map<String, Object> row : keywordRows) {
            Long docId = toLong(row.get("resourceId"));
            if (docId != null && courseAccessService.canReadResource(docId, userId)) {
                keywordOk.add(new KeywordHit(docId, toLong(row.get("chunkId")), str(row.get("docTitle")),
                        toInt(row.get("pageNum")), str(row.get("vectorId")),
                        str(row.get("content")), toDouble(row.get("kwScore"))));
            }
        }

        Map<String, RankedDoc> fused = new LinkedHashMap<>();
        rankInto(fused, vectorOk.stream().map(d -> (Rankable) VectorRank.of(d)).toList());
        rankInto(fused, keywordOk.stream().map(k -> (Rankable) k).toList());
        List<RankedDoc> ordered = new ArrayList<>(fused.values());
        ordered.sort((a, b) -> Double.compare(b.rrfScore(), a.rrfScore()));

        List<RankedDoc> finalDocs = ordered;
        if (rerank && !ordered.isEmpty()) {
            List<RankedDoc> reranked = rerank(query, ordered, ordered.size());
            if (reranked != null) {
                finalDocs = reranked;
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        int rank = 1;
        for (RankedDoc doc : finalDocs) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("chunkId", doc.chunkId());
            m.put("docId", doc.docId());
            m.put("docTitle", doc.docTitle());
            m.put("page", doc.page());
            m.put("snippet", abbreviate(doc.text(), 160));
            m.put("vectorScore", doc.vectorScore() == null ? null : round3(doc.vectorScore()));
            m.put("keywordScore", doc.keywordScore() == null ? null : round3(doc.keywordScore()));
            m.put("rrfScore", round3(doc.rrfScore()));
            m.put("rerankScore", doc.rerankScore() == null ? null : round3(doc.rerankScore()));
            out.add(m);
        }
        return out;
    }

    // ---------- 内部:融合/重排/包装 ----------

    private void rankInto(Map<String, RankedDoc> fused, List<Rankable> docs) {
        int rank = 1;
        for (Rankable d : docs) {
            RankedDoc current = fused.get(d.key());
            if (current == null) {
                current = RankedDoc.empty(d.docId(), d.docTitle(), d.page(),
                        d.chunkId(), d.vectorId(), d.text());
            }
            if (d instanceof VectorRank vr) {
                current = current.withVector(vr.score(), rank, vr.doc());
            } else if (d instanceof KeywordHit kh) {
                current = current.withKeyword(kh, rank);
            }
            fused.put(d.key(), current);
            rank++;
        }
    }

    /** BGE-Reranker 重排(兼容 SiliconFlow /v1/rerank)。失败返回 null 由调用方降级。 */
    private List<RankedDoc> rerank(String query, List<RankedDoc> docs, int topN) {
        try {
            List<String> texts = docs.stream().map(RankedDoc::text).toList();
            String payload = objectMapper.writeValueAsString(Map.of(
                    "model", props.getRerankModel(),
                    "query", query,
                    "documents", texts,
                    "top_n", Math.min(topN, docs.size())));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getRerankBaseUrl() + "/v1/rerank"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + props.getRerankApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("重排服务返回 {},降级为融合顺序", resp.statusCode());
                return null;
            }
            JsonNode results = objectMapper.readTree(resp.body()).path("results");
            List<RankedDoc> out = new ArrayList<>();
            for (JsonNode r : results) {
                int idx = r.path("index").asInt(-1);
                double score = r.path("relevance_score").asDouble(0);
                if (idx >= 0 && idx < docs.size()) {
                    out.add(docs.get(idx).withRerank(score));
                }
            }
            return out;
        } catch (Exception e) {
            log.warn("重排调用失败,降级为融合顺序: {}", e.getMessage());
            return null;
        }
    }

    private List<Document> filterAllowed(List<Document> hits, Long userId) {
        if (hits == null) {
            return List.of();
        }
        return hits.stream().filter(hit -> {
            Long docId = toLong(hit.getMetadata().get("docId"));
            return docId != null && courseAccessService.canReadResource(docId, userId);
        }).toList();
    }

    // ---------- 数据结构 ----------

    /** 两路召回统一可融合的抽象 */
    private interface Rankable {
        String key();

        Long docId();

        String docTitle();

        Integer page();

        Long chunkId();

        String vectorId();

        String text();
    }

    private record VectorRank(String key, Long docId, String docTitle, Integer page,
                              Long chunkId, String vectorId, String text, double score, Document doc)
            implements Rankable {
        static VectorRank of(Document d) {
            Map<String, Object> meta = d.getMetadata();
            Long docId = toLong(meta.get("docId"));
            Integer page = meta.get("page") == null || meta.get("page").toString().isBlank() ? null
                    : Integer.valueOf(meta.get("page").toString());
            String vectorId = meta.get("vectorId") == null ? null : meta.get("vectorId").toString();
            String title = meta.get("docTitle") == null ? "" : meta.get("docTitle").toString();
            return new VectorRank(keyOf(docId, vectorId, null), docId, title, page,
                    null, vectorId, d.getText() == null ? "" : d.getText(),
                    d.getScore() == null ? 0 : d.getScore(), d);
        }

        @Override
        public Long chunkId() {
            return chunkId;
        }
    }

    private record KeywordHit(String key, Long docId, String docTitle, Integer page,
                              Long chunkId, String vectorId, String text, double score)
            implements Rankable {
        KeywordHit(Long docId, Long chunkId, String docTitle, Integer page, String vectorId,
                   String text, double score) {
            this(keyOf(docId, vectorId, chunkId), docId, docTitle, page, chunkId, vectorId, text, score);
        }
    }

    /** 融合+重排后的检索结果(不可变累加) */
    private record RankedDoc(String key, Long docId, String docTitle, Integer page, Long chunkId,
                             String vectorId, String text, double rrfScore, Double vectorScore,
                             Double keywordScore, Double rerankScore, Document doc) {

        static RankedDoc empty(Long docId, String docTitle, Integer page, Long chunkId,
                               String vectorId, String text) {
            return new RankedDoc(keyOf(docId, vectorId, chunkId), docId, docTitle, page, chunkId,
                    vectorId, text == null ? "" : text, 0, null, null, null, null);
        }

        RankedDoc withVector(double score, int rank, Document d) {
            return new RankedDoc(key, docId, docTitle, page, chunkId, vectorId, text,
                    rrfScore + 1.0 / (RRF_K + rank), score, keywordScore, rerankScore, d);
        }

        RankedDoc withKeyword(KeywordHit kh, int rank) {
            // 关键词路携带 chunkId/page,若向量路先建条目则补齐
            return new RankedDoc(key, docId, docTitle, page != null ? page : kh.page(),
                    chunkId != null ? chunkId : kh.chunkId(), vectorId, text,
                    rrfScore + 1.0 / (RRF_K + rank), vectorScore, kh.score(), rerankScore, doc);
        }

        RankedDoc withRerank(double score) {
            return new RankedDoc(key, docId, docTitle, page, chunkId, vectorId, text,
                    rrfScore, vectorScore, keywordScore, score, doc);
        }

        Document toDocument() {
            Map<String, Object> meta = new LinkedHashMap<>();
            // Spring AI 不允许元数据为 null,故逐项判空后写入
            if (docId != null) {
                meta.put("docId", docId);
            }
            if (docTitle != null && !docTitle.isBlank()) {
                meta.put("docTitle", docTitle);
            }
            if (page != null) {
                meta.put("page", page);
            }
            if (vectorId != null) {
                meta.put("vectorId", vectorId);
            }
            if (chunkId != null) {
                meta.put("chunkId", chunkId);
            }
            meta.put("rrfScore", rrfScore);
            if (rerankScore != null) {
                meta.put("rerankScore", rerankScore);
            }
            double score = rerankScore != null ? rerankScore : rrfScore;
            return Document.builder().text(text).metadata(meta).score(score).build();
        }
    }

    // ---------- 工具 ----------

    private static String str(Object o) {
        return o == null ? "" : o.toString();
    }

    /** 融合唯一键:优先 vectorId,缺省用 chunkId 兜底,避免同文档未嵌入块互相覆盖 */
    private static String keyOf(Long docId, String vectorId, Long chunkId) {
        return docId + ":" + (vectorId != null ? vectorId : "chunk" + chunkId);
    }

    private static Long toLong(Object o) {
        try {
            return o == null ? null : Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer toInt(Object o) {
        try {
            return o == null ? null : Integer.valueOf(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double toDouble(Object o) {
        try {
            return o == null ? null : Double.parseDouble(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static double round3(double v) {
        return Math.round(v * 1000) / 1000.0;
    }

    private static String abbreviate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
