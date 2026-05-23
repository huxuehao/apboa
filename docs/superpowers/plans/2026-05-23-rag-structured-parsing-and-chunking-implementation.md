# RAG Structured Parsing And Chunking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade APBOA local RAG document ingestion from plain-text parsing plus string chunking to structured parsing, layout-preserving metadata, recursive chunking, and local PDF OCR support.

**Architecture:** Keep the existing `DocumentParser -> LocalRagService -> QdrantVectorStore` flow, but insert a structured intermediate document model and new chunking pipeline. Non-PDF parsers emit structured blocks first, PDF uses text extraction plus OCR fallback and lightweight layout recovery, and retrieval persists block metadata to support later parent-child context restoration.

**Tech Stack:** Java 21, Spring Boot 3.4.9, Apache Tika, Apache POI, Apache PDFBox, Tess4J, Tabula, Jackson, JUnit, Maven

---

## File Map

### New files

- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parse\BlockType.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parse\BoundingBox.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parse\ParsedBlock.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parse\ParsedPage.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parse\ParsedDocument.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parser\impl\StructuredPdfParser.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\service\RecursiveTextChunker.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\service\StructureAwareChunker.java`
- `D:\WorkSpace\apboa\core\src\test\java\com\hxh\apboa\core\rag\service\RecursiveTextChunkerTest.java`
- `D:\WorkSpace\apboa\core\src\test\java\com\hxh\apboa\core\rag\service\StructureAwareChunkerTest.java`

### Modified files

- `D:\WorkSpace\apboa\common\pom.xml`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\DocumentParser.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parser\impl\ExcelParser.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parser\impl\PptParser.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parser\impl\TikaParser.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\service\LocalRagService.java`
- `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\service\SemanticChunker.java`
- `D:\WorkSpace\apboa\biz\rag\src\main\java\com\hxh\apboa\rag\controller\RagDocumentController.java`

## Task 1: Add Structured Parsing Model And Dependencies

**Files:**
- Create: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parse\BlockType.java`
- Create: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parse\BoundingBox.java`
- Create: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parse\ParsedBlock.java`
- Create: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parse\ParsedPage.java`
- Create: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parse\ParsedDocument.java`
- Modify: `D:\WorkSpace\apboa\common\pom.xml`

- [ ] Step 1: Add failing tests later through chunker tests that will require structured document types to compile.
- [ ] Step 2: Add PDF/OCR dependencies to `common/pom.xml`:

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
<dependency>
    <groupId>net.sourceforge.tess4j</groupId>
    <artifactId>tess4j</artifactId>
    <version>5.13.0</version>
</dependency>
<dependency>
    <groupId>technology.tabula</groupId>
    <artifactId>tabula</artifactId>
    <version>1.0.5</version>
</dependency>
```

- [ ] Step 3: Create the structured parsing model classes with focused responsibilities.
- [ ] Step 4: Run `mvn -pl core -am -DskipTests compile`.
- [ ] Step 5: Commit:

```bash
git add common/pom.xml core/src/main/java/com/hxh/apboa/core/rag/parse
git commit -m "feat: add structured rag parse model"
```

## Task 2: Upgrade Non-PDF Parsers To Emit Structured Documents

**Files:**
- Modify: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\DocumentParser.java`
- Modify: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parser\impl\ExcelParser.java`
- Modify: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parser\impl\PptParser.java`
- Modify: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parser\impl\TikaParser.java`

- [ ] Step 1: Extend parser implementations to expose `parseStructured(...)` methods returning `ParsedDocument`.
- [ ] Step 2: Keep `parse(...)` string APIs by serializing structured blocks back to plain text for backward compatibility.
- [ ] Step 3: For Excel emit `SHEET`, `TABLE`, `TABLE_ROW` blocks with `sheetName` and header metadata.
- [ ] Step 4: For PPT emit `SLIDE_TITLE`, `PARAGRAPH`, `TABLE` blocks with `slideNumber`.
- [ ] Step 5: For Tika-backed text docs infer headings, paragraphs, lists, code, and tables by lightweight textual rules.
- [ ] Step 6: Run `mvn -pl core -am -DskipTests compile`.
- [ ] Step 7: Commit:

```bash
git add core/src/main/java/com/hxh/apboa/core/rag/DocumentParser.java core/src/main/java/com/hxh/apboa/core/rag/parser/impl
git commit -m "feat: emit structured blocks for non-pdf rag parsers"
```

## Task 3: Implement Recursive And Structure-Aware Chunkers

