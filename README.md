<p align="center">
  <h1 align="center">🚀 Apboa</h1>
  <p align="center">
    企业级智能体平台 · Agent 原生架构
  </p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-blue.svg" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4.9-brightgreen.svg" />
  <img src="https://img.shields.io/badge/Vue-3.5-4FC08D.svg" />
  <img src="https://img.shields.io/badge/AgentScope-1.0.9-orange.svg" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg" />
</p>
<p align="center">
  基于 AgentScope 构建的可视化智能体平台  
  支持敏感词 · 多模型接入 · Tool · Skill · MCP · RAG · Human-in-the-Loop · Agent-as-Tool · AGUI
</p>


---

# ✨ 为什么选择 Apboa？

在大模型应用进入工程化阶段之后，仅仅调用 API 已经远远不够。

你需要的是：

* ✅ 可管理的智能体
* ✅ 可扩展的能力模块
* ✅ 可控的运行与审核机制
* ✅ 可落地的企业级架构

**Apboa 正是为此而生。**

Apboa 基于 AgentScope 构建，提供一套完整的 Agent 工程化解决方案，让你快速搭建企业级智能体系统，而不是拼凑工具链。

---

# 🧠 核心能力

## 🤖 Agent 原生架构

* 可视化创建与配置智能体
* 模块化能力组合（提示词 / 工具 / 知识库 / 审核策略）
* 支持 Agent 作为 Tool 被其他 Agent 调用
* 支持复杂协作与任务分解

---

## 🔌 多模型统一接入

* 支持 OpenAI / DashScope / Anthropic / Ollama 等主流模型
* 统一抽象接口，模型自由切换
* API Key 轮询与容错机制
* 易于扩展新的模型供应商

---

## 📚 原生 RAG 能力

* 内置知识增强生成流程
* 支持百炼 / Dify / RagFlow 等知识源
* 向量语义检索提升回答准确率
* 可插拔式知识库结构

---

## 🔄 企业级运行控制

* Human-in-the-Loop 审核机制
* MCP 协议支持（HTTP / SSE / STDIO）
* WebSocket 实时通信
* 流式对话与工具调用

---

# 🖥 系统界面预览

### 智能体配置

![image-20260218001931537](image/image-20260218001931537.png)

![image-20260218002035305](image/image-20260218002035305.png)

![image-20260218002115445](image/image-20260218002115445.png)



### 对话运行界面

![image-20260218002559192](image/image-20260218002559192.png)

![image-20260218002457509](image/image-20260218002457509.png)

![image-20260218002519769](image/image-20260218002519769.png)

### 其他

![image-20260218002705029](image/image-20260218002705029.png)

![image-20260218002817132](image/image-20260218002817132.png)

![image-20260218002748412](image/image-20260218002748412.png)


---

# 🏗 技术栈

## 后端

* Java 21
* Spring Boot 3.4.9
* AgentScope 1.0.8
* MyBatis-Plus
* MySQL
* Redis

## 前端

* Vue 3.5
* TypeScript
* Ant Design Vue
* Vite
* Pinia

---

# 🚀 5 分钟快速启动

```bash
# 1. 克隆
git clone https://gitee.com/studioustiger/apboa.git
cd apboa

# 2. 初始化数据库
mysql -u root -p -e "CREATE DATABASE apboa CHARACTER SET utf8mb4;"
mysql -u root -p apboa < docs/schema.sql

# 3. 启动后端
cd console
mvn clean install
mvn spring-boot:run

# 4. 启动前端
cd ui
pnpm install
pnpm run dev
```

访问：

```
http://localhost:3000
```

默认账号：

```
admin / Admin@123.com
```

---

# 🎯 适用场景

* 企业智能客服
* 内部知识问答系统
* AI 工作流自动化
* 多 Agent 协作系统
* AI 工具平台

---

# 🛣 Roadmap

* ☐ 可视化编排实现多智能体协同

---

# 🤝 参与贡献

欢迎 Issue、PR、讨论与建议。

如果这个项目对你有帮助，请点一个 ⭐ Star 支持一下！

---

# 📜 License

MIT License

---

<p align="center">
  <strong>让智能体真正具备工程能力</strong><br/>
  Build production-ready AI agents, not demos.
</p>
