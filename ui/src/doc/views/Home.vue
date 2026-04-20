<!-- eslint-disable vue/multi-word-component-names -->
/**
 * Doc 首页视图 - 平台概述
 *
 * @author huxuehao
 */
<script setup lang="ts">
import {
  RobotOutlined,
  ToolOutlined,
  DatabaseOutlined,
  ApiOutlined,
  SafetyCertificateOutlined,
  FileTextOutlined,
  CloudServerOutlined,
  LoginOutlined,
  AppstoreOutlined,
  BookOutlined,
} from '@ant-design/icons-vue';
import { RouterLink } from 'vue-router';

/**
 * 平台核心功能列表
 */
const features = [
  { icon: SafetyCertificateOutlined, title: '敏感词管理', desc: '配置敏感词过滤规则，支持拦截、警告两种处理方式，保障内容安全合规', color: '#E55B5B' },
  { icon: FileTextOutlined, title: '提示词模板', desc: '创建和管理系统提示词模板，统一管理智能体的系统提示词', color: '#F0843E' },
  { icon: CloudServerOutlined, title: '模型供应商', desc: '管理 AI 模型供应商和模型配置，支持 DashScope、OpenAI、Anthropic、Ollama 等多家供应商', color: '#00B81B' },
  { icon: LoginOutlined, title: '钩子', desc: '配置智能体执行前后的拦截逻辑，实现输入输出预处理和后处理', color: '#DB2781' },
  { icon: RobotOutlined, title: '智能体管理', desc: '创建、配置和管理智能体，支持自定义和A2A两种类型，配置模型、工具、知识库等多维度能力', color: '#5C6BC0' },
  { icon: ToolOutlined, title: '工具管理', desc: '注册和配置内置/自定义工具，扩展智能体的执行能力边界', color: '#399DF2' },
  { icon: ApiOutlined, title: 'MCP 服务', desc: '接入和管理 MCP 协议服务，支持 HTTP/SSE/STDIO 协议，标准化工具调用方式', color: '#66BB6A' },
  { icon: DatabaseOutlined, title: '知识库', desc: '通过 RAG 技术将企业私有文档构建为可检索、可推理的结构化知识体系', color: '#42A5F5' },
  { icon: AppstoreOutlined, title: '技能包', desc: '组合多个工具和脚本形成技能包，实现复杂业务流程自动化', color: '#AB47BC' },
];

/**
 * 文档导航列表
 */
const docNavItems = [
  { path: '/sensitive', icon: SafetyCertificateOutlined, title: '敏感词', desc: '配置敏感词过滤规则，支持拦截、警告两种处理方式。', color: '#E55B5B' },
  { path: '/prompt', icon: FileTextOutlined, title: '提示词模板', desc: '创建和管理系统提示词模板，统一管理智能体的系统提示词。', color: '#F0843E' },
  { path: '/model', icon: CloudServerOutlined, title: '模型供应商', desc: '管理 AI 模型供应商和模型配置，支持多家主流供应商。', color: '#00B81B' },
  { path: '/hook', icon: LoginOutlined, title: '钩子', desc: '配置智能体执行前后的拦截逻辑，实现输入输出预处理和后处理。', color: '#DB2781' },
  { path: '/tool', icon: ToolOutlined, title: '工具', desc: '注册和配置内置/自定义工具，扩展智能体的执行能力。', color: '#399DF2' },
  { path: '/skill', icon: AppstoreOutlined, title: '技能包', desc: '组合多个工具和脚本形成技能包，实现复杂业务流程。', color: '#AB47BC' },
  { path: '/mcp', icon: ApiOutlined, title: 'MCP', desc: '接入和管理 MCP 协议服务，标准化工具调用方式。', color: '#66BB6A' },
  { path: '/knowledge', icon: DatabaseOutlined, title: '知识库', desc: '通过 RAG 技术构建结构化知识体系，为智能体提供专业知识。', color: '#42A5F5' },
  { path: '/agent', icon: RobotOutlined, title: '智能体', desc: '创建、配置和管理智能体，支持自定义和A2A两种类型。', color: '#5C6BC0' },
  { path: '/markdown-extension', icon: BookOutlined, title: 'Markdown 扩展指南', desc: '了解平台支持的 Markdown 扩展语法，包括数学公式、流程图、代码高亮等。', color: '#409eff' },
];
</script>

