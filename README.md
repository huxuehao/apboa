<p align="center">
  <h1 align="center">Apboa</h1>
  <p align="center">
    <strong>企业级智能体平台 · Agent 原生架构</strong>
  </p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-blue.svg" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.9-brightgreen.svg" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Vue-3.5-4FC08D.svg" alt="Vue 3.5" />
  <img src="https://img.shields.io/badge/AgentScope-1.0.9-orange.svg" alt="AgentScope" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="MIT License" />
</p>

<p align="center">
  基于 AgentScope 构建的可视化智能体平台<br/>
  敏感词 · 多模型接入 · 多模态 · Tool · Skill · MCP · RAG · Human-in-the-Loop · Agent-as-Tool · AGUI<br/>
  多节点部署 · 智能体缓存 · API Key 管理
</p>

---

> **Note:** 因 issue 被恶意注入大量广告，issue 暂时关闭。有问题欢迎扫描底部二维码，进群讨论。

---

## 目录

- [为什么选择 Apboa](#-为什么选择-apboa)
- [核心能力](#-核心能力)
- [演示视频](#-演示视频)
- [技术栈](#-技术栈)
- [项目结构](#-项目结构)
- [快速启动](#-快速启动)
- [适用场景](#-适用场景)
- [Roadmap](#-roadmap)
- [参与贡献](#-参与贡献)
- [License](#-license)

---

# 为什么选择 Apboa？

在大模型应用进入工程化阶段之后，仅仅调用 API 已经远远不够。你需要的是：

| 需求 | Apboa 的回答 |
|------|-------------|
| 可管理的智能体 | 可视化创建、配置、监控全生命周期 |
| 可扩展的能力模块 | Tool / Skill / MCP / RAG 即插即用 |
| 可控的运行与审核机制 | Human-in-the-Loop + Hook 灵活控制 |
| 可落地的企业级架构 | 多节点部署、智能体缓存、API Key 管理 |

**Apboa 正是为此而生。** 基于 AgentScope 构建，提供一套完整的 Agent 工程化解决方案，让你快速搭建企业级智能体系统，而不是拼凑工具链。

---

# 核心能力

## Agent 原生架构

* 可视化创建与配置智能体
* 模块化能力组合（提示词 / 工具 / MCP / 知识库 / 审核策略等核心能力）
* 支持 Agent 作为 Tool 被其他 Agent 调用
* 支持复杂协作与任务分解

## 多模型统一接入

* 支持 OpenAI / DashScope / Anthropic / Ollama 等主流模型
* 统一抽象接口，模型自由切换
* API Key 轮询与容错机制
* 易于扩展新的模型供应商

## API Key 管理

* 支持为模型配置多个 API Key，自动轮询调度
* 内置容错与失败重试机制，单 Key 异常自动切换
* API Key 用量统计与状态监控
* 支持按模型维度独立管理密钥，灵活配置

## 智能体缓存

* 内置 Agent 运行时缓存机制，提升重复调用响应速度
* 基于 Redis 实现分布式缓存，支持多节点共享
* 支持缓存策略配置（过期时间、淘汰策略等）
* 有效降低模型调用成本，减少不必要的重复推理

## 多节点部署

* 支持水平扩展，多实例集群部署
* 基于 Redis 实现节点间状态同步与协调
* 会话与任务自动路由，无状态服务设计
* 支持负载均衡，提升系统整体吞吐量与高可用性

## 多模态支持

* 支持 3 种存储方案（S3 | FTP | 本地存储）
* 支持 3 种文件类型（图片 | 音频 | 视频）

## 定时任务

* 支持为智能体配置定时任务，自动化周期性执行

## Hook 定义

* 支持硬编码 Hook，在代码中预定义生命周期钩子
* 支持在线编写 Hook，通过平台界面动态配置与热更新
* 灵活扩展 Agent 运行流程与行为控制

## 技能包

* 支持手动创建
* 支持装载本地技能包
* 支持导入技能压缩包
* 支持下载 Git 技能包

## Tool 定义

* 支持硬编码 Tool，在代码中预定义工具能力
* 支持在线编写 Tool，通过平台界面动态创建与编排
* 工具可被 Agent 灵活调用，支持复杂业务逻辑

## MCP 接入

* 支持 MCP（Model Context Protocol）协议接入
* 支持 HTTP、SSE、STDIO 多种传输方式
* 与 MCP 生态工具无缝集成，扩展智能体能力边界

## 原生 RAG 能力

* 内置知识增强生成流程
* 支持百炼 / Dify / RagFlow 等知识源
* 向量语义检索提升回答准确率
* 可插拔式知识库结构

## 企业级运行控制

* Human-in-the-Loop 审核机制
* WebSocket 实时通信
* 流式对话与工具调用

---

# 演示视频

> [平台整体介绍](https://www.bilibili.com/video/BV14dN3zGEUi/)

| 序号 | 主题 | 链接 |
|------|------|------|
| 1 | 智能体的配置介绍 | [观看](https://www.bilibili.com/video/BV1seQ2BTEnN/) |
| 2 | 智能体操作菜单的介绍 | [观看](https://www.bilibili.com/video/BV1KaQ2BzEpF/) |
| 3 | 智能体对话演示 | [观看](https://www.bilibili.com/video/BV11aQ2BBEx7/) |
| 4 | 智能体对话操作按钮说明 | [观看](https://www.bilibili.com/video/BV1xaQ2BBEWm/) |
| 5 | 在对话过程中使用多模态的前提条件 | [观看](https://www.bilibili.com/video/BV1xaQ2BBEkh/) |
| 6 | 在线工具的注意事项 | [观看](https://www.bilibili.com/video/BV12aQ2BBETo/) |
| 7 | 技能包的说明 | [观看](https://www.bilibili.com/video/BV1xaQ2BzEeF/) |
| 8 | 系统设置介绍 | [观看](https://www.bilibili.com/video/BV1xaQ2BBETc/) |

---

# 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.4.9 |
| AI 框架 | AgentScope 1.0.8 |
| ORM | MyBatis-Plus |
| 数据库 | MySQL |
| 缓存/集群 | Redis |
| 前端框架 | Vue 3.5 + TypeScript |
| UI 组件 | Ant Design Vue |
| 构建工具 | Vite |
| 状态管理 | Pinia |

---

# 项目结构

```
apboa
├── common/                  # 通用基础层：实体、DTO、VO、枚举、工具类、异常、常量等
├── cluster/                 # 集群通信：基于 Redis 发布订阅实现多节点状态同步
├── websocket/               # WebSocket 实时通信模块
├── core/                    # 核心整合层：串联 Agent 全生命周期（模型/提示词/工具/MCP/知识库/Hook/技能等）
├── job/                     # 定时任务：基于 Quartz 的智能体周期调度
├── console/                 # 应用入口：会话配置、启动引导
├── biz/                     # 业务功能层
│   ├── model/               #   模型管理
│   ├── prompt/              #   提示词管理
│   ├── tool/                #   工具管理（硬编码 + 在线编写）
│   ├── mcp/                 #   MCP 协议接入（HTTP / SSE / STDIO）
│   ├── skill/               #   技能包管理与脚本执行
│   ├── knowledge/           #   知识库与 RAG 管理
│   ├── hook/                #   生命周期钩子
│   ├── sensitive/           #   敏感词过滤
│   ├── agent/               #   智能体核心（整合上述所有 biz 子模块）
│   ├── account/             #   账户与权限管理
│   ├── resource/            #   资源与文件存储（S3 / FTP / 本地）
│   ├── params/              #   系统参数配置
│   ├── a2a/                 #   Agent-to-Agent 通信
│   ├── studio/              #   Studio 集成
│   └── sk/                  #   技能框架初始化
├── starter/                 # Spring Boot Starter
│   ├── apboa-spring-boot-autoconfigure/   # 自动配置
│   └── apboa-spring-boot-starter/         # Starter 入口
├── ui/                      # 前端：Vue 3.5 + TypeScript + Ant Design Vue
└── docs/                    # 数据库脚本（schema + 增量 SQL）
```

### 模块依赖关系

```
starter -> console -> core -> agent -> [model, prompt, tool, mcp, skill, knowledge, hook, sensitive, params, a2a, studio]
                        |        |
                        |        └-> resource -> params
                        |
                        ├-> account -> params, websocket
                        ├-> job (Quartz)
                        └-> sk

common ← 被所有模块依赖（实体、工具类、缓存键等）
cluster ← 被 common / websocket 依赖（Redis 发布订阅）
```

---

# 快速启动

### 1. 克隆项目

```bash
git clone https://gitee.com/studioustiger/apboa.git
cd apboa
```

### 2. 初始化数据库

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE apboa CHARACTER SET utf8mb4;"

# 执行基础表结构
mysql -u root -p apboa < docs/schema.sql

# 按时间顺序执行增量 SQL（必须全部执行）
mysql -u root -p apboa < docs/2026-02-23-01.sql
mysql -u root -p apboa < docs/2026-02-27-01.sql
mysql -u root -p apboa < docs/2026-03-01-01.sql
mysql -u root -p apboa < docs/2026-03-02-01.sql
mysql -u root -p apboa < docs/2026-03-04-01.sql
mysql -u root -p apboa < docs/2026-03-05-01.sql
mysql -u root -p apboa < docs/2026-03-10-01.sql
mysql -u root -p apboa < docs/2026-03-20-01.sql
mysql -u root -p apboa < docs/2026-03-31-01.sql
mysql -u root -p apboa < docs/2026-04-01-01.sql
mysql -u root -p apboa < docs/2026-04-02-01.sql
mysql -u root -p apboa < docs/2026-04-04-01.sql
...
```

> **注意：** `schema.sql` 为基础表结构，必须首先执行。其余 SQL 文件为增量更新脚本，请严格按照文件名中的日期顺序依次执行，不可跳过。

### 3. 启动后端

```bash
cd console
mvn clean install
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd ui
pnpm install
pnpm run dev
```

### 5. 访问平台

```
http://localhost:3000
```

默认账号：`admin` / `Admin@123.com`

---

# 适用场景

* 企业智能客服
* 内部知识问答系统
* AI 工作流自动化
* 多 Agent 协作系统
* AI 工具平台

---

# Roadmap

- [ ] 可视化编排实现多智能体协同

---

# 参与贡献

欢迎 Issue、PR、讨论与建议。

如果这个项目对你有帮助，请点一个 Star 支持一下！

---

# License

[MIT License](LICENSE)

---

有问题，欢迎进群讨论：

![微信群二维码](image/image-wechat-group.png)

---

<p align="center">
  <strong>让智能体真正具备工程能力</strong><br/>
  Build production-ready AI agents, not demos.
</p>
