package com.rag.edu.service.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 递归字符分割法(Recursive Character Splitting):
 * 依次尝试用 ["空行","换行","。","!","?",";",",","空格"] 等分隔符递归拆分,
 * 使每个分块尽量接近目标长度且不切断语义;相邻块保留 overlap 重叠字符。
 */
@Component
public class TextChunker {

    /** 依次尝试的分隔符:全角/半角标点 + 空白符,最后兜底硬切 */
    private static final String[] SEPARATORS = {
            "\n\n", "\n", "。", "!", "\uFF01", "?", "\uFF1F", ";", "\uFF1B",
            ",", "\uFF0C", " ", ""
    };

    /**
     * 分块结果:page 继承自所属段落,用于引用溯源
     */
    public record Chunk(String text, Integer page) {
    }

    public List<Chunk> chunk(List<TextExtractor.Segment> segments, int size, int overlap) {
        List<Chunk> result = new ArrayList<>();
        for (TextExtractor.Segment seg : segments) {
            List<String> pieces = new ArrayList<>();
            splitRecursive(seg.text(), 0, size, overlap, pieces);
            for (String piece : merge(pieces, size, overlap)) {
                String text = piece.replaceAll("\n{3,}", "\n\n").trim();
                if (!text.isEmpty()) {
                    result.add(new Chunk(text, seg.page()));
                }
            }
        }
        return result;
    }

    private void splitRecursive(String text, int sepIdx, int size, int overlap, List<String> out) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (text.length() <= size) {
            out.add(text);
            return;
        }
        // 分隔符用尽时按固定窗口硬切
        if (sepIdx >= SEPARATORS.length - 1) {
            int step = Math.max(size - overlap, 1);
            for (int i = 0; i < text.length(); i += step) {
                out.add(text.substring(i, Math.min(text.length(), i + size)));
                if (i + size >= text.length()) {
                    break;
                }
            }
            return;
        }
        String sep = SEPARATORS[sepIdx];
        int start = 0;
        while (start < text.length()) {
            int idx = text.indexOf(sep, start);
            String part = idx == -1 ? text.substring(start) : text.substring(start, idx + sep.length());
            splitRecursive(part, sepIdx + 1, size, overlap, out);
            if (idx == -1) {
                break;
            }
            start = idx + sep.length();
        }
    }

    private List<String> merge(List<String> pieces, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String p : pieces) {
            if (cur.length() > 0 && cur.length() + p.length() > size) {
                chunks.add(cur.toString());
                // 取尾部 overlap 个字符作为下一块的开头,保证上下文连续
                String tail = cur.substring(Math.max(0, cur.length() - overlap));
                cur = new StringBuilder(tail);
            }
            cur.append(p);
        }
        if (cur.length() > 0) {
            chunks.add(cur.toString());
        }
        return chunks;
    }
}
