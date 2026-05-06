package com.hxh.apboa.core.rag.parser.impl;

import com.hxh.apboa.core.rag.parser.IParser;
import org.apache.poi.xslf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.*;

/**
 * 描述：PPT文档解析器
 *
 * @author huxuehao
 **/
public class PptParser implements IParser {
    private static final Logger log = LoggerFactory.getLogger(PptParser.class);

    @Override
    public String parse(InputStream inputStream, String fileName, ParseFallback fallback) {
        return parsePpt(inputStream, fileName, fallback);
    }

    /**
     * PPT/PPTX 结构化解析：支持表格、占位符、文本框提取
     *
     * @param inputStream   文档输入流
     * @param fileName      文件名
     * @param fallback      降级解析方案，当POI解析失败时调用
     * @return              解析后的文本内容
     */
    private String parsePpt(InputStream inputStream, String fileName, ParseFallback fallback) {
        try {
            XMLSlideShow slideShow = new XMLSlideShow(inputStream);
            StringBuilder sb = new StringBuilder();

            List<XSLFSlide> slides = slideShow.getSlides();
            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);

                // 添加页码标题
                sb.append("## 第").append(i + 1).append("页\n\n");

                // 提取演讲者备注（如果有）
                XSLFNotes notes = slide.getNotes();
                if (notes != null) {
                    try {
                        String notesText = extractNotesText(notes);
                        if (notesText != null && !notesText.isBlank()) {
                            sb.append("> 备注：").append(notesText.trim()).append("\n\n");
                        }
                    } catch (Exception ex) {
                        log.debug("备注提取失败: {}", ex.getMessage());
                    }
                }

                // 收集当前页所有形状并按Y坐标排序（从上到下）
                List<XSLFShape> sortedShapes = new ArrayList<>(slide.getShapes());
                sortedShapes.sort(Comparator.comparingDouble(s -> {
                    try {
                        return s.getAnchor().getY();
                    } catch (Exception e) {
                        return Double.MAX_VALUE;
                    }
                }));

                // 记录已处理的占位符，避免重复
                HashSet<XSLFTextShape> processedShapes = new HashSet<>();

                // 第一遍：提取所有内容（占位符、文本框、表格等）
                for (XSLFShape shape : sortedShapes) {
                    // 处理占位符和普通文本框
                    if (shape instanceof XSLFTextShape textShape) {
                        // 跳过已处理的占位符（这里只是标记，实际提取在下面统一处理）
                        processedShapes.add(textShape);
                    }

                    // 提取文本内容
                    String extractedText = extractShapeText(shape, processedShapes);
                    if (extractedText != null && !extractedText.isBlank()) {
                        sb.append(extractedText).append("\n\n");
                    }
                }

                // 添加幻灯片分隔符
                sb.append("\n---\n\n");
            }

