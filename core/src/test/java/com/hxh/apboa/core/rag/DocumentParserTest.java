package com.hxh.apboa.core.rag;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentParserTest {

    private final DocumentParser parser = new DocumentParser();

    @Test
    void officeAndPdfExtensionsAreSupported() {
        for (String fileName : new String[]{
                "sample.pdf", "sample.doc", "sample.docx", "sample.xls",
                "sample.xlsx", "sample.ppt", "sample.pptx"
        }) {
            assertFalse(parser.isNotSupported(fileName), fileName + " should be supported");
        }
    }

    @Test
    void parseGeneratedPdf() throws Exception {
        String content = parser.parse(new ByteArrayInputStream(createPdf()), "sample.pdf");
        assertTrue(content.contains("Apboa PDF multimodal parser smoke"));
    }

    @Test
    void parseGeneratedDocx() throws Exception {
        String content = parser.parse(new ByteArrayInputStream(createDocx()), "sample.docx");
        assertTrue(content.contains("Apboa DOCX multimodal parser smoke"));
    }

    @Test
    void parseGeneratedXlsx() throws Exception {
        String content = parser.parse(new ByteArrayInputStream(createXlsx()), "sample.xlsx");
        assertTrue(content.contains("Apboa XLSX multimodal parser smoke"));
    }

    @Test
    void parseGeneratedPptx() throws Exception {
        String content = parser.parse(new ByteArrayInputStream(createPptx()), "sample.pptx");
        assertTrue(content.contains("Apboa PPTX multimodal parser smoke"));
    }

    private byte[] createPdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA, 12);
                stream.newLineAtOffset(72, 720);
                stream.showText("Apboa PDF multimodal parser smoke");
                stream.endText();
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private byte[] createDocx() throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText("Apboa DOCX multimodal parser smoke");
            document.write(out);
            return out.toByteArray();
        }
    }

    private byte[] createXlsx() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Row row = workbook.createSheet("Sheet1").createRow(0);
            row.createCell(0).setCellValue("Apboa XLSX multimodal parser smoke");
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] createPptx() throws Exception {
        try (XMLSlideShow slideShow = new XMLSlideShow(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSLFSlide slide = slideShow.createSlide();
            XSLFTextBox textBox = slide.createTextBox();
            textBox.setAnchor(new Rectangle(50, 50, 500, 80));
            textBox.setText("Apboa PPTX multimodal parser smoke");
            slideShow.write(out);
            return out.toByteArray();
        }
    }
}
