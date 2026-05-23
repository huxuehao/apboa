# APBOA RAG 知识库文档结构化解析与分块改造设计

## 1. 背景与目标

当前 APBOA 本地 RAG 链路集中在 [DocumentParser](/D:/WorkSpace/apboa/core/src/main/java/com/hxh/apboa/core/rag/DocumentParser.java)、[LocalRagService](/D:/WorkSpace/apboa/core/src/main/java/com/hxh/apboa/core/rag/service/LocalRagService.java)、[SemanticChunker](/D:/WorkSpace/apboa/core/src/main/java/com/hxh/apboa/core/rag/service/SemanticChunker.java)。

现状问题主要有三类：

1. 解析结果以线性纯文本为主，页、标题、表格、图片、幻灯片、Sheet 等结构信息在解析阶段大量丢失。
2. 分块阶段主要面向字符串启发式切分，无法稳定利用版面结构，表格、标题与正文关系容易被拆散。
3. 对扫描件 PDF、图片型 PDF、复杂表格的处理能力不足，缺少本地 OCR 与版面恢复链路。

本次改造目标是：

1. 参考 RAGFlow 的“布局分析先行 + 结构感知分块”思路，增强文档结构化解析与版面保留。
2. 参考 Dify 的“格式化抽取器 + 递归 splitter + parent-child”思路，增强超长内容分块与上下文回补能力。
3. 在不改变 APBOA 现有 `DocumentParser -> LocalRagService -> QdrantVectorStore` 主链路的前提下，以增量方式补齐本地知识库在文档解析与分块方面的不足。
4. 支持纯本地运行，并允许新增 Java 侧 OCR / PDF 处理依赖。

## 2. 外部项目源码分析结论

### 2.1 RAGFlow 的实现方式

本次对照的关键源码位于：

1. `D:\WorkSpace\apboa\.tmp\ragflow\deepdoc\parser\pdf_parser.py`
2. `D:\WorkSpace\apboa\.tmp\ragflow\rag\flow\chunker\title_chunker\common.py`
3. `D:\WorkSpace\apboa\.tmp\ragflow\rag\flow\chunker\token_chunker.py`

结论如下：

1. PDF 解析不是直接输出纯文本，而是先走 OCR、layout recognition、table structure recognition，再给每个文本框挂 `layout_type/layoutno/page/position` 等信息。
2. 版面类型会显式区分 `title/text/table/figure/header/footer/page number/caption` 等组件，后续 chunker 基于这些结构组块。
3. 表格和图片不是简单拼接到文本中，而是作为独立内容块处理，并可挂接上下文。
4. 标题分块不是固定长度切分，而是优先用目录、正则层级、版面标题特征来构造层级 chunk。
5. Token chunker 只是在结构化基础上做二次切分，而不是替代结构化解析。

对 APBOA 的启发是：

1. 结构化解析必须先于分块。
2. 分块的输入应该是“结构块流”，不是“单一长字符串”。
3. 表格、图片、标题、页级位置信息必须进入 metadata，而不是仅用于调试。

### 2.2 Dify 的实现方式

本次对照的关键源码位于：

1. `D:\WorkSpace\apboa\.tmp\dify\api\core\rag\extractor\pdf_extractor.py`
2. `D:\WorkSpace\apboa\.tmp\dify\api\core\rag\index_processor\index_processor_base.py`
3. `D:\WorkSpace\apboa\.tmp\dify\api\core\rag\index_processor\processor\parent_child_index_processor.py`
4. `D:\WorkSpace\apboa\.tmp\dify\api\core\rag\splitter\fixed_text_splitter.py`
5. `D:\WorkSpace\apboa\.tmp\dify\api\core\rag\splitter\text_splitter.py`

结论如下：

1. Dify 把“文件抽取”和“文本分块”明确拆开，先按文件类型得到 `Document` 列表，再统一走 splitter。
2. splitter 采用递归切分策略，优先按段落、句子、空格等语义边界递归降级，最后才做长度硬切。
3. parent-child 索引会保存父块的大上下文和子块的小召回单元，兼顾召回精度和回答上下文完整性。
4. PDF extractor 以页为单位抽取文本和图片，结构恢复能力弱于 RAGFlow，但工程边界清晰、可维护性较高。