            slideShow.close();
            String content = sb.toString();
            return content.isBlank() ? "" : cleanContent(content);

        } catch (Exception e) {
            log.warn("PPT结构化解析失败，回退到降级解析, fileName={}", fileName, e);
            if (fallback != null) {
                try {
                    return fallback.fallback(inputStream, fileName);
                } catch (Exception ex) {
                    throw new RuntimeException("PPT解析失败: " + fileName, ex);
                }
            }
            throw new RuntimeException("PPT解析失败: " + fileName, e);
        }
    }

    /**
     * 提取形状中的文本内容（支持文本框、表格、组合形状等）
     *
     * @param shape            PPT形状对象
     * @param processedShapes  已处理的形状集合（用于避免重复）
     * @return                 提取的文本内容
     */
    private String extractShapeText(XSLFShape shape, HashSet<XSLFTextShape> processedShapes) {
        StringBuilder sb = new StringBuilder();

        // 1. 处理文本框（包括占位符）
        if (shape instanceof XSLFTextShape textShape) {
            // 避免重复处理同一个占位符
            if (processedShapes != null && processedShapes.contains(textShape)) {
                String text = textShape.getText();
                if (text != null && !text.isBlank()) {
                    return text.trim();
                }
            }
            return null;
        }

        // 2. 处理表格（重点）
        else if (shape instanceof XSLFTable table) {
            return extractTableText(table);
        }

        // 3. 处理组合形状（递归提取内部形状）
        else if (shape instanceof XSLFGroupShape groupShape) {
            List<XSLFShape> shapes = groupShape.getShapes();
            if (shapes != null && !shapes.isEmpty()) {
                for (XSLFShape innerShape : shapes) {
                    String innerText = extractShapeText(innerShape, processedShapes);
                    if (innerText != null && !innerText.isBlank()) {
                        sb.append(innerText).append("\n");
                    }
                }
                return sb.toString().trim();
            }
        }

        return null;
    }

    /**
     * 提取演讲者备注文本
     *
     * @param notes 备注对象
     * @return 备注文本
     */
    private String extractNotesText(XSLFNotes notes) {
        StringBuilder sb = new StringBuilder();
        try {
            for (List<XSLFTextParagraph> paras : notes.getTextParagraphs()) {
                for (XSLFTextParagraph para : paras) {
                    if (para != null) {
                        String text = para.getText();
                        if (text != null && !text.isBlank()) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(text.trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("备注文本提取失败: {}", e.getMessage());
        }
        return sb.toString().trim();
    }

    /**
     * 提取表格内容，保留行列结构
     *
     * @param table  PPT表格对象
     * @return       格式化的表格文本
     */
    private String extractTableText(XSLFTable table) {
        StringBuilder sb = new StringBuilder();

        if (table == null) {
            return "";
        }

        // 添加表格标记
        sb.append("【表格】\n");

        List<XSLFTableRow> rows = table.getRows();
        if (rows == null || rows.isEmpty()) {
            return "";
        }

        // 计算最大列数（处理合并单元格）
        int maxCols = 0;
        for (XSLFTableRow row : rows) {
            maxCols = Math.max(maxCols, row.getCells().size());
        }

        if (maxCols == 0) {
            return "";
        }

        // 提取每行每列的内容
        for (int i = 0; i < rows.size(); i++) {
            XSLFTableRow row = rows.get(i);
            List<XSLFTableCell> cells = row.getCells();

            List<String> rowContent = new ArrayList<>();
            for (int j = 0; j < maxCols; j++) {
                if (j < cells.size()) {
                    XSLFTableCell cell = cells.get(j);
                    String cellText = getCellText(cell);
                    rowContent.add(cellText);
                } else {
                    rowContent.add(""); // 合并单元格导致的空位
                }
            }

            // 用竖线分隔单元格
            String rowText = String.join(" | ", rowContent);

            // 过滤空行
            if (!rowText.replace("|", "").trim().isEmpty()) {
                sb.append(rowText);

                // 如果是第一行（通常是表头），添加分隔线
                if (i == 0 && rows.size() > 1) {
                    sb.append("\n");
                    // 添加 Markdown 风格的分隔线
                    for (int j = 0; j < maxCols; j++) {
                        sb.append("---");
                        if (j < maxCols - 1) sb.append(" | ");
                    }
                }
                sb.append("\n");
            }
        }

        sb.append("【表格结束】\n");
        return sb.toString();
    }

    /**
     * 获取表格单元格的文本内容
     *
     * @param cell  表格单元格
     * @return      单元格文本
     */
    private String getCellText(XSLFTableCell cell) {
        if (cell == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 获取单元格中的所有文本段落
        List<XSLFTextParagraph> paragraphs = cell.getTextParagraphs();
        if (paragraphs != null && !paragraphs.isEmpty()) {
            for (int i = 0; i < paragraphs.size(); i++) {
                XSLFTextParagraph para = paragraphs.get(i);
                String paraText = para.getText();
                if (paraText != null && !paraText.isBlank()) {
                    if (i > 0) {
                        sb.append(" "); // 段落间用空格分隔
                    }
                    sb.append(paraText.trim());
                }
            }
        }

        return sb.toString().trim();
    }
}
