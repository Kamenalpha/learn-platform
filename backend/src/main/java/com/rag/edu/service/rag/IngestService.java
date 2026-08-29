package com.rag.edu.service.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.config.RagProperties;
import com.rag.edu.dto.KbDtos.KbConfig;
import com.rag.edu.entity.Course;
import com.rag.edu.entity.CourseDocument;
import com.rag.edu.entity.DocChunk;
import com.rag.edu.mapper.CourseDocumentMapper;
import com.rag.edu.mapper.CourseMapper;
import com.rag.edu.mapper.DocChunkMapper;
import com.rag.edu.service.KbConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 摄入链路:解析 -> 分块 -> 向量嵌入 -> 入库(Chroma)+ 落库(doc_chunk)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestService {

    private final CourseDocumentMapper documentMapper;
    private final CourseMapper courseMapper;
    private final DocChunkMapper chunkMapper;
    private final TextExtractor extractor;
    private final TextChunker chunker;
    private final VectorStore vectorStore;
    private final KbConfigService kbConfigService;
    private final RagProperties props;

    /**
     * 解析指定文档并写入向量库,返回分块数量。失败时置 parse_status=2 并抛出异常。
     */
    public int ingest(Long docId) {
        CourseDocument doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new BizException("文档不存在");
        }
        Course course = courseMapper.selectById(doc.getCourseId());
        if (course == null) {
            throw new BizException("所属课程不存在");
        }
        KbConfig cfg = kbConfigService.get();
        Path path = Path.of(props.getUploadDir(), doc.getFileUrl());
        try {
            List<TextExtractor.Segment> segments = extractor.extract(path.toFile(), doc.getFileType());
            List<TextChunker.Chunk> chunks = chunker.chunk(segments, cfg.chunkSize(), cfg.chunkOverlap());
            if (chunks.isEmpty()) {
                throw new BizException("未解析到文本内容");
            }
            // 重新解析场景:先清理旧向量与旧分块
            removeVectors(docId);
            chunkMapper.delete(new LambdaQueryWrapper<DocChunk>().eq(DocChunk::getDocId, docId));

            // 构造 Spring AI Document(id 固定为 docId-chunkIndex,便于删除重建)
            List<Document> aiDocs = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i++) {
                TextChunker.Chunk chunk = chunks.get(i);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("docId", String.valueOf(docId));
                metadata.put("docTitle", doc.getDocTitle());
                metadata.put("courseId", String.valueOf(doc.getCourseId()));
                metadata.put("courseName", course.getCourseName());
                metadata.put("page", chunk.page() == null ? "" : String.valueOf(chunk.page()));
                metadata.put("vectorId", vectorId(docId, i));
                aiDocs.add(new Document(vectorId(docId, i), chunk.text(), metadata));
            }
            // 分批嵌入并写入向量库
            int batch = Math.max(props.getEmbedBatchSize(), 1);
            for (int from = 0; from < aiDocs.size(); from += batch) {
                vectorStore.add(aiDocs.subList(from, Math.min(from + batch, aiDocs.size())));
            }
            // 原文分块落库(用于溯源、分块调整与删除重建)
            for (int i = 0; i < chunks.size(); i++) {
                DocChunk row = new DocChunk();
                row.setDocId(docId);
                row.setChunkIndex(i);
                row.setContent(chunks.get(i).text());
                row.setPageNum(chunks.get(i).page());
                row.setVectorId(vectorId(docId, i));
                chunkMapper.insert(row);
            }

            doc.setChunkCount(chunks.size());
            doc.setParseStatus(1);
            doc.setFailReason(null);
            documentMapper.updateById(doc);
            log.info("文档[{}]入库完成,共 {} 个分块", doc.getDocTitle(), chunks.size());
            return chunks.size();
        } catch (BizException e) {
            markFailed(doc, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("文档入库失败: {}", doc.getDocTitle(), e);
            markFailed(doc, e.getMessage());
            throw new BizException("文档入库失败: " + e.getMessage());
        }
    }

    /** 删除某文档在向量库中的全部向量 */
    public void removeVectors(Long docId) {
        List<DocChunk> chunks = chunkMapper.selectList(
                new LambdaQueryWrapper<DocChunk>().eq(DocChunk::getDocId, docId));
        if (chunks.isEmpty()) {
            return;
        }
        try {
            vectorStore.delete(chunks.stream().map(DocChunk::getVectorId).toList());
        } catch (Exception e) {
            // 向量删除失败不阻断主流程(重建时会被覆盖),记录日志便于排查
            log.warn("删除向量失败 docId={}: {}", docId, e.getMessage());
        }
    }

    private void markFailed(CourseDocument doc, String reason) {
        doc.setParseStatus(2);
        doc.setFailReason(reason == null ? null
                : reason.substring(0, Math.min(reason.length(), 490)));
        documentMapper.updateById(doc);
    }

    private String vectorId(Long docId, int index) {
        return docId + "-" + index;
    }
}