**Files:**
- Create: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\service\RecursiveTextChunker.java`
- Create: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\service\StructureAwareChunker.java`
- Create: `D:\WorkSpace\apboa\core\src\test\java\com\hxh\apboa\core\rag\service\RecursiveTextChunkerTest.java`
- Create: `D:\WorkSpace\apboa\core\src\test\java\com\hxh\apboa\core\rag\service\StructureAwareChunkerTest.java`
- Modify: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\service\SemanticChunker.java`

- [ ] Step 1: Write failing tests covering recursive splitting and structure-preserving chunk assembly.
- [ ] Step 2: Run the targeted tests and confirm failure.
- [ ] Step 3: Implement `RecursiveTextChunker` with separator priority `title -> blank line -> sentence -> line -> space -> hard cut`.
- [ ] Step 4: Implement `StructureAwareChunker` that keeps title/body, table/caption, and slide/sheet boundaries.
- [ ] Step 5: Refactor `SemanticChunker` to delegate to the new chunkers instead of relying on plain string heuristics for all paths.
- [ ] Step 6: Run:

```bash
mvn -pl core -am test -Dtest=RecursiveTextChunkerTest,StructureAwareChunkerTest
```

- [ ] Step 7: Commit:

```bash
git add core/src/main/java/com/hxh/apboa/core/rag/service core/src/test/java/com/hxh/apboa/core/rag/service
git commit -m "feat: add structure aware rag chunking"
```

## Task 4: Add Structured PDF Parsing With OCR Fallback

**Files:**
- Create: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\parser\impl\StructuredPdfParser.java`
- Modify: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\DocumentParser.java`

- [ ] Step 1: Add page-level text extraction, coordinate capture, and scanned-page heuristics using PDFBox.
- [ ] Step 2: Add OCR fallback via Tess4J for low-text or garbled pages.
- [ ] Step 3: Add lightweight layout classification for `TITLE`, `PARAGRAPH`, `HEADER`, `FOOTER`, `PAGE_NUMBER`, `TABLE`, and `FIGURE`.
- [ ] Step 4: Add table recovery using Tabula first and OCR/heuristic fallback second.
- [ ] Step 5: Wire `DocumentParser` to use `StructuredPdfParser` for `.pdf`.
- [ ] Step 6: Run `mvn -pl core -am -DskipTests compile`.
- [ ] Step 7: Commit:

```bash
git add core/src/main/java/com/hxh/apboa/core/rag/DocumentParser.java core/src/main/java/com/hxh/apboa/core/rag/parser/impl/StructuredPdfParser.java
git commit -m "feat: add structured pdf parsing with local ocr"
```

## Task 5: Integrate Structured Parsing Into LocalRagService

**Files:**
- Modify: `D:\WorkSpace\apboa\core\src\main\java\com\hxh\apboa\core\rag\service\LocalRagService.java`

- [ ] Step 1: Replace the direct text parsing path with structured parsing in `processDocument`.
- [ ] Step 2: Use `StructureAwareChunker` output as the source of embedding chunks.
- [ ] Step 3: Persist `blockType`, `page`, `bbox`, `sectionPath`, `sheetName`, `slideNumber`, `parseMethod`, and `parentId` into chunk metadata.
- [ ] Step 4: Keep old behavior as fallback for unsupported files or parsing failures.
- [ ] Step 5: Run `mvn -pl core -am -DskipTests compile`.
- [ ] Step 6: Commit:

```bash
git add core/src/main/java/com/hxh/apboa/core/rag/service/LocalRagService.java
git commit -m "feat: integrate structured rag ingestion pipeline"
```

## Task 6: Expose New Processing Options Through API

**Files:**
- Modify: `D:\WorkSpace\apboa\biz\rag\src\main\java\com\hxh\apboa\rag\controller\RagDocumentController.java`

- [ ] Step 1: Extend upload and reprocess endpoints with `parseMethod`, `layoutMode`, `enableParentChild`, `parentChunkSize`, `childChunkSize`, `tableStrategy`, `removeHeaderFooter`, and `ocrLanguage`.
- [ ] Step 2: Merge new options into `KnowledgeBaseConfig.retrievalConfig` without breaking existing fields.
- [ ] Step 3: Preserve compatibility with the in-flight local modifications already present in this file.
- [ ] Step 4: Run `mvn -pl biz/rag -am -DskipTests compile`.
- [ ] Step 5: Commit:

```bash
git add biz/rag/src/main/java/com/hxh/apboa/rag/controller/RagDocumentController.java
git commit -m "feat: expose structured rag parsing options"
```

## Task 7: Run Verification And Clean Up

**Files:**
- Modify only as needed based on test failures

- [ ] Step 1: Run targeted core tests:

```bash
mvn -pl core -am test -Dtest=RecursiveTextChunkerTest,StructureAwareChunkerTest
```

- [ ] Step 2: Run broader compilation:

```bash
mvn -pl biz/rag,core -am -DskipTests compile
```

- [ ] Step 3: Inspect `git diff --stat` and ensure unrelated user changes were not reverted.
- [ ] Step 4: Commit final fixes:

```bash
git add common/pom.xml core biz/rag docs/superpowers/plans/2026-05-23-rag-structured-parsing-and-chunking-implementation.md
git commit -m "feat: complete structured rag parsing and chunking upgrade"
```

## Self-Review

Spec coverage check:

1. Structured parsing model is covered in Task 1.
2. Non-PDF structured parsing is covered in Task 2.
3. Recursive and structure-aware chunking is covered in Task 3.
4. PDF OCR and layout preservation is covered in Task 4.
5. Local ingestion integration and metadata persistence is covered in Task 5.
6. API and config expansion is covered in Task 6.
7. Verification is covered in Task 7.

Placeholder scan:

1. No `TODO`, `TBD`, or “similar to previous task” text remains.
2. Each task references exact files and concrete commands.

Type consistency:

1. `ParsedDocument`, `ParsedPage`, `ParsedBlock`, `StructureAwareChunker`, and `RecursiveTextChunker` names are used consistently.
2. `parseMethod`, `layoutMode`, `enableParentChild`, `parentChunkSize`, `childChunkSize`, `tableStrategy`, `removeHeaderFooter`, and `ocrLanguage` are referenced consistently across parser and controller tasks.
