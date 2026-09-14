package com.rag.edu.service.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rag.edu.config.RagProperties;
import com.rag.edu.dto.KbDtos.KbConfig;
import com.rag.edu.mapper.DocChunkMapper;
import com.rag.edu.service.CourseAccessService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RetrievalServiceTest {

    private final VectorStore vectorStore = mock(VectorStore.class);
    private final DocChunkMapper chunkMapper = mock(DocChunkMapper.class);
    private final CourseAccessService access = mock(CourseAccessService.class);
    private final RagProperties props = new RagProperties();
    private final RetrievalService service =
            new RetrievalService(vectorStore, chunkMapper, access, props, new ObjectMapper());

    private static final KbConfig CFG = new KbConfig(5, 0.5, null, null, null, false);

    @Test
    void fusesVectorAndKeywordHitsAndKeepsChunkMetadata() {
        // 同一分块同时被两路召回,并有一条越权关键词命中被过滤
        Document vectorDoc = Document.builder()
                .text("向量命中的分块内容")
                .metadata(Map.of("docId", 1L, "docTitle", "教材A", "page", 3, "vectorId", "v1"))
                .score(0.9).build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(vectorDoc));
        when(chunkMapper.keywordSearch(anyString(), anyList(), anyInt())).thenReturn(List.of(
                keywordRow(1L, 2L, "教材A", 3, "v1", "关键词命中的分块内容", 5.0),
                keywordRow(2L, 9L, "他人资料", 1, "v9", "无权访问的内容", 8.0)));
        when(access.canReadResource(1L, 7L)).thenReturn(true);
        when(access.canReadResource(2L, 7L)).thenReturn(false);

        List<Document> hits = service.retrieve("什么是卷积", List.of(10L), 7L, CFG);

        assertEquals(1, hits.size());
        Document hit = hits.get(0);
        assertEquals(1L, hit.getMetadata().get("docId"));
        assertEquals("教材A", hit.getMetadata().get("docTitle"));
        // 关键词路带回了 chunkId,不应丢失
        assertEquals(2L, hit.getMetadata().get("chunkId"));
        assertEquals(3, hit.getMetadata().get("page"));
        assertEquals("v1", hit.getMetadata().get("vectorId"));
        // 融合分 = 两路各自 rank1 的 RRF 之和
        assertTrue(hit.getScore() > 0);
    }

    @Test
    void returnsEmptyWhenNoCourseScope() {
        assertTrue(service.retrieve("问题", null, 1L, CFG).isEmpty());
        assertTrue(service.retrieve("问题", List.of(), 1L, CFG).isEmpty());
    }

    @Test
    void rerankFailureDegradesToFusedOrderWithoutThrowing() {
        // 重排地址指向不可达端口,调用失败应降级为融合顺序而非抛异常
        props.setRerankBaseUrl("http://127.0.0.1:1");
        props.setRerankModel("BAAI/bge-reranker-v2-m3");
        props.setRerankApiKey("sk-fake");
        KbConfig withRerank = new KbConfig(5, 0.5, null, null, null, true);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                Document.builder().text("内容A")
                        .metadata(Map.of("docId", 1L, "docTitle", "教材A", "page", 1, "vectorId", "va"))
                        .score(0.8).build()));
        when(chunkMapper.keywordSearch(anyString(), anyList(), anyInt())).thenReturn(List.of(
                keywordRow(1L, 2L, "教材A", 1, "va", "内容A", 3.0)));
        when(access.canReadResource(1L, 7L)).thenReturn(true);

        List<Document> hits = service.retrieve("测试问题", List.of(10L), 7L, withRerank);

        assertEquals(1, hits.size());
        assertEquals("内容A", hits.get(0).getText());
    }

    @Test
    void keywordIndexMissingDegradesToVectorOnly() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                Document.builder().text("向量内容")
                        .metadata(Map.of("docId", 1L, "docTitle", "教材A", "page", 2, "vectorId", "vb"))
                        .score(0.7).build()));
        when(chunkMapper.keywordSearch(anyString(), anyList(), anyInt()))
                .thenThrow(new RuntimeException("MATCH 无全文索引"));
        when(access.canReadResource(1L, 7L)).thenReturn(true);

        List<Document> hits = service.retrieve("问题", List.of(10L), 7L, CFG);

        assertEquals(1, hits.size());
        assertEquals("向量内容", hits.get(0).getText());
    }

    @Test
    void vectorHitWithBlankPageMetadataDoesNotCrash() {
        // TXT 等无页码文档入库时 page 写入空串,检索侧应容忍为 null 而非抛 NumberFormatException
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                Document.builder().text("无页码分块内容")
                        .metadata(Map.of("docId", 1L, "docTitle", "教材B", "page", "", "vectorId", "vc"))
                        .score(0.7).build()));
        when(chunkMapper.keywordSearch(anyString(), anyList(), anyInt()))
                .thenThrow(new RuntimeException("MATCH 无全文索引"));
        when(access.canReadResource(1L, 7L)).thenReturn(true);

        List<Document> hits = service.retrieve("问题", List.of(10L), 7L, CFG);

        assertEquals(1, hits.size());
        assertEquals("无页码分块内容", hits.get(0).getText());
    }

    private static Map<String, Object> keywordRow(Long resourceId, Long chunkId, String title,
                                                  Integer page, String vectorId, String content, double kwScore) {
        return Map.of(
                "resourceId", resourceId, "chunkId", chunkId, "docTitle", title,
                "pageNum", page, "vectorId", vectorId, "content", content, "kwScore", kwScore);
    }
}
