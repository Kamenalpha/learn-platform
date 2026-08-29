package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.config.RagProperties;
import com.rag.edu.dto.KbDtos.KbConfig;
import com.rag.edu.entity.CourseDocument;
import com.rag.edu.mapper.CourseDocumentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.service.DocumentService;
import com.rag.edu.service.KbConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理(管理员):检索参数配置 / 删除重建 / 运行状态
 */
@RestController
@RequestMapping("/api/admin/kb")
@RequiredArgsConstructor
public class KbController {

    private final KbConfigService kbConfigService;
    private final DocumentService documentService;
    private final CourseDocumentMapper documentMapper;
    private final RagProperties props;

    @Value("${spring.ai.vectorstore.chroma.client.host:http://localhost}")
    private String chromaHost;

    @Value("${spring.ai.vectorstore.chroma.client.port:8000}")
    private int chromaPort;

    @Value("${spring.ai.vectorstore.chroma.collection-name:rag_edu_knowledge}")
    private String collectionName;

    @GetMapping("/config")
    public Result<KbConfig> config() {
        return Result.ok(kbConfigService.get());
    }

    @PutMapping("/config")
    public Result<KbConfig> updateConfig(@RequestBody KbConfig config) {
        kbConfigService.update(config);
        return Result.ok(kbConfigService.get());
    }

    /** 按课程重建向量库:对课程下全部文档重新解析入库 */
    @PostMapping("/rebuild/{courseId}")
    public Result<Map<String, Object>> rebuild(@PathVariable Long courseId) {
        List<CourseDocument> docs = documentMapper.selectList(
                new LambdaQueryWrapper<CourseDocument>().eq(CourseDocument::getCourseId, courseId));
        int ok = 0, fail = 0;
        for (CourseDocument doc : docs) {
            try {
                documentService.reparse(doc.getDocId());
                ok++;
            } catch (Exception e) {
                fail++;
            }
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", docs.size());
        data.put("success", ok);
        data.put("failed", fail);
        return Result.ok(data);
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("chromaUrl", chromaHost + ":" + chromaPort);
        data.put("collection", collectionName);
        data.put("embedBatchSize", props.getEmbedBatchSize());
        data.put("chunkSize", props.getChunkSize());
        data.put("chunkOverlap", props.getChunkOverlap());
        data.put("docTotal", documentMapper.selectCount(null));
        data.put("docParsed", documentMapper.selectCount(
                new LambdaQueryWrapper<CourseDocument>().eq(CourseDocument::getParseStatus, 1)));
        return Result.ok(data);
    }
}
