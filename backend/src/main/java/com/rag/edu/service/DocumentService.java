package com.rag.edu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rag.edu.common.BizException;
import com.rag.edu.common.LoginUser;
import com.rag.edu.common.UserContext;
import com.rag.edu.config.RagProperties;
import com.rag.edu.entity.DocChunk;
import com.rag.edu.entity.DocResource;
import com.rag.edu.mapper.DocChunkMapper;
import com.rag.edu.mapper.DocResourceMapper;
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
 * 资料管理:上传 / 列表 / 详情 / 删除 / 重新解析 / 文件预览。
 * 可见性:私有(0)。上传人即资料归属者。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final Set<String> ALLOWED_EXT = Set.of("pdf", "doc", "docx", "ppt", "pptx", "txt");

    private final DocResourceMapper resourceMapper;
    private final DocChunkMapper chunkMapper;
    private final IngestService ingestService;
    private final RagProperties props;

    /** 上传资料并自动触发解析入库,返回资料信息 */
    public DocResource upload(MultipartFile file, Long courseId, Long uploaderId) {
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

        DocResource res = new DocResource();
        res.setUserId(uploaderId);
        res.setCourseId(courseId);
        res.setTitle(stripExt(original));
        res.setFileType(fileTypeOf(ext));
        res.setFileUrl(storedName);
        res.setFileSize(file.getSize());
        res.setParseStatus(0);
        res.setVisibility(0);
        res.setAuditStatus(0);
        resourceMapper.insert(res);

        // 同步解析入库(课件一般几十页,耗时数秒;失败不影响记录,可重新解析)
        try {
            ingestService.ingest(res.getResourceId());
        } catch (Exception e) {
            log.warn("资料[{}]自动解析失败: {}", original, e.getMessage());
        }
        return resourceMapper.selectById(res.getResourceId());
    }

    public List<DocResource> list(Long courseId) {
        LambdaQueryWrapper<DocResource> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) {
            wrapper.eq(DocResource::getCourseId, courseId);
        }
        // 默认仅看自己上传 + 公开审核通过
        Long uid = UserContext.userId();
        wrapper.and(w -> w.eq(DocResource::getUserId, uid)
                .or().eq(DocResource::getVisibility, 1)
                .eq(DocResource::getAuditStatus, 1));
        wrapper.orderByDesc(DocResource::getCreateTime);
        return resourceMapper.selectList(wrapper);
    }

    public Map<String, Object> detail(Long resourceId) {
        DocResource res = resourceMapper.selectById(resourceId);
        if (res == null) {
            throw new BizException("资料不存在");
        }
        List<DocChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<DocChunk>()
                .eq(DocChunk::getResourceId, resourceId).orderByAsc(DocChunk::getChunkIndex));
        return Map.of("resource", res, "chunks", chunks);
    }

    /** 删除资料:清理向量 + 分块 + 文件 + 记录(归属者或管理员) */
    public void delete(Long resourceId, Long userId) {
        DocResource res = resourceMapper.selectById(resourceId);
        if (res == null) {
            throw new BizException("资料不存在");
        }
        checkOwnerOrAdmin(res, userId);
        ingestService.removeVectors(resourceId);
        chunkMapper.delete(new LambdaQueryWrapper<DocChunk>().eq(DocChunk::getResourceId, resourceId));
        try {
            Files.deleteIfExists(Path.of(props.getUploadDir(), res.getFileUrl()));
        } catch (IOException e) {
            log.warn("删除源文件失败: {}", res.getFileUrl());
        }
        resourceMapper.deleteById(resourceId);
    }

    /** 重新解析(调整分块参数后可重建向量;归属者或管理员) */
    public int reparse(Long resourceId, Long userId) {
        DocResource res = resourceMapper.selectById(resourceId);
        if (res == null) {
            throw new BizException("资料不存在");
        }
        checkOwnerOrAdmin(res, userId);
        return ingestService.ingest(resourceId);
    }

    /** 设置资料可见性(归属者或管理员):公开(1)会(重新)进入审核,通过后游客可见 */
    public void setVisibility(Long resourceId, Integer visibility, Long userId) {
        DocResource res = resourceMapper.selectById(resourceId);
        if (res == null) {
            throw new BizException("资料不存在");
        }
        checkOwnerOrAdmin(res, userId);
        if (visibility == null || visibility < 0 || visibility > 2) {
            throw new BizException("非法的可见性取值");
        }
        res.setVisibility(visibility);
        // 审核状态由服务端裁定:可见性变更后回到待审核(0)
        res.setAuditStatus(0);
        resourceMapper.updateById(res);
    }

    /** 文件预览/下载 */
    public Map<String, Object> loadFile(Long resourceId) {
        DocResource res = resourceMapper.selectById(resourceId);
        if (res == null) {
            throw new BizException("资料不存在");
        }
        Path path = Path.of(props.getUploadDir(), res.getFileUrl());
        if (!Files.exists(path)) {
            throw new BizException("源文件已丢失");
        }
        String contentType = switch (res.getFileType()) {
            case "pdf" -> MediaType.APPLICATION_PDF_VALUE;
            case "word" -> res.getFileUrl().endsWith(".docx")
                    ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    : "application/msword";
            case "ppt" -> res.getFileUrl().endsWith(".pptx")
                    ? "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                    : "application/vnd.ms-powerpoint";
            default -> MediaType.TEXT_PLAIN_VALUE;
        };
        return Map.of("resource", new FileSystemResource(path),
                "contentType", contentType,
                "filename", res.getTitle() + "." + extOf(res.getFileUrl()));
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

    /** 归属者或管理员可执行删除/重新解析 */
    private void checkOwnerOrAdmin(DocResource res, Long userId) {
        LoginUser u = UserContext.get();
        boolean admin = u != null && u.getRole() != null && u.getRole() == 1;
        if (!admin && (res.getUserId() == null || !res.getUserId().equals(userId))) {
            throw new BizException(403, "无权操作该资料");
        }
    }
}