对 APBOA 的启发是：

1. 解析层和分块层边界要清晰。
2. 超长文本块需要独立递归切分器，而不是在结构识别阶段直接截断。
3. parent-child 是适合作为 APBOA 后续增强的检索结构，但可以先以 metadata 和轻量回补的方式增量引入。

## 3. 总体设计

本次采用“结构化解析 + 版面保留 + 结构优先分块 + 递归切分 + 轻量 parent-child”的组合方案。

主链路保持不变：

1. 上传文档
2. 进入 `DocumentParser`
3. 解析为结构化中间模型
4. 进入新的结构感知分块器
5. 生成 chunk、embedding、向量存储
6. 检索时按 child chunk 召回，并按 metadata 回补结构上下文

不采用的方案：

1. 不仅仅在现有 `SemanticChunker` 上继续追加字符串规则。
2. 不直接把 RAGFlow 的 Python 视觉模型链路整套迁移到 Java。
3. 不一次性重做整个检索层或引入独立 OCR 服务。

## 4. 结构化中间模型设计

在 `core.rag` 下新增结构化文档中间模型，建议包路径为：

`com.hxh.apboa.core.rag.parse`

建议新增对象：

### 4.1 ParsedDocument

文档级结构，包含：

1. `fileName`
2. `parserType`
3. `parseMethod`
4. `language`
5. `pageCount`
6. `scanned`
7. `pages`
8. `metadata`

### 4.2 ParsedPage

页级结构，包含：

1. `pageNumber`
2. `width`
3. `height`
4. `rotation`
5. `blocks`

### 4.3 ParsedBlock

统一块模型，包含：

1. `blockType`
2. `text`
3. `order`
4. `level`
5. `pageNumber`
6. `bbox`
7. `sectionPath`
8. `attributes`
9. `children`

### 4.4 BlockType

至少覆盖：

1. `TITLE`
2. `PARAGRAPH`
3. `LIST`
4. `TABLE`
5. `TABLE_ROW`
6. `TABLE_CAPTION`
7. `FIGURE`
8. `FIGURE_CAPTION`
9. `HEADER`
10. `FOOTER`
11. `PAGE_NUMBER`
12. `SLIDE_TITLE`
13. `SHEET`
14. `CODE`

### 4.5 BoundingBox

统一位置模型，包含：

1. `x0`
2. `y0`
3. `x1`
4. `y1`

该结构的核心目的是在解析阶段把结构和版面保留下来，供后续 chunker 与检索使用，而不是只做一次性文本拼接。

## 5. 解析层改造设计

### 5.1 DocumentParser 改造原则

[DocumentParser](/D:/WorkSpace/apboa/core/src/main/java/com/hxh/apboa/core/rag/DocumentParser.java) 由“直接返回字符串”扩展为：

1. 保留现有 `parse(InputStream, fileName)` 兼容接口。
2. 新增返回 `ParsedDocument` 的结构化接口。
3. 旧接口内部可降级调用新接口，再将结构块序列化为兼容文本。

### 5.2 非 PDF 文档解析

#### Word / Markdown / HTML / TXT

1. 优先保留标题层级、列表、代码块、表格、正文段落。
2. 解析为 `TITLE/PARAGRAPH/LIST/CODE/TABLE` 等结构块。
3. 对于无法结构化识别的内容再落回纯段落。

#### Excel / CSV

基于现有 [ExcelParser](/D:/WorkSpace/apboa/core/src/main/java/com/hxh/apboa/core/rag/parser/impl/ExcelParser.java) 增强：

1. 保留 `sheetName`
2. 将一张 Sheet 下的逻辑表区域识别为 `TABLE`
3. 表头单独识别并写入 `attributes.headerRow`
4. 行记录支持转为 `TABLE_ROW`
5. 合并单元格、日期、公式显示逻辑保留

#### PPT

基于现有 [PptParser](/D:/WorkSpace/apboa/core/src/main/java/com/hxh/apboa/core/rag/parser/impl/PptParser.java) 增强：

