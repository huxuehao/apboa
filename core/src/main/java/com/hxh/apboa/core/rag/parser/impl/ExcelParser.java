package com.hxh.apboa.core.rag.parser.impl;

import com.hxh.apboa.core.rag.parser.IParser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 描述：Excel文档解析器，支持xlsx/xls/csv格式的结构化提取
 *
 * @author huxuehao
 **/
public class ExcelParser implements IParser {
    private static final Logger log = LoggerFactory.getLogger(ExcelParser.class);

    /** 行分隔符，用于在每行后追加的分块标记，默认换行 */
    private String rowDelimiter = "\n";

    /**
     * 设置行分隔符
     *
     * @param rowDelimiter 行分隔符
     */
    public void setRowDelimiter(String rowDelimiter) {
        if (rowDelimiter != null && !rowDelimiter.isEmpty()) {
            this.rowDelimiter = rowDelimiter;
        }
    }

    @Override
    public String parse(InputStream inputStream, String fileName, ParseFallback fallback) {
        try {
            String ext = extractExtension(fileName);
            if ("csv".equals(ext)) {
                return parseCsv(inputStream);
            }
            return parseExcel(inputStream);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Excel结构化解析失败，尝试降级方案, fileName={}", fileName, e);
            if (fallback != null) {
                try {
                    return fallback.fallback(inputStream, fileName);
                } catch (Exception ex) {
                    throw new RuntimeException("Excel解析失败: " + fileName, ex);
                }
            }
            throw new RuntimeException("Excel解析失败: " + fileName, e);
        }
    }

    /**
     * Excel结构化解析：按Sheet逐行提取，支持表头检测、日期处理、合并单元格
     *
     * @param inputStream 文档输入流
     * @return 解析后的Markdown表格文本
     */
    private String parseExcel(InputStream inputStream) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            StringBuilder result = new StringBuilder();

            int totalSheets = workbook.getNumberOfSheets();
            for (int i = 0; i < totalSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                // 跳过空Sheet
                if (sheet.getPhysicalNumberOfRows() == 0) {
                    continue;
                }

                if (sheetName != null && !sheetName.isBlank()) {
                    result.append("## ").append(sheetName).append("\n\n");
                }

                // 收集合并区域信息，用于跳过被合并的单元格
                Set<String> mergedCells = collectMergedCells(sheet);

                // 确定有效列范围
                int maxColIndex = getMaxColIndex(sheet);

                String sheetContent = extractSheetContent(sheet, maxColIndex, mergedCells);
                if (!sheetContent.isBlank()) {
                    result.append(sheetContent);
                    result.append("\n");
                }
            }

