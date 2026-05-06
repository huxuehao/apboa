package com.hxh.apboa.core.rag.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文本分块器，支持固定大小分块、段落感知分块、自定义标识分块
 *
 * @author huxuehao
 */
@Component
public class TextChunker {

    /**
     * 固定大小分块（带重叠）
     *
     * @param text      原始文本
     * @param chunkSize 分块大小（字符数）
     * @param overlap   重叠大小（字符数）
     * @return 分块列表
     */
    public List<ChunkResult> fixedSizeChunk(String text, int chunkSize, int overlap) {
        List<ChunkResult> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        int index = 0;
        while (true) {
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
     * 自定义标识分块：按指定分隔符拆分文本，超长段落按行分割
     *
     * @param text       原始文本
     * @param chunkSize  最大分块大小（字符数），超过此大小的段落会按行分割
     * @param overlap    重叠大小（字符数）
     * @param delimiters 分隔符列表，按优先级依次尝试拆分
     * @return 分块列表
     */
    public List<ChunkResult> delimiterChunk(String text, int chunkSize, int overlap, List<String> delimiters) {
        List<ChunkResult> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }
        if (delimiters == null || delimiters.isEmpty()) {
            return fixedSizeChunk(text, chunkSize, overlap);
        }

        String regex = buildDelimiterRegex(delimiters);
        String[] segments = text.split(regex);

        List<String> processedSegments = new ArrayList<>();
        for (String segment : segments) {
            String trimmed = segment.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.length() > chunkSize) {
                processedSegments.addAll(splitByLine(trimmed, chunkSize));
            } else {
                processedSegments.add(trimmed);
            }
        }

        int index = 0;
        int offset = 0;
        for (String chunkText : processedSegments) {
            String trimmed = chunkText.trim();
            if (!trimmed.isEmpty()) {
                int end = offset + trimmed.length();
                chunks.add(new ChunkResult(index++, trimmed, offset, end));
            }
            offset += chunkText.length() + 1;
        }

        return applyOverlap(chunks, overlap);
    }

    /**
     * 构建分隔符的正则表达式，将多个分隔符合并为一个正则
     */
    private String buildDelimiterRegex(List<String> delimiters) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < delimiters.size(); i++) {
            if (i > 0) {
                sb.append("|");
            }
            sb.append(java.util.regex.Pattern.quote(delimiters.get(i)));
        }
        return sb.toString();
    }

    /**
     * 按行分割超长段落，尽量在行边界处切分
     */
    private List<String> splitByLine(String text, int chunkSize) {
        List<String> result = new ArrayList<>();
        String[] lines = text.split("\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (line.trim().isEmpty() && current.isEmpty()) {
                continue;
            }

            if (current.length() + line.length() + 1 > chunkSize && !current.isEmpty()) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }

            if (!current.isEmpty()) {
                current.append("\n");
            }
            current.append(line);
        }

        if (!current.isEmpty()) {
            String trimmed = current.toString().trim();
            if (!trimmed.isEmpty()) {
                if (trimmed.length() > chunkSize) {
                    result.addAll(splitFixedSize(trimmed, chunkSize));
                } else {
                    result.add(trimmed);
                }
            }
        }

        return result;
    }

    /**
     * 对无法按行分割的文本进行固定大小切分
     */
    private List<String> splitFixedSize(String text, int chunkSize) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                result.add(chunk);
            }
            start = end;
        }
        return result;
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

            if (current.length() + trimmed.length() + 2 > chunkSize && !current.isEmpty()) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }

            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(trimmed);
        }

        if (!current.isEmpty()) {
            result.add(current.toString().trim());
        }

        return result;
    }

    /**
     * 合并小片段，使每个分块接近目标大小
     */
    private List<String> mergeSmallSegments(List<String> segments, int chunkSize) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String segment : segments) {
            if (current.length() + segment.length() + 1 > chunkSize && !current.isEmpty()) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }

            if (!current.isEmpty()) {
                current.append("\n");
            }
            current.append(segment);
        }

        if (!current.isEmpty()) {
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