1. 区分 `SLIDE_TITLE`、正文、备注、表格
2. 保留 `slideNumber`
3. 保留 shape 顺序和表格结构

### 5.3 PDF 解析

新增 `StructuredPdfParser`，建议包路径：

`com.hxh.apboa.core.rag.parser.impl`

#### 解析链路

1. 先使用 PDF 文本提取能力获取字符、行、段落及坐标
2. 页级判定是否为扫描页
3. 扫描页走 OCR
4. OCR / 文本结果统一进入轻量布局分析
5. 表格区域优先做文本表格恢复，失败时走 OCR 表格恢复
6. 最终生成 `ParsedPage + ParsedBlock`

#### 页级扫描判定

建议综合以下特征：

1. 文本字符数过低
2. 乱码比例过高
3. 图像区域占比高
4. 页中大块文本坐标缺失

#### 轻量布局分析

本次不引入 RAGFlow 那类深度 layout 模型，先采用规则版：

1. 根据字体大小、字体样式、空白间隔、缩进判断标题和正文
2. 根据重复位置内容识别页眉页脚和页码
3. 根据 X 坐标分布与投影判断单栏 / 双栏阅读顺序
4. 根据文本框密度与线条区域判断表格候选块
5. 根据图片对象与 OCR 区域判断 figure 块

#### OCR 技术栈

允许新增：

1. `org.apache.pdfbox:pdfbox`
2. `net.sourceforge.tess4j:tess4j`
3. `technology.tabula:tabula`

OCR 采用本地 Tesseract，支持通过配置传入语言包，如 `chi_sim+eng`。

#### 表格恢复

1. 文本型 PDF 表格优先使用 Tabula / PDF 文本坐标恢复
2. 图片型表格先 OCR，再按列投影、分隔线、行对齐恢复为逻辑表格
3. 表格与表题尽量关联到同一 `sectionPath`

## 6. 分块层改造设计

### 6.1 StructureAwareChunker

新增结构感知分块器，建议路径：

`com.hxh.apboa.core.rag.service.StructureAwareChunker`

输入为 `ParsedDocument`，输出为 `StructuredChunk` 列表。

分块规则：

1. `TITLE + 下属短正文` 尽量同块
2. `TABLE` 单独成块，并可携带 table caption 与上文标题
3. `FIGURE` 单独成块，图说明按配置并入或拆分
4. `HEADER/FOOTER/PAGE_NUMBER` 默认不进入主召回块
5. Excel 按 `sheet -> logical table -> row group` 组块
6. PPT 按 `slide title -> body -> notes` 组块

### 6.2 RecursiveTextChunker

新增递归文本切分器，建议路径：

`com.hxh.apboa.core.rag.service.RecursiveTextChunker`

仅对超长文本结构块生效。

递归切分优先级：

1. 标题边界
2. 空段落 `\n\n`
3. 句号 / 问号 / 分号
4. 行边界
5. 空格
6. 最后按长度硬切

### 6.3 overlap 策略调整

现有 [SemanticChunker](/D:/WorkSpace/apboa/core/src/main/java/com/hxh/apboa/core/rag/service/SemanticChunker.java) 的 overlap 是“拼接前 N 个 chunk 内容”，这会导致 chunk 体积膨胀且边界不可控。

改造后：

1. overlap 统一改为字符或 token 窗口语义
2. 只在子块层面局部重叠
3. 表格、图片块不做字符串 overlap

### 6.4 parent-child 结构

本次引入轻量 parent-child 结构：

1. `parent chunk` 为完整结构块，如一个章节正文块或一个表格块
2. `child chunk` 为 parent 超长时递归切出的子块
3. 向量召回优先对 child chunk 建索引
4. metadata 中记录 `parentId/parentSummary/sectionPath/blockType`
5. 检索命中 child 后可按 `parentId` 回补父上下文

本次不强制单独建 parent 表结构，优先以 metadata 和服务逻辑实现。

## 7. 存储与元数据设计

继续沿用 [RagDocumentChunk](/D:/WorkSpace/apboa/common/src/main/java/com/hxh/apboa/common/entity/RagDocumentChunk.java)。

优先将新增结构信息写入 `metadata`，例如：

