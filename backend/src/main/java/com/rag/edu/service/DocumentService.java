package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.config.RagProperties;
import com.rag.edu.entity.CourseDocument;
import com.rag.edu.entity.DocChunk;
import com.rag.edu.mapper.CourseDocumentMapper;
import com.rag.edu.mapper.DocChunkMapper;
import com.rag.edu.service.rag.IngestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 文档管理:上传 / 列表 / 详情 / 删除 / 重新解析 / 文件预览
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "ppt", "pptx", "txt");

    private final CourseDocumentMapper documentMapper;
    private final DocChunkMapper chunkMapper;
    private final IngestService ingestService;
    private final RagProperties props;

    /** 上传文档并触发解析入库,返回文档信息 */
    public CourseDocument upload(MultipartFile file, Long courseId, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String original = file.getOriginalFilename() == null ? "未命名" : file.getOriginalFilename();
        String ext = extOf(original);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BizException("仅支持 pdf / word(doc,docx) / ppt(ppt,pptx) / txt 格式");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = Path.of(props.getUploadDir());
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(storedName).toAbsolutePath());
        } catch (IOException e) {
            throw new BizException("文件保存失败: " + e.getMessage());
        }

        CourseDocument doc = new CourseDocument();
        doc.setCourseId(courseId);
        doc.setDocTitle(stripExt(original));
        doc.setFileType(fileTypeOf(ext));
        doc.setFileUrl(storedName);
        doc.setFileSize(file.getSize());
        doc.setParseStatus(0);
        doc.setUploaderId(uploaderId);
        documentMapper.insert(doc);

        // 同步解析入库(课件一般几十页,耗时数秒;失败不影响文档记录,可重新解析)
        try {
            ingestService.ingest(doc.getDocId());
        } catch (Exception e) {
            log.warn("文档[{}]自动解析失败: {}", original, e.getMessage());
        }
        return documentMapper.selectById(doc.getDocId());
    }

    public List<CourseDocument> list(Long courseId) {
        LambdaQueryWrapper<CourseDocument> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) {
            wrapper.eq(CourseDocument::getCourseId, courseId);
        }
        wrapper.orderByDesc(CourseDocument::getCreateTime);
        return documentMapper.selectList(wrapper);
    }

    public Map<String, Object> detail(Long docId) {
        CourseDocument doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new BizException("文档不存在");
        }
        List<DocChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<DocChunk>()
                .eq(DocChunk::getDocId, docId).orderByAsc(DocChunk::getChunkIndex));
        return Map.of("doc", doc, "chunks", chunks);
    }

    /** 删除文档:清理向量 + 分块 + 文件 + 记录 */
    public void delete(Long docId) {
        CourseDocument doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new BizException("文档不存在");
        }
        ingestService.removeVectors(docId);
        chunkMapper.delete(new LambdaQueryWrapper<DocChunk>().eq(DocChunk::getDocId, docId));
        try {
            Files.deleteIfExists(Path.of(props.getUploadDir(), doc.getFileUrl()));
        } catch (IOException e) {
            log.warn("删除源文件失败: {}", doc.getFileUrl());
        }
        documentMapper.deleteById(docId);
    }

    /** 重新解析(调整分块参数后可重建向量) */
    public int reparse(Long docId) {
        return ingestService.ingest(docId);
    }

    /** 文件预览/下载 */
    public Map<String, Object> loadFile(Long docId) {
        CourseDocument doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new BizException("文档不存在");
        }
        Path path = Path.of(props.getUploadDir(), doc.getFileUrl());
        if (!Files.exists(path)) {
            throw new BizException("源文件已丢失");
        }
        String contentType = switch (doc.getFileType()) {
            case "pdf" -> MediaType.APPLICATION_PDF_VALUE;
            case "word" -> doc.getFileUrl().endsWith(".docx")
                    ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    : "application/msword";
            case "ppt" -> doc.getFileUrl().endsWith(".pptx")
                    ? "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    : "application/vnd.ms-powerpoint";
            default -> MediaType.TEXT_PLAIN_VALUE;
        };
        return Map.of("resource", new FileSystemResource(path),
                "contentType", contentType,
                "filename", doc.getDocTitle() + "." + extOf(doc.getFileUrl()));
    }

    private static String extOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase();
    }

    private static String fileTypeOf(String ext) {
        return switch (ext) {
            case "pdf" -> "pdf";
            case "doc", "docx" -> "word";
            case "ppt", "pptx" -> "ppt";
            default -> "txt";
        };
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }
}