<template>
  <div class="doc-home">
    <!-- 欢迎区域 -->
    <section class="doc-home-hero">
      <img src="@/assets/images/logo/logo.png" alt="logo" class="doc-home-logo" />
      <h1 class="doc-home-hero-title">Apboa 智能体平台</h1>
      <p class="doc-home-hero-desc">
        Apboa 是一款功能强大的智能体管理平台，帮助企业快速构建、部署和管理 AI 智能体应用。
        本手册将帮助您了解平台的各项功能和使用方法。
      </p>
    </section>

    <!-- 核心功能 -->
    <section class="doc-home-features">
      <h2 class="doc-home-section-title">核心功能</h2>
      <div class="doc-home-feature-grid">
        <div
          v-for="feature in features"
          :key="feature.title"
          class="doc-home-feature-card"
        >
          <component :is="feature.icon" class="doc-home-feature-icon" :style="{ color: feature.color }" />
          <h3 class="doc-home-feature-title">{{ feature.title }}</h3>
          <p class="doc-home-feature-desc">{{ feature.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 快速开始 -->
    <section class="doc-home-quickstart">
      <h2 class="doc-home-section-title">快速开始</h2>
      <div class="doc-home-steps">
        <div class="doc-home-step">
          <span class="doc-home-step-num">1</span>
          <div>
            <h4>登录平台</h4>
            <p>使用管理员分配的账号登录平台，进入管理控制台。</p>
          </div>
        </div>
        <div class="doc-home-step">
          <span class="doc-home-step-num">2</span>
          <div>
            <h4>配置模型</h4>
            <p>在「模型供应商」中添加 AI 模型服务，为智能体提供推理能力。</p>
          </div>
        </div>
        <div class="doc-home-step">
          <span class="doc-home-step-num">3</span>
          <div>
            <h4>创建智能体</h4>
            <p>在「智能体」中创建智能体，配置系统提示词、工具和知识库等。</p>
          </div>
        </div>
        <div class="doc-home-step">
          <span class="doc-home-step-num">4</span>
          <div>
            <h4>开始对话</h4>
            <p>进入智能体对话界面，与智能体进行交互测试。</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 文档导航 -->
    <section class="doc-home-docs">
      <h2 class="doc-home-section-title">使用文档</h2>
      <div class="doc-home-doc-list">
        <RouterLink
          v-for="doc in docNavItems"
          :key="doc.path"
          :to="doc.path"
          class="doc-home-doc-card"
        >
          <component :is="doc.icon" class="doc-home-doc-icon" :style="{ color: doc.color }" />
          <div>
            <h4>{{ doc.title }}</h4>
            <p>{{ doc.desc }}</p>
          </div>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.doc-home {
  max-width: 880px;
  margin: 0 auto;
  padding: 32px 0 64px;
}

/* 欢迎区域 */
.doc-home-hero {
  text-align: center;
  padding: 40px 0 28px;
}
.doc-home-logo {
  width: 90px;
  height: 85px;
  flex-shrink: 0;
}

.doc-home-hero-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 16px;
}

.doc-home-hero-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 12px;
}

.doc-home-hero-desc {
  font-size: 16px;
  color: #666;
  line-height: 1.6;
  max-width: 640px;
  margin: 0 auto;
}

/* 通用章节标题 */
.doc-home-section-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 20px;
  padding-bottom: 8px;
  border-bottom: 2px solid #409eff;
  display: inline-block;
}

/* 核心功能 */
.doc-home-features {
  margin-bottom: 40px;
}

.doc-home-feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.doc-home-feature-card {
  background: #f8fafc;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  padding: 20px;
  transition: all 0.2s ease;

  &:hover {
    border-color: #d0e4ff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.doc-home-feature-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.doc-home-feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 6px;
}

.doc-home-feature-desc {
  font-size: 13px;
  color: #888;
  line-height: 1.5;
  margin: 0;
}

/* 快速开始 */
.doc-home-quickstart {
  margin-bottom: 40px;
}

.doc-home-steps {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-home-step {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  background: #f8fafc;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  padding: 16px 20px;

  h4 {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0 0 4px;
  }

  p {
    font-size: 13px;
    color: #888;
    margin: 0;
  }
}

.doc-home-step-num {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

/* 文档导航 */
.doc-home-doc-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-home-doc-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  background: #f8fafc;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  padding: 16px 20px;
  text-decoration: none;
  transition: all 0.2s ease;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
  }

  h4 {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0 0 4px;
  }

  p {
    font-size: 13px;
    color: #888;
    margin: 0;
  }
}

.doc-home-doc-icon {
  font-size: 22px;
  margin-top: 2px;
  flex-shrink: 0;
}
</style>
