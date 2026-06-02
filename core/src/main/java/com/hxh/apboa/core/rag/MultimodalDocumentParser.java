package com.hxh.apboa.core.rag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Optional multimodal document parser. It keeps the normal text extraction path intact and
 * appends vision-model descriptions for visual pages, slides, and embedded images.
 */
@Component
public class MultimodalDocumentParser {

    private static final Logger log = LoggerFactory.getLogger(MultimodalDocumentParser.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String envVarName;
    private final String model;
    private final String prompt;
    private final int maxImagesPerDocument;
    private final int maxPdfPages;
    private final int maxPptSlides;
    private final int imageMaxWidth;
    private final int timeoutSeconds;
    private final int bufferSizeMb;

    public MultimodalDocumentParser(
            @Value("${rag.multimodal-parser.enabled:false}") boolean enabled,
            @Value("${rag.multimodal-parser.base-url:}") String baseUrl,
            @Value("${rag.multimodal-parser.api-key:}") String apiKey,
            @Value("${rag.multimodal-parser.env-var-name:}") String envVarName,
            @Value("${rag.multimodal-parser.model:}") String model,
            @Value("${rag.multimodal-parser.prompt:}") String prompt,
            @Value("${rag.multimodal-parser.max-images-per-document:8}") int maxImagesPerDocument,
            @Value("${rag.multimodal-parser.max-pdf-pages:4}") int maxPdfPages,
            @Value("${rag.multimodal-parser.max-ppt-slides:8}") int maxPptSlides,
            @Value("${rag.multimodal-parser.image-max-width:1600}") int imageMaxWidth,
            @Value("${rag.multimodal-parser.timeout-seconds:60}") int timeoutSeconds,
            @Value("${rag.multimodal-parser.buffer-size-mb:20}") int bufferSizeMb) {
        this.enabled = enabled;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.envVarName = envVarName;
        this.model = model;
        this.prompt = prompt;
        this.maxImagesPerDocument = maxImagesPerDocument;
        this.maxPdfPages = maxPdfPages;
        this.maxPptSlides = maxPptSlides;
        this.imageMaxWidth = imageMaxWidth;
        this.timeoutSeconds = timeoutSeconds;
        this.bufferSizeMb = bufferSizeMb;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String parse(byte[] fileBytes, String fileName) {
        if (!enabled || fileBytes == null || fileBytes.length == 0) {
            return "";
        }
        String token = resolveApiKey();
        if (isBlank(baseUrl) || isBlank(model) || isBlank(token)) {
            log.warn("多模态文档解析已开启但配置不完整，跳过视觉解析, fileName={}", fileName);
            return "";
        }

        List<VisualImage> images = collectImages(fileBytes, fileName);
        if (images.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder("## 多模态视觉解析\n\n");
        int successCount = 0;
        for (VisualImage image : images) {
            try {
                String description = describeImage(image, token);
                if (!isBlank(description)) {
                    sb.append("### ").append(image.label()).append("\n\n");
                    sb.append(description.trim()).append("\n\n");
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("多模态视觉解析失败, fileName={}, image={}: {}", fileName, image.label(), e.getMessage());
            }
        }
        return successCount == 0 ? "" : sb.toString().trim();
    }

    private List<VisualImage> collectImages(byte[] fileBytes, String fileName) {
        String ext = extractExtension(fileName);
        List<VisualImage> images = new ArrayList<>();
        try {
            switch (ext) {
                case "pdf" -> images.addAll(renderPdfPages(fileBytes));
                case "pptx" -> images.addAll(renderPptxSlides(fileBytes));
                case "ppt" -> images.addAll(renderPptSlides(fileBytes));
                case "docx" -> images.addAll(extractDocxImages(fileBytes));
                case "doc" -> images.addAll(extractDocImages(fileBytes));
                case "xlsx", "xls" -> images.addAll(extractWorkbookImages(fileBytes));
                default -> {
                }
            }
        } catch (Exception e) {
            log.warn("提取文档视觉内容失败, fileName={}: {}", fileName, e.getMessage());
        }
        return images.size() > maxImagesPerDocument
                ? new ArrayList<>(images.subList(0, maxImagesPerDocument))
                : images;
    }

    private List<VisualImage> renderPdfPages(byte[] fileBytes) throws Exception {
        List<VisualImage> images = new ArrayList<>();
        try (PDDocument document = PDDocument.load(fileBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = Math.min(document.getNumberOfPages(), maxPdfPages);
            for (int i = 0; i < pages; i++) {
                BufferedImage page = renderer.renderImageWithDPI(i, 144, ImageType.RGB);
                images.add(toVisualImage("PDF 第 " + (i + 1) + " 页", page));
            }
        }
        return images;
    }

    private List<VisualImage> renderPptxSlides(byte[] fileBytes) throws Exception {
        List<VisualImage> images = new ArrayList<>();
        try (XMLSlideShow slideShow = new XMLSlideShow(new ByteArrayInputStream(fileBytes))) {
            Dimension size = slideShow.getPageSize();
            List<XSLFSlide> slides = slideShow.getSlides();
            int count = Math.min(slides.size(), maxPptSlides);
            for (int i = 0; i < count; i++) {
                images.add(toVisualImage("PPTX 第 " + (i + 1) + " 页", renderSlide(slides.get(i), size)));
            }
            if (images.isEmpty()) {
                for (XSLFPictureData picture : slideShow.getPictureData()) {
                    images.add(new VisualImage("PPTX 内嵌图片", picture.getData(), picture.getContentType()));
                }
            }
        }
        return images;
    }

    private List<VisualImage> renderPptSlides(byte[] fileBytes) throws Exception {
        List<VisualImage> images = new ArrayList<>();
        try (HSLFSlideShow slideShow = new HSLFSlideShow(new ByteArrayInputStream(fileBytes))) {
            Dimension size = slideShow.getPageSize();
            List<HSLFSlide> slides = slideShow.getSlides();
            int count = Math.min(slides.size(), maxPptSlides);
            for (int i = 0; i < count; i++) {
                images.add(toVisualImage("PPT 第 " + (i + 1) + " 页", renderSlide(slides.get(i), size)));
            }
        }
        return images;
    }

    private BufferedImage renderSlide(Slide<?, ?> slide, Dimension size) {
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setPaint(java.awt.Color.WHITE);
            graphics.fillRect(0, 0, size.width, size.height);
            slide.draw(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private List<VisualImage> extractDocxImages(byte[] fileBytes) throws Exception {
        List<VisualImage> images = new ArrayList<>();
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            int index = 1;
            for (XWPFPictureData picture : document.getAllPictures()) {
                images.add(new VisualImage("DOCX 内嵌图片 " + index++, picture.getData(), picture.getPackagePart().getContentType()));
            }
        }
        return images;
    }

    private List<VisualImage> extractDocImages(byte[] fileBytes) throws Exception {
        List<VisualImage> images = new ArrayList<>();
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(fileBytes))) {
            int index = 1;
            for (Picture picture : document.getPicturesTable().getAllPictures()) {
                images.add(new VisualImage("DOC 内嵌图片 " + index++, picture.getContent(),
                        mimeTypeFromExtension(picture.suggestFileExtension())));
            }
        }
        return images;
    }

    private List<VisualImage> extractWorkbookImages(byte[] fileBytes) throws Exception {
        List<VisualImage> images = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileBytes))) {
            int index = 1;
            for (org.apache.poi.ss.usermodel.PictureData picture : workbook.getAllPictures()) {
                images.add(new VisualImage("Excel 内嵌图片 " + index++, picture.getData(), picture.getMimeType()));
            }
        }
        return images;
    }

    private VisualImage toVisualImage(String label, BufferedImage image) throws Exception {
        BufferedImage scaled = scaleImage(image);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(scaled, "jpg", out);
        return new VisualImage(label, out.toByteArray(), "image/jpeg");
    }

    private BufferedImage scaleImage(BufferedImage source) {
        if (source.getWidth() <= imageMaxWidth) {
            return source;
        }
        int targetWidth = imageMaxWidth;
        int targetHeight = Math.max(1, Math.round((float) source.getHeight() * targetWidth / source.getWidth()));
        BufferedImage scaled = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private String describeImage(VisualImage image, String token) throws Exception {
        URI uri = buildChatCompletionsUri();
        String base = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        String path = uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());

        WebClient client = WebClient.builder()
                .baseUrl(base)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(bufferSizeMb * 1024 * 1024))
                        .build())
                .defaultHeader("Authorization", "Bearer " + token)
                .build();