```json
{
  "blockType": "TABLE",
  "page": 3,
  "bbox": [72, 120, 510, 388],
  "sectionPath": ["3 财务分析", "3.2 收入结构"],
  "sheetName": null,
  "slideNumber": null,
  "parseMethod": "PDFBOX_OCR",
  "parentId": "p-10001",
  "role": "TABLE"
}
```

metadata 至少包含：

1. `blockType`
2. `page`
3. `bbox`
4. `sectionPath`
5. `sheetName`
6. `slideNumber`
7. `parseMethod`
8. `parserType`
9. `parentId`
10. `role`

## 8. 接口与配置设计

### 8.1 文档处理参数

在 [RagDocumentController](/D:/WorkSpace/apboa/biz/rag/src/main/java/com/hxh/apboa/rag/controller/RagDocumentController.java) 现有参数基础上扩展：

1. `parseMethod`: `AUTO | TEXT | OCR | HYBRID`
2. `layoutMode`: `NONE | BASIC | ENHANCED`
3. `enableParentChild`
4. `parentChunkSize`
5. `childChunkSize`
6. `tableStrategy`: `AUTO | TEXT_TABLE | OCR_TABLE`
7. `removeHeaderFooter`
8. `ocrLanguage`

### 8.2 知识库默认配置

在 `KnowledgeBaseConfig.retrievalConfig` 中新增对应默认值。

文档上传时的参数优先级高于知识库默认配置。

### 8.3 前端展示

建议后续增强以下页面：

1. [DocumentList.vue](/D:/WorkSpace/apboa/ui/src/components/rag/DocumentList.vue)
2. [ChunkDrawer.vue](/D:/WorkSpace/apboa/ui/src/components/rag/ChunkDrawer.vue)
3. [SearchTest.vue](/D:/WorkSpace/apboa/ui/src/components/rag/SearchTest.vue)

展示字段建议增加：

1. `blockType`
2. `page`
3. `sectionPath`
4. `parseMethod`
5. `sheetName / slideNumber`

## 9. 实施分期

### 第一阶段

1. 新增结构化中间模型
2. 改造 `DocumentParser`
3. 增强 Word / Markdown / Excel / PPT 的结构化解析
4. 新增递归文本切分器
5. metadata 持久化

### 第二阶段

1. 新增 `StructuredPdfParser`
2. 引入 PDFBox / Tess4J / Tabula
3. 支持扫描页 OCR
4. 支持轻量版面恢复
5. 支持表格恢复

### 第三阶段

1. 引入 parent-child 分块和检索回补
2. 补充前端结构化信息展示
3. 补充更多 PDF / Excel / PPT 场景优化

## 10. 测试策略

至少新增以下测试：

1. Markdown / Word 标题层级切块测试
2. Excel 多 Sheet / 表头 / 合并单元格测试
3. PPT 标题、正文、备注、表格测试
4. PDF 文本页解析测试
5. PDF 扫描页 OCR 测试
6. PDF 表格页恢复测试
7. metadata 保留 `page/blockType/sectionPath/bbox` 的测试
8. parent-child 回补测试
9. 递归切分器边界测试

## 11. 风险与约束

1. 本地 OCR 依赖 Tesseract 语言包，需要明确部署说明。
2. 规则版面分析无法完全达到 RAGFlow 深度布局模型的复杂页面效果。
3. 扫描件表格恢复效果受图像质量影响较大，需要保留回退策略。
4. 初期优先保证“结构不丢、回退稳定、元数据完整”，不追求一次性覆盖所有 PDF 复杂版式。

## 12. 结论

本次改造应将 APBOA 的本地知识库文档处理链路从“纯文本解析 + 字符串切块”升级为：

1. 结构化解析
2. 版面保留
3. 结构优先分块
4. 递归文本切分
5. 轻量 parent-child 召回回补

其中最关键的设计原则是：

1. 结构化解析先于分块
2. 版面信息进入 metadata
3. 表格、标题、页级来源不能在解析阶段丢失
4. OCR 是 PDF 解析增强的一部分，不是独立旁路
5. 对 APBOA 而言，应优先采用工程可控的 Java 方案，而不是直接迁移外部项目的整套模型体系