            String content = result.toString();
            return content.isBlank() ? "" : cleanContent(content);
        }
    }

    /**
     * CSV文件解析：使用BufferedReader逐行读取，按逗号分隔
     *
     * @param inputStream 文档输入流
     * @return 解析后的文本
     */
    private String parseCsv(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            List<String[]> allRows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] cells = parseCsvLine(line);
                allRows.add(cells);
            }

            if (allRows.isEmpty()) {
                return "";
            }

            // 计算最大列数
            int maxCols = 0;
            for (String[] row : allRows) {
                maxCols = Math.max(maxCols, row.length);
            }

            for (int i = 0; i < allRows.size(); i++) {
                String[] row = allRows.get(i);
                String[] padded = new String[maxCols];
                for (int j = 0; j < maxCols; j++) {
                    padded[j] = j < row.length ? row[j].trim() : "";
                }
                String rowText = String.join(" | ", padded);

                if (!rowText.replace("|", "").trim().isEmpty()) {
                    sb.append(rowText);
                    // 表头分隔线
                    if (i == 0 && allRows.size() > 1) {
                        sb.append("\n");
                        for (int j = 0; j < maxCols; j++) {
                            sb.append("---");
                            if (j < maxCols - 1) sb.append(" | ");
                        }
                    }
                    sb.append(rowDelimiter);
                }
            }
        }

        String content = sb.toString();
        return content.isBlank() ? "" : cleanContent(content);
    }

    /**
     * 解析CSV单行，处理引号包裹的字段
     *
     * @param line CSV行文本
     * @return 字段数组
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());

        return fields.toArray(new String[0]);
    }

    /**
     * 收集Sheet中所有被合并区域覆盖的单元格（跳过合并区域中非左上角的单元格）
     *
     * @param sheet Excel Sheet
     * @return 被合并的单元格坐标集合，格式为"row-col"
     */
    private Set<String> collectMergedCells(Sheet sheet) {
        Set<String> merged = new HashSet<>();
        List<CellRangeAddress> mergedRegions = sheet.getMergedRegions();
        if (mergedRegions != null) {
            for (CellRangeAddress region : mergedRegions) {
                for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                    for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                        // 跳过左上角（保留其值）
                        if (r == region.getFirstRow() && c == region.getFirstColumn()) {
                            continue;
                        }
                        merged.add(r + "-" + c);
                    }
                }
            }
        }
        return merged;
    }

    /**
     * 计算Sheet的最大有效列索引
     *
     * @param sheet Excel Sheet
     * @return 最大列索引
     */
    private int getMaxColIndex(Sheet sheet) {
        int maxCol = 0;
        for (Row row : sheet) {
            if (row != null) {
                maxCol = Math.max(maxCol, row.getLastCellNum());
            }
        }
        return maxCol;
    }

    /**
     * 提取Sheet内容为Markdown表格格式
     *
     * @param sheet       Excel Sheet
     * @param maxColIndex 最大列索引
     * @param mergedCells 被合并的单元格集合
     * @return Markdown表格文本
     */
    private String extractSheetContent(Sheet sheet, int maxColIndex, Set<String> mergedCells) {
        if (maxColIndex <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean headerProcessed = false;

        for (Row row : sheet) {
            if (row == null) {
                continue;
            }

            List<String> cells = new ArrayList<>();
            boolean hasContent = false;

            for (int j = 0; j < maxColIndex; j++) {
                // 跳过被合并的非左上角单元格
                if (mergedCells.contains(row.getRowNum() + "-" + j)) {
                    cells.add("");
                    continue;
                }

                Cell cell = row.getCell(j, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                String cellText = getCellText(cell);
                if (!cellText.isEmpty()) {
                    hasContent = true;
                }
                cells.add(cellText);
            }

            // 跳过完全空行
            if (!hasContent) {
                continue;
            }

            String rowText = String.join(" | ", cells);
            sb.append(rowText);

            // 第一行作为表头，添加Markdown表格分隔线
            if (!headerProcessed && sheet.getPhysicalNumberOfRows() > 1) {
                headerProcessed = true;
                sb.append("\n");
                for (int j = 0; j < maxColIndex; j++) {
                    sb.append("---");
                    if (j < maxColIndex - 1) sb.append(" | ");
                }
            }

            sb.append(rowDelimiter);
        }

        return sb.toString();
    }

    /**
     * 获取单元格文本值，支持多种数据类型
     *
     * @param cell 单元格
     * @return 单元格文本
     */
    private String getCellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    try {
                        LocalDateTime dateTime = cell.getLocalDateTimeCellValue();
                        yield dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } catch (Exception e) {
                        yield String.valueOf(cell.getNumericCellValue());
                    }
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        double numVal = cell.getNumericCellValue();
                        if (numVal == Math.floor(numVal) && !Double.isInfinite(numVal)) {
                            yield String.valueOf((long) numVal);
                        }
                        yield String.valueOf(numVal);
                    } catch (Exception ex) {
                        yield cell.getCellFormula();
                    }
                }
            }
            default -> "";
        };
    }

    /**
     * 提取文件扩展名
     *
     * @param fileName 文件名
     * @return 小写扩展名，无扩展名时返回null
     */
    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
