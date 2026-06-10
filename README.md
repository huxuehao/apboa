<p align="center">
  <h1 align="center">Apboa</h1>
  <p align="center">
    <strong>企业级 AI 智能体平台  ·  Agent-Native 架构</strong>
  </p>
</p>

<p align="center">
  <a href="https://gitee.com/studioustiger/apboa/blob/master/LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License" /></a>
  <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.9-brightgreen.svg" alt="Spring Boot 3.4.9" />
  <img src="https://img.shields.io/badge/Vue-3.5-4FC08D.svg" alt="Vue 3.5" />
  <img src="https://img.shields.io/badge/AgentScope-1.0.12-orange.svg" alt="AgentScope 1.0.12" />
</p>
<p align="center">
  可视化智能体构建 · 多模型统一接入 · 全生命周期管理<br/>
  Tool · Skill · MCP · RAG · Hook · A2A · Long-Term Memory · Human-in-the-Loop<br/>
  多节点集群 · API Key · 容器化隔离部署
</p>


---

> **Notice:** 因 Issue 区被恶意注入大量广告，暂时关闭 Issue 功能。**有问题欢迎扫描底部二维码，进群讨论。**

---

## 目录

- [Apboa 是什么？](#apboa-是什么)
- [设计理念](#设计理念)
- [核心能力](#核心能力)
- [预览](#预览)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速开始](#快速开始)
- [部署方案](#部署方案)
- [参与贡献](#参与贡献)
- [License](#license)
- [社区交流](#社区交流)

---

## Apboa 是什么？

**Apboa** 是一套面向企业级场景的 AI 智能体平台。它基于 [AgentScope](https://github.com/modelscope/agentscope) 构建，提供智能体从创建、配置、调试到上线运行的全生命周期管理能力。

当大模型应用进入工程化阶段后，仅调用 API 远远不够——你需要一个平台来管理复杂的智能体协作、能力扩展和生产环境运维。Apboa 正是为此而生。

### 一句话总结

> 用可视化的方式配置智能体，用插件化的方式扩展能力，用企业级的架构保障运行——而不是拼凑工具链。

---

## 设计理念

### 模块化插件体系

Apboa 将智能体的每个能力单元（Model / Tool / Skill / MCP / Knowledge / Hook）抽象为独立模块，通过 **组件化编排** 组合为完整的 Agent 配置。每个模块可独立管理、独立扩展、热插拔替换。

```
                  Agent Core
                      |
    +-----+-----+-----+-----+-----+-----+
    |     |     |     |     |     |     |
  Model Tool Skill  MCP   RAG  Hook A2A
    |
  每层均支持硬编码 + 在线编写双模式
```

### 双模式扩展机制

所有可扩展点均支持两种工作模式：

| 模式 | 适用场景 | 优势 |
|------|----------|------|
| **硬编码注入** | 稳定的平台级能力、高性能要求 | 编译期检查、性能最优 |
| **在线编写/配置** | 业务多变的场景、非研发人员使用 | 热更新、零停机、动态生效 |

Skill 和 Tool 模块甚至支持在线编写，编写即生效。

### 企业级安全防护

从输入到执行构建多层安全防线：

- **脚本安全校验**：所有动态脚本需通过安全规则引擎验证后才可执行
- **容器化隔离**：Agent Runner 在受限 Docker 容器中运行，cap_drop=ALL，非 root 用户
- **工作空间沙箱**：每会话独立文件系统空间，路径安全校验
- **XSS 防护**：内置 DOMPurify 过滤，防止脚本注入
- **Human-in-the-Loop**：关键操作人工审核确认

---

## 核心能力

### 智能体全生命周期管理

- 可视化表单创建与配置智能体
- 支持 Agent-as-Tool 嵌套调用，构建层级化协作体系
- 定时任务自动调度执行（基于 Quartz）
- 运行时缓存机制，降低重复推理成本

### 多模型统一接入

- 内置支持 OpenAI、DashScope、Anthropic、Ollama 等主流模型供应商
- 统一抽象接口，模型自由切换，新供应商易于扩展
- 按模型维度独立管理密钥

### 可扩展能力模块

| 模块 | 能力 | 扩展方式 |
|------|------|----------|
| **Tool** | 定义可被 Agent 调用的工具 | 硬编码 / 在线编写 Groovy |
| **Skill** | 封装可复用能力包 | 手动创建 / 本地装载 / Git 拉取 / Zip 导入 |
| **MCP** | 接入 MCP 生态工具 | HTTP / SSE / STDIO 三传输协议 |
| **RAG** | 知识增强生成 | 本地知识库 / 百炼 / Dify / RagFlow |
| **Hook** | 生命周期钩子控制 | 硬编码 / 在线编写热更新 |
| **Long-Term Memory** | 跨会话持久化记忆 | Mem0 / ReMe（阿里通义千问）/ 百炼记忆库 |

### 长期记忆（Long-Term Memory）

智能体可启用跨会话持久化记忆，在多次对话间保持对用户偏好、历史交互的持续记忆。支持三种记忆服务：

- **Mem0**：开源长期记忆框架，支持 Platform 和 Self-hosted 两种部署方式
- **ReMe**：基于阿里通义千问的记忆服务
- **百炼记忆库**：基于阿里云百炼平台的记忆库服务

记忆类型通过策略模式可插拔扩展，用户标识从登录上下文自动获取，无需额外配置。

### 原生 Markdown 渲染引擎

内置功能完备的 Markdown 渲染引擎，开箱即用，灵活扩展：

- **代码高亮**：基于 highlight.js，覆盖主流编程语言
- **数学公式**：KaTeX 渲染行内与块级公式
- **XSS 防护**：DOMPurify 过滤
- **自定义容器**：`:::tip` / `:::warning` 等语法
- **Mermaid 图表**：流式渲染，自动适配暗色主题
- **容器扩展自动发现**：新增 `.ts` 扩展文件即可自动注册

### 多模态与工作空间

- 支持图片 / 音频 / 视频文件，3 种存储方案（S3 / FTP / 本地）
- 每会话独立工作空间，文件上传下载、预览、删除，Zip 自动解压
- 代码执行环境支持（Python / Node.js）

### 企业级部署运维

- **多节点集群**：基于 Redis 发布订阅实现状态同步与协调
- **负载均衡**：无状态服务设计，会话自动路由
- **容器化部署**：Docker Compose 一键启动，支持离线/私有化环境
- **内置文档系统**：平台内置部署指南、使用手册、扩展开发文档

---

## 预览

### 核心页面

| ![智能体对话](image/image-20260411140636264.png) | ![智能体配置](image/image-20260411140659383.png) |
| :----------------------------------------------: | :----------------------------------------------: |
|                    智能体管理                    |                    智能体配置                    |

| ![模型管理](image/image-20260411140717467.png) | ![知识库管理](image/image-20260411140733513.png) |
| :--------------------------------------------: | :----------------------------------------------: |
|                  智能体自动化                  |                   智能体架构图                   |

| ![技能包](image/image-20260411140754472.png) | ![工具管理](image/image-20260411140827201.png) |
| :------------------------------------------: | :--------------------------------------------: |
|                  智能体接入                  |                    对话历史                    |

| ![MCP 配置](image/image-20260411140847484.png) | ![提示词模板](image/image-20260411140921656.png) |
| :--------------------------------------------: | :----------------------------------------------: |
|                  对话统计分析                  |                     在线Tool                     |

| ![image-20260523204455845](image/image-20260523204455845.png) | ![image-20260523204531479](image/image-20260523204531479.png) |
| :----------------------------------------------------------: | :----------------------------------------------------------: |
|                           技能管理                           |                         技能在线编写                         |

| ![Hook 配置](image/image-20260411141406956.png) | ![敏感词过滤](image/image-20260411141421777.png) |
| :---------------------------------------------: | :----------------------------------------------: |
|                    系统配置                     |                     文件存储                     |

| ![image-20260523204647055](image/image-20260523204647055.png) | ![工作空间](image/image-20260506003900491.png) |
| :----------------------------------------------------------: | :--------------------------------------------: |
|                          知识库管理                          |                   本地知识库                   |

| ![定时任务](image/image-20260502194842488.png) | ![A2A 通信](image/image-20260502195403150.png) |
| :--------------------------------------------: | :--------------------------------------------: |
|          对话页面一（自动适配移动端）          |          对话页面二（自动适配移动端）          |

### Markdown 引擎

| ![代码高亮](image/image-20260412165749836.png) | ![数学公式](image/image-20260412165823512.png) |
| :--------------------------------------------: | :--------------------------------------------: |
|                 KaTeX 数学公式                 |                    表格渲染                    |

| ![Mermaid 图表](image/image-20260412165908454.png) | ![自定义容器](image/image-20260412165929208.png) |
| :------------------------------------------------: | :----------------------------------------------: |
|                      图片预览                      |                    自定义容器                    |

| ![代码块全屏](image/image-20260412170001004.png) | ![表格渲染](image/image-20260412170023565.png) |
| :----------------------------------------------: | :--------------------------------------------: |
|                     Markdown                     |                    Markdown                    |

| ![全屏预览](image/image-20260412170053559.png) | ![图片预览](image/image-20260412170111978.png) |
| :--------------------------------------------: | :--------------------------------------------: |
|                   HTML预览一                   |                   HTML预览二                   |

### 内置使用手册

| ![使用手册](image/image-20260419124239189.png) | ![功能说明](image/image-20260419124319295.png) |
|:--:|:--:|
| 使用手册目录 | 功能详细说明 |

---

## 技术栈

| 层级 | 技术选型                                          | 版本 |
|------|-----------------------------------------------|------|
| **语言运行时** | Java                                          | 21 LTS |
| **后端框架** | Spring Boot                                   | 3.4.9 |
| **AI 框架** | AgentScope                                    | 1.0.12 |
| **ORM** | MyBatis-Plus                                  | 3.5.7 |
| **连接池** | Druid                                         | 1.2.19 |
| **数据库** | MySQL                                         | 8.0 |
| **缓存/集群** | Redis                                         | 7 |
| **向量数据库** | pgvector / Milvus / Qdrant / Elasticsearch / Weaviate | 可插拔 |
| **脚本引擎** | Apache Groovy                                 | 4.0 |
| **前端框架** | Vue 3 + TypeScript                            | 3.5 / 5.9 |
| **UI 组件库** | Ant Design Vue                                | 4.x |
| **构建工具** | Vite                                          | 7.x |
| **状态管理** | Pinia                                         | 3.x |
| **代码编辑器** | CodeMirror 6                                  | 6.x |
| **Markdown 渲染** | marked + highlight.js + KaTeX + Mermaid       | - |
| **图表** | ECharts 6                                     | - |
| **容器化** | Docker + Docker Compose                       | - |

---

## 项目结构

```
apboa
├── common/                    # 通用基础层：实体、DTO、工具类、异常、缓存键
├── cluster/                   # 集群通信：Redis 发布订阅实现多节点状态同步
├── websocket/                 # WebSocket 实时通信模块
├── core/                      # 核心整合层：串联 Agent 全生命周期编排
├── job/                       # 定时任务：基于 Quartz 的智能体周期调度
├── console/                   # 应用入口：Spring Boot 启动引导
├── biz/                       # 业务功能层
│   ├── model/                 #   模型管理
│   ├── prompt/                #   提示词模板管理
│   ├── tool/                  #   工具管理（硬编码 + 在线编写）
│   ├── mcp/                   #   MCP 协议接入（HTTP / SSE / STDIO）
│   ├── skill/                 #   技能包管理与脚本执行
│   ├── knowledge/             #   知识库与 RAG
│   ├── hook/                  #   生命周期钩子
│   ├── sensitive/             #   敏感词过滤
│   ├── agent/                 #   智能体核心（整合上述各 biz 子模块）
│   ├── account/               #   账户与权限管理
│   ├── resource/              #   资源与文件存储（S3 / FTP / 本地）
│   ├── params/                #   系统参数配置
│   ├── a2a/                   #   Agent-to-Agent 通信
│   ├── studio/                #   Studio 集成调试
│   ├── sk/                    #   技能框架初始化
│   └── rag/                   #   RAG 检索增强
├── security/                  # 安全模块
│   └── script-security/       #   脚本安全校验引擎
├── ui/                        # 前端：Vue 3 + TypeScript + Ant Design Vue
├── docker/                    # Docker 部署配置（Compose + Dockerfile）
└── docs/                      # 数据库脚本（初始化 + 增量迁移）
```

### 模块依赖关系

```
console ──→ core ──→ agent ──→ [model, prompt, tool, mcp, skill, knowledge, hook, sensitive, params, a2a, studio]
                    │              │
                    │              └──→ resource ──→ params
                    │
                    ├──→ account ──→ params, websocket
                    ├──→ job (Quartz)
                    └──→ sk, rag

common    ←── 被所有模块依赖（实体层、工具类、缓存键）
cluster   ←── 被 common / websocket 依赖（Redis 发布订阅）
security  ←── 被 skill / core 依赖（脚本安全校验）
```

---

## 快速开始

### 环境要求

| 组件 | 最低版本 |
|------|----------|
| JDK | 21 |
| Maven | 3.6+ |
| MySQL | 8.0 |
| Redis | 7 |
| Node.js | 20.19+ 或 22.12+ |
| pnpm | 9.x |

### 1. 克隆项目

```bash
git clone https://gitee.com/studioustiger/apboa.git
cd apboa
```

### 2. 初始化数据库

```bash
mysql -u root -p < docs/once_db_init/db_init.sql
```

### 3. 启动后端

```bash
cd console
mvn clean install -DskipTests
mvn spring-boot:run
```

后端默认运行在 `http://localhost:3060`。

### 4. 启动前端

```bash
cd ui
pnpm install
pnpm run dev
```

前端默认运行在 `http://localhost:3000`，自动代理 API 请求到后端。

### 5. 访问平台

打开浏览器访问 `http://localhost:3000`，使用默认账号登录：

- 账号：`admin`
- 密码：`Admin@123.com`

---

## 部署方案

平台内置完整的部署文档，启动后访问 [http://localhost:3000/doc.html#/build](http://localhost:3000/doc.html#/build) 查看详细说明。支持以下四种部署方案：

| 方案 | 适用场景 | 说明 |
|------|----------|------|
| 前后端分离部署 | 开发 / 测试环境 | 前端 Vite 开发服务器 + 后端 Spring Boot 独立运行 |
| 一体化 JAR 部署 | 单机轻量部署 | 前端打包为静态资源嵌入 JAR，一个文件启动 |
| Docker Compose 部署 | 生产环境推荐 | 一键启动 MySQL + Redis + pgvector + 前后端全套服务 |
| Dockerfile 自定义 | 私有化定制部署 | 支持离线镜像仓库、资源限制、非 root 用户等安全配置 |

### Docker Compose 快速部署

```bash
cd docker
# 编辑 .env 文件配置环境变量
docker compose up -d
```

启动后访问 `http://localhost`（默认端口 80）。

---

## 参与贡献

欢迎参与 Apboa 的开发与改进！无论是提交 Issue、PR 还是参与讨论，你的贡献都将帮助项目变得更好。

### Issue 提交流程

> 当前 Issue 功能已暂时关闭，有问题请扫描底部二维码进群讨论。

### Pull Request 提交指南

**提交前准备：**

1. Fork 本仓库并克隆到本地
2. 从 `master` 分支创建新分支，命名建议：`feature/xxx`、`fix/xxx`、`docs/xxx`
3. 确保代码与最新 `master` 同步

**代码规范：**

- 遵循项目已有的代码风格与注释规范
- 提交前确保构建通过：后端 `mvn clean install -DskipTests`，前端 `pnpm run build`

**Commit 格式（约定式提交）：**

```
<type>(<scope>): <subject>
```

| type | 说明 |
|------|------|
| `feat` | 新功能 |
| `fix` | 缺陷修复 |
| `docs` | 文档更新 |
| `style` | 代码格式调整 |
| `refactor` | 代码重构 |
| `test` | 测试相关 |
| `chore` | 构建/工具链/依赖变更 |

**PR 要求：**

- 一个 PR 只解决一个问题或实现一个功能
- 标题简洁明了，描述中说明变更内容、关联 Issue、测试方式和影响范围
- 确保 CI 流水线通过

---

## License

本项目基于 [MIT License](LICENSE) 开源。

---

## 社区交流

| Apboa 交流群 1（已满） | Apboa 交流群 2（已满） | Apboa 交流群 3 |
|:--:|:--:|:--:|
| ![交流群1](image/image-20260508235150666.png) | ![交流群2](image/image-20260521093906069.png) | ![image-20260610234639342](image/image-20260610234639342.png) |

---

<p align="center">
  <strong>让智能体真正具备工程能力</strong><br/>
  Build production-ready AI agents, not demos.
</p>