        String dataUrl = "data:" + normalizeMimeType(image.mimeType()) + ";base64,"
                + Base64.getEncoder().encodeToString(image.data());
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", buildPrompt(image.label())),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                        )
                ))
        );

        String response = client.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(OBJECT_MAPPER.writeValueAsString(body))
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(timeoutSeconds));
        return parseContent(response);
    }

    private URI buildChatCompletionsUri() {
        String url = baseUrl.trim();
        if (!url.endsWith("/chat/completions")) {
            url = url.endsWith("/") ? url + "v1/chat/completions" : url + "/v1/chat/completions";
        }
        return URI.create(url);
    }

    private String buildPrompt(String label) {
        if (!isBlank(prompt)) {
            return prompt + "\n\n当前图片来源: " + label;
        }
        return """
                请解析这张来自文档的图片，输出适合 RAG 检索的中文文本。
                要求：
                1. 描述页面、图表、流程图、截图、表格或图片中的关键信息。
                2. 尽量保留可见标题、字段、指标、时间、结论和业务含义。
                3. 不要编造图片中不存在的信息。

                当前图片来源: %s
                """.formatted(label);
    }

    private String parseContent(String response) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(response);
        JsonNode content = root.path("choices").path(0).path("message").path("content");
        if (content.isTextual()) {
            return content.asText();
        }
        if (content.isArray()) {
            StringBuilder sb = new StringBuilder();
            for (JsonNode item : content) {
                JsonNode text = item.path("text");
                if (text.isTextual()) {
                    sb.append(text.asText()).append("\n");
                }
            }
            return sb.toString().trim();
        }
        return "";
    }

    private String resolveApiKey() {
        if (!isBlank(envVarName)) {
            String value = System.getenv(envVarName);
            if (!isBlank(value)) {
                return value;
            }
            log.warn("多模态解析环境变量 {} 未设置或为空，回退使用 api-key 配置", envVarName);
        }
        if (!isBlank(apiKey) && apiKey.startsWith("${") && apiKey.endsWith("}")) {
            String envName = apiKey.substring(2, apiKey.length() - 1);
            return System.getenv(envName);
        }
        return apiKey;
    }

    private String normalizeMimeType(String mimeType) {
        return isBlank(mimeType) ? "image/jpeg" : mimeType;
    }

    private String mimeTypeFromExtension(String extension) {
        if (isBlank(extension)) {
            return "image/jpeg";
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "bmp" -> "image/bmp";
            case "tif", "tiff" -> "image/tiff";
            case "webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record VisualImage(String label, byte[] data, String mimeType) {
    }
}
