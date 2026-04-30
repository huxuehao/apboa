package com.hxh.apboa.core.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块器，支持固定大小分块与段落感知分块
 *
 * @author huxuehao
 */
@Component
public class TextChunker {

    /**
     * 固定大小分块（带重叠）
     *
     * @param text     原始文本
     * @param chunkSize  分块大小（字符数）
     * @param overlap  重叠大小（字符数）
     * @return 分块列表
     */
    public List<ChunkResult> fixedSizeChunk(String text, int chunkSize, int overlap) {
        List<ChunkResult> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        int index = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            String chunkText = text.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                chunks.add(new ChunkResult(index++, chunkText, start, end));
            }

            start += chunkSize - overlap;
            if (start < 0) {
                start = 0;
            }
            if (start >= text.length()) {
                break;
            }
            if (start <= end - chunkSize + overlap && chunks.size() > 1) {
                break;
            }
        }

        return chunks;
    }

    /**
     * 段落感知分块：按段落分割后合并小段落
     *
     * @param text      原始文本
     * @param chunkSize 目标分块大小
     * @param overlap   重叠段落数
     * @return 分块列表
     */
    public List<ChunkResult> paragraphChunk(String text, int chunkSize, int overlap) {
        List<ChunkResult> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        String[] paragraphs = text.split("\n\n+");
        List<String> mergedParagraphs = mergeSmallParagraphs(paragraphs, chunkSize);

        int index = 0;
        int offset = 0;
        for (int i = 0; i < mergedParagraphs.size(); i++) {
            String chunkText = mergedParagraphs.get(i).trim();
            if (!chunkText.isEmpty()) {
                int end = offset + chunkText.length();
                chunks.add(new ChunkResult(index++, chunkText, offset, end));
            }
            offset += chunkText.length() + 2;
        }

        return applyOverlap(chunks, overlap);
    }

    /**
     * 合并小段落，使每个分块接近目标大小
     */
    private List<String> mergeSmallParagraphs(String[] paragraphs, int chunkSize) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (current.length() + trimmed.length() + 2 > chunkSize && current.length() > 0) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }

            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(trimmed);
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    /**
     * 为分块列表应用重叠策略
     */
    private List<ChunkResult> applyOverlap(List<ChunkResult> chunks, int overlapParagraphs) {
        if (overlapParagraphs <= 0 || chunks.size() <= 1) {
            return chunks;
        }

        List<ChunkResult> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            StringBuilder sb = new StringBuilder();

            for (int j = Math.max(0, i - overlapParagraphs); j < i; j++) {
                sb.append(chunks.get(j).content()).append("\n\n");
            }

            sb.append(chunks.get(i).content());
            result.add(new ChunkResult(
                    chunks.get(i).index(),
                    sb.toString().trim(),
                    chunks.get(i).startOffset(),
                    chunks.get(i).endOffset()
            ));
        }

        return result;
    }

    /**
     * 分块结果记录
     */
    public record ChunkResult(int index, String content, int startOffset, int endOffset) {
    }
}
