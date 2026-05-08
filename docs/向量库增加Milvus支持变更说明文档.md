# Milvus 向量库支持变更说明

## 概述

为 RAG 模块增加 Milvus 向量数据库支持，重构向量存储层为可插拔架构，保留原有 PgVector 方案的同时支持通过配置切换向量存储后端。

## 架构变更

### 向量存储层抽象

新增 `VectorStore` 接口（`core/rag/VectorStore.java`），定义向量数据库的核心操作：

| 方法 | 说明 |
|------|------|
| `isAvailable()` | 判断向量存储是否可用 |
| `storeEmbedding(...)` | 存储单条向量 |
| `storeEmbeddings(List<EmbeddingRecord>)` | 批量存储向量 |
| `search(...)` | 向量相似度检索 |
| `deleteByDocumentId(...)` | 按文档ID删除 |
| `deleteByKnowledgeBaseConfigId(...)` | 按知识库配置ID删除 |
| `deleteByChunkId(...)` | 按分块ID删除 |

### 新增数据类型

- **`EmbeddingRecord`** — 向量记录（record 类型），包含 id、chunkId、documentId、knowledgeBaseConfigId、embedding
- **`RetrievalResult`** — 检索结果（record 类型），包含 chunkId、documentId、score

## 新增文件

### 1. `core/rag/MilvusConfig.java`
- Milvus 客户端配置类，`@ConditionalOnProperty(name = "rag.store", havingValue = "milvus")`
- 启动时自动创建 Milvus 数据库（对标 PgVectorDataSourceConfig 的 ensureDatabaseExists）
- 配置项：`rag.milvus.host`、`rag.milvus.port`、`rag.milvus.username`、`rag.milvus.password`、`rag.milvus.token`、`rag.milvus.database`
- 认证支持用户名/密码和 token 两种方式

### 2. `core/rag/MilvusVectorStore.java`
- Milvus 向量存储实现，`@ConditionalOnProperty(name = "rag.store", havingValue = "milvus")`
- 按维度分 Collection：`rag_embedding_{dim}`，支持 8 种向量维度（64/128/256/512/768/1024/2048/2560）
- 使用 HNSW 索引 + COSINE 相似度度量
- 自动初始化 Schema（创建 Collection、索引、加载到内存）
- 支持单条/批量插入、条件检索（按知识库配置ID过滤 + 分数阈值）、按维度级联删除

### 3. `core/rag/NoOpVectorStore.java`
- 空实现回退，当未配置任何向量数据库时作为默认 bean
- 所有操作只打 warn 日志，不抛异常，保证业务优雅降级
- 写入操作静默跳过，检索操作返回空列表

### 4. `core/rag/VectorStoreConfig.java`
- `@Configuration` 类，通过 `@Bean` + `@ConditionalOnMissingBean(VectorStore.class)` 提供 NoOpVectorStore 回退
- `@Configuration` 在组件扫描之后处理，确保条件判断准确

### 5. `core/rag/VectorStore.java`
- 向量存储接口

### 6. `core/rag/EmbeddingRecord.java`
- 向量记录数据类

### 7. `core/rag/RetrievalResult.java`
- 检索结果数据类

## 修改文件

### 1. `core/rag/PgVectorStore.java`
- 实现 `VectorStore` 接口
- 添加 `@ConditionalOnProperty(name = "rag.store", havingValue = "pgvector")`
- 构造器注入改为 `@Qualifier("pgVectorDataSource")` + `required = false`
- 批量存储改为接收 `List<EmbeddingRecord>`，检索返回 `List<RetrievalResult>`

### 2. `core/rag/service/LocalRagService.java`
- 依赖注入从 `PgVectorStore` 改为 `VectorStore` 接口
- 使用 `EmbeddingRecord` 和 `RetrievalResult` 替代原有内联数据结构
- 使用 `vectorStore.storeEmbeddings(records)` 批量存储

### 3. `common/config/db/PgVectorDataSourceConfig.java`
- 添加 `@ConditionalOnProperty(name = "rag.store", havingValue = "pgvector")`
- pgvector 数据源仅在配置为 pgvector 模式时生效

### 4. `core/pom.xml`
- 新增依赖：`io.milvus:milvus-sdk-java:2.6.8`

### 5. `console/src/main/resources/application-dev.yml`
- 新增 `rag.store` 配置项（可选值：`pgvector` | `milvus`）
- 新增 `rag.milvus` 配置段（host、port、username、password、token、database）
- 移除已废弃的 `rag.pgvector.enabled` 参数

### 6. `README.md`
- 更新快速启动章节，说明支持 PostgreSQL/pgvector 和 Milvus 两种向量数据库

### 7. `.gitignore`
- 新增 `.claude` 目录

## 配置方式

```yaml
rag:
  store: milvus  # 可选 pgvector | milvus，不配置则不启用向量库
  pgvector:
    url: jdbc:postgresql://127.0.0.1:5432/apboa_vector
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  milvus:
    host: 127.0.0.1
    port: 19530
    username:          # 自建部署留空
    password:          # 自建部署留空
    token:             # SaaS 认证 token，与用户名/密码二选一
    database: default
```

### 不启用向量库

将 `rag.store` 注释或删除即可，系统自动使用 NoOpVectorStore 空实现。无需配置 pgvector 或 milvus。

```yaml
rag:
  # store: milvus  # 注释掉，不启用向量库
```

此时所有 RAG 相关写入/检索操作均为静默降级，不会抛出异常。

## 设计要点

1. **维度分表/分 Collection**：不同 embedding 模型输出的向量维度不同，按维度拆分存储单元，避免维度不一致问题
2. **条件装配**：通过 `@ConditionalOnProperty` 控制具体实现，`@Configuration` + `@Bean` + `@ConditionalOnMissingBean` 提供可靠的回退机制
3. **优雅降级**：未配置向量存储时使用 NoOpVectorStore，所有操作打 warn 日志后静默返回，不影响系统正常运行
4. **HNSW 索引**：Milvus 使用 HNSW 索引（M=16, efConstruction=200），兼顾精度与构建速度
5. **COSINE 相似度**：两种后端统一使用余弦相似度，检索行为一致
