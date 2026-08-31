package com.rag.edu.service.rag;

import com.rag.edu.common.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档文本抽取:PDF(PDFBox)/ Word(POI)/ PPT(POI)/ TXT。
 * 输出按“页”组织的段落列表,便于后续分块时保留页码,实现引用溯源。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextExtractor {

    private final OcrService ocrService;

    /**
     * 一个解析段落:page 为页码(PDF页/幻灯片页),无页概念的文档为 null
     */
    public record Segment(Integer page, String text) {
    }

    public List<Segment> extract(File file, String type) {
        try {
            return switch (type) {
                case "pdf" -> extractPdf(file);
                case "word" -> extractWord(file);
                case "ppt" -> extractPpt(file);
                case "txt" -> extractTxt(file);
                default -> throw new BizException("不支持的文件类型: " + type);
            };
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败: {}", file.getName(), e);
            throw new BizException("文档解析失败: " + e.getMessage());
        }
    }

    private List<Segment> extractPdf(File file) throws IOException {
        List<Segment> segments = new ArrayList<>();
        try (PDDocument doc = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                stripper.setStartPage(i);
                stripper.setEndPage(i);
                String text = stripper.getText(doc);
                text = text == null ? "" : text.replace('\u0000', ' ').trim();
                if (!text.isEmpty()) {
                    segments.add(new Segment(i, text));
                }
            }
        }
        // 无文本层 = 扫描件;配置了 OCR 服务时交由 OCR 识别
        if (segments.isEmpty()) {
            return ocrService.recognizePdf(file);
        }
        return segments;
    }

    private List<Segment> extractWord(File file) throws IOException {
        // Word 无固定分页概念,整篇作为一个段落
        String name = file.getName().toLowerCase();
        String text;
        if (name.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(file.toPath()));
                 XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                text = extractor.getText();
            }
        } else {
            try (org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(
                    Files.newInputStream(file.toPath()));
                 org.apache.poi.hwpf.extractor.WordExtractor extractor =
                         new org.apache.poi.hwpf.extractor.WordExtractor(doc)) {
                text = extractor.getText();
            }
        }
        return text == null || text.isBlank() ? List.of() : List.of(new Segment(null, text.trim()));
    }

    private List<Segment> extractPpt(File file) throws IOException {
        List<Segment> segments = new ArrayList<>();
        String name = file.getName().toLowerCase();
        if (name.endsWith(".pptx")) {
            try (XMLSlideShow ppt = new XMLSlideShow(Files.newInputStream(file.toPath()))) {
                List<XSLFSlide> slides = ppt.getSlides();
                for (int i = 0; i < slides.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    for (XSLFShape shape : slides.get(i).getShapes()) {
                        if (shape instanceof XSLFTextShape textShape) {
                            String t = textShape.getText();
                            if (t != null && !t.isBlank()) {
                                sb.append(t.trim()).append('\n');
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        segments.add(new Segment(i + 1, sb.toString().trim()));
                    }
                }
            }
        } else {
            try (HSLFSlideShow ppt = new HSLFSlideShow(Files.newInputStream(file.toPath()))) {
                List<HSLFSlide> slides = ppt.getSlides();
                for (int i = 0; i < slides.size(); i++) {
                    StringBuilder sb = new StringBuilder();
                    String title = slides.get(i).getTitle();
                    if (title != null && !title.isBlank()) {
                        sb.append(title.trim()).append('\n');
                    }
                    slides.get(i).getTextParagraphs().forEach(paras ->
                            sb.append(HSLFTextParagraph.getRawText(paras)).append('\n'));
                    if (sb.length() > 0) {
                        segments.add(new Segment(i + 1, sb.toString().trim()));
                    }
                }
            }
        }
        return segments;
    }

    private List<Segment> extractTxt(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        return List.of(new Segment(null, decodeText(bytes).trim()));
    }

    /** 优先 UTF-8,失败时回退 GBK(常见中文课件编码) */
    private String decodeText(byte[] bytes) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT);
            return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, java.nio.charset.Charset.forName("GBK"));
        }
    }
}
