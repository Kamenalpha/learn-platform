package com.rag.edu.service.rag;

import com.rag.edu.config.RagProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OCR 服务(扫描件教材识别)。默认对接一个 REST OCR 服务(PaddleOCR 起服务后按需适配)。
 * 约定:POST <ocrBaseUrl>,请求体为 PNG 图片字节,响应 JSON: {"text":"识别文本"}。
 * 未配置 ocr-base-url 时,本服务不启用,返回空(扫描件将解析为空文本,可在后台调整).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OcrService {

    private final RagProperties props;
    private final ObjectMapper objectMapper;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    /** 对扫描版 PDF 逐页 OCR,返回按页组织的段落。OCR 未配置时返回空列表 */
    public List<TextExtractor.Segment> recognizePdf(File file) {
        if (props.getOcrBaseUrl() == null || props.getOcrBaseUrl().isBlank()) {
            return List.of();
        }
        List<TextExtractor.Segment> segments = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(file)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pages = Math.min(doc.getNumberOfPages(), props.getOcrMaxPages());
            for (int i = 0; i < pages; i++) {
                try {
                    BufferedImage img = renderer.renderImageWithDPI(i, 200);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(img, "png", baos);
                    String text = recognize(baos.toByteArray());
                    if (text != null && !text.isBlank()) {
                        segments.add(new TextExtractor.Segment(i + 1, text.trim()));
                    }
                } catch (Exception e) {
                    log.warn("OCR 第{}页失败: {}", i + 1, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("OCR 打开PDF失败: {}", file.getName(), e);
        }
        return segments;
    }

    private String recognize(byte[] png) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(props.getOcrBaseUrl()))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "image/png")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(png))
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.warn("OCR 服务返回 {}", resp.statusCode());
                return "";
            }
            JsonNode node = objectMapper.readTree(resp.body());
            JsonNode text = node.get("text");
            return text == null ? "" : text.asText();
        } catch (Exception e) {
            log.warn("OCR 调用失败: {}", e.getMessage());
            return "";
        }
    }
}
