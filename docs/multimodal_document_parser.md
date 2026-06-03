# 多模态文档解析

本地知识库支持上传 `pdf`、`doc`、`docx`、`xls`、`xlsx`、`ppt`、`pptx` 文档。默认情况下系统会使用 Tika/POI 提取文本、表格和演示文稿结构；当后端启用 `rag.multimodal-parser.enabled=true` 并配置 OpenAI-compatible 视觉模型后，系统会额外解析文档中的视觉信息。

## 支持范围

- PDF：渲染前若干页为图片，交给多模态模型描述页面、图表、流程、截图和版式信息。
- PPT/PPTX：渲染前若干页幻灯片，补充视觉布局、图表、流程图和图片内容描述。
- DOC/DOCX：在文本解析基础上提取内嵌图片并生成描述。
- XLS/XLSX：在结构化表格解析基础上提取内嵌图片并生成描述。

多模态解析结果会追加到普通文本解析结果后，一起进入分块、向量化和检索流程。未启用多模态配置时，现有文档上传与解析行为不变。

## Docker 配置

```env
RAG_MULTIMODAL_PARSER_ENABLED=true
RAG_MULTIMODAL_PARSER_BASE_URL=https://your-openai-compatible-host
RAG_MULTIMODAL_PARSER_API_KEY=your-api-key
RAG_MULTIMODAL_PARSER_MODEL=your-vision-model
RAG_MULTIMODAL_PARSER_MAX_IMAGES=8
RAG_MULTIMODAL_PARSER_MAX_PDF_PAGES=4
RAG_MULTIMODAL_PARSER_MAX_PPT_SLIDES=8
RAG_MULTIMODAL_PARSER_IMAGE_MAX_WIDTH=1600
RAG_MULTIMODAL_PARSER_TIMEOUT_SECONDS=60
RAG_MULTIMODAL_PARSER_BUFFER_SIZE_MB=20
```

`RAG_MULTIMODAL_PARSER_BASE_URL` 使用 OpenAI-compatible 服务根地址，程序会调用 `{baseUrl}/v1/chat/completions`。

如需通过环境变量读取密钥，可配置：

```env
RAG_MULTIMODAL_PARSER_API_KEY=
RAG_MULTIMODAL_PARSER_ENV_VAR_NAME=YOUR_VISION_API_KEY_ENV
```
