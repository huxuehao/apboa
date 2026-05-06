package com.hxh.apboa.core.rag;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 文档解析器，基于Apache Tika支持多种文档格式，
 * 针对Excel和PPT做结构化提取以提升RAG效果
 *
 * @author huxuehao
 */
@Component
public class DocumentParser {

    private static final Logger log = LoggerFactory.getLogger(DocumentParser.class);

    private static final Set<String> EXCEL_TYPES = Set.of("xlsx", "xls", "csv");
    private static final Set<String> PPT_TYPES = Set.of("pptx", "ppt");
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "txt", "md", "pdf", "doc", "docx", "xlsx", "xls", "csv", "pptx", "ppt"
    );

    /**
     * 获取支持的文件类型集合
     */
    public Set<String> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    /**
     * 判断文件类型是否受支持
     */
    public boolean isSupported(String fileName) {
        String ext = extractExtension(fileName);
        return ext != null && SUPPORTED_TYPES.contains(ext);
    }

    /**
     * 解析文档输入流，提取纯文本内容
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名（用于日志和类型推断）
     * @return 解析后的文本内容
     */
    public String parse(InputStream inputStream, String fileName) {
        return parse(inputStream, fileName, null);
    }

    /**
     * 解析文档输入流，提取纯文本内容
     *
     * @param inputStream   文档输入流
     * @param fileName      文件名（用于日志和类型推断）
     * @param rowDelimiter  行分隔符，用于 Excel 等表格文档，在每行后追加此分隔符以便后续分块
     * @return 解析后的文本内容
     */
    public String parse(InputStream inputStream, String fileName, String rowDelimiter) {
        String ext = extractExtension(fileName);
        if (ext == null || !SUPPORTED_TYPES.contains(ext)) {
            throw new RuntimeException("不支持的文件类型: " + fileName);
        }

        try {
            if (EXCEL_TYPES.contains(ext)) {
                return parseExcel(inputStream, fileName, rowDelimiter);
            } else if (PPT_TYPES.contains(ext)) {
                return parsePpt(inputStream, fileName);
            } else {
                return parseByTika(inputStream, fileName);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败, fileName={}", fileName, e);
            throw new RuntimeException("文档解析失败: " + fileName, e);
        }
    }

    /**
     * 使用Tika通用解析（适用于txt、md、pdf、word）
     */
    private String parseByTika(InputStream inputStream, String fileName) throws Exception {
        Parser parser = new AutoDetectParser();
        BodyContentHandler handler = new BodyContentHandler(-1);
        Metadata metadata = new Metadata();
        metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
        ParseContext context = new ParseContext();
        context.set(Parser.class, parser);

        parser.parse(inputStream, handler, metadata, context);
        String content = handler.toString();

        if (content == null || content.isBlank()) {
            log.warn("文档解析结果为空, fileName={}", fileName);
            return "";
        }

        return cleanContent(content);
    }

    /**
     * Excel结构化解析：按Sheet逐行提取，保留表格结构
     *
     * @param inputStream   文档输入流
     * @param fileName      文件名
     * @param rowDelimiter  行分隔符，在每行后追加此分隔符以便后续分块
     */
    private String parseExcel(InputStream inputStream, String fileName, String rowDelimiter) {
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            StringBuilder sb = new StringBuilder();
            String effectiveDelimiter = (rowDelimiter != null && !rowDelimiter.isEmpty()) ? rowDelimiter : "\n";

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                if (sheetName != null && !sheetName.isBlank()) {
                    sb.append("## ").append(sheetName).append("\n\n");
                }

                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        cells.add(getCellText(cell));
                    }
                    String rowText = String.join(" | ", cells);
                    if (!rowText.replace("|", "").trim().isEmpty()) {
                        sb.append(rowText).append(effectiveDelimiter);
                    }
                }
                sb.append("\n");
            }

            workbook.close();
            String content = sb.toString();
            return content.isBlank() ? "" : cleanContent(content);
        } catch (Exception e) {
            log.warn("Excel结构化解析失败，回退到Tika解析, fileName={}", fileName, e);
            try {
                return parseByTika(inputStream, fileName);
            } catch (Exception ex) {
                throw new RuntimeException("Excel解析失败: " + fileName, ex);
            }
        }
    }

    /**
     * PPT结构化解析：按幻灯片逐页提取，保留页面结构
     */
    private String parsePpt(InputStream inputStream, String fileName) {
        try {
            XMLSlideShow slideShow = new XMLSlideShow(inputStream);
            StringBuilder sb = new StringBuilder();

            List<XSLFSlide> slides = slideShow.getSlides();
            for (int i = 0; i < slides.size(); i++) {
                XSLFSlide slide = slides.get(i);

                sb.append("## 第").append(i + 1).append("页\n\n");

                HashSet<XSLFTextShape> placeholderSet = new HashSet<>();
                for (XSLFTextShape placeholder : slide.getPlaceholders()) {
                    placeholderSet.add(placeholder);
                    String text = placeholder.getText();
                    if (text != null && !text.isBlank()) {
                        sb.append(text.trim()).append("\n\n");
                    }
                }

                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape && !placeholderSet.contains(textShape)) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text.trim()).append("\n\n");
                        }
                    }
                }
            }

            slideShow.close();
            String content = sb.toString();
            return content.isBlank() ? "" : cleanContent(content);
        } catch (Exception e) {
            log.warn("PPT结构化解析失败，回退到Tika解析, fileName={}", fileName, e);
            try {
                return parseByTika(inputStream, fileName);
            } catch (Exception ex) {
                throw new RuntimeException("PPT解析失败: " + fileName, ex);
            }
        }
    }

    /**
     * 获取单元格文本值
     */
    private String getCellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
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
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    /**
     * 清洗文本内容：去除多余空白、特殊字符
     */
    private String cleanContent(String content) {
        return content
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .replaceAll("[ \t]+", " ")
                .trim();
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
