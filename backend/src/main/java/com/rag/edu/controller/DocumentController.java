package com.rag.edu.controller;

import com.rag.edu.common.Result;
import com.rag.edu.common.UserContext;
import com.rag.edu.entity.DocResource;
import com.rag.edu.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 文档管理接口:上传/列表/详情/预览开放;删除与重新解析仅管理员
 */
@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /** 上传课件并自动解析入库 */
    @PostMapping("/upload")
    public Result<DocResource> upload(@RequestParam("file") MultipartFile file,
                                      @RequestParam("courseId") Long courseId) {
        return Result.ok(documentService.upload(file, courseId, UserContext.userId()));
    }

    @GetMapping
    public Result<List<DocResource>> list(@RequestParam(required = false) Long courseId) {
        return Result.ok(documentService.list(courseId));
    }

    /** 文档详情 + 分块列表(文档预览/分块检查) */
    @GetMapping("/{docId}")
    public Result<Map<String, Object>> detail(@PathVariable Long docId) {
        return Result.ok(documentService.detail(docId));
    }

    /** 源文件预览(PDF可在浏览器内直接打开) */
    @GetMapping("/{docId}/file")
    public ResponseEntity<Resource> file(@PathVariable Long docId) {
        Map<String, Object> loaded = documentService.loadFile(docId);
        String filename = URLEncoder.encode((String) loaded.get("filename"), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType((String) loaded.get("contentType")))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + filename)
                .body((Resource) loaded.get("resource"));
    }

    @PostMapping("/{resourceId}/reparse")
    public Result<Integer> reparse(@PathVariable Long resourceId) {
        return Result.ok(documentService.reparse(resourceId, UserContext.userId()));
    }

    /** 设置资料可见性:0私有 1公开(需审核) 2分享 */
    @PutMapping("/{resourceId}/visibility")
    public Result<Void> setVisibility(@PathVariable Long resourceId, @RequestParam Integer visibility) {
        documentService.setVisibility(resourceId, visibility, UserContext.userId());
        return Result.ok();
    }

    @DeleteMapping("/{resourceId}")
    public Result<Void> delete(@PathVariable Long resourceId) {
        documentService.delete(resourceId, UserContext.userId());
        return Result.ok();
    }
}
