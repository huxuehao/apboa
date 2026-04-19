/**
 * Doc 子应用根组件
 *
 * @author huxuehao
 * @component
 */
<script setup lang="ts">
import { RouterView, RouterLink, useRoute } from 'vue-router';
import zhCN from 'ant-design-vue/es/locale/zh_CN';
import { BookOutlined, ReadOutlined, HomeOutlined, StopOutlined, FileTextOutlined, CloudServerOutlined, LinkOutlined, ToolOutlined, AppstoreOutlined, ApiOutlined, DatabaseOutlined, RobotOutlined, QuestionCircleOutlined, BuildOutlined } from '@ant-design/icons-vue';

const route = useRoute();

/**
 * 文档导航菜单配置
 */
const docMenus = [
  { path: '/', label: '概述', icon: HomeOutlined, color: '#409eff' },
  { path: '/sensitive', label: '敏感词', icon: StopOutlined, color: '#E55B5B' },
  { path: '/prompt', label: '提示词模板', icon: FileTextOutlined, color: '#F0843E' },
  { path: '/model', label: '模型供应商', icon: CloudServerOutlined, color: '#00B81B' },
  { path: '/hook', label: '钩子', icon: LinkOutlined, color: '#DB2781' },
  { path: '/tool', label: '工具', icon: ToolOutlined, color: '#399DF2' },
  { path: '/skill', label: '技能包', icon: AppstoreOutlined, color: '#AB47BC' },
  { path: '/mcp', label: 'MCP', icon: ApiOutlined, color: '#66BB6A' },
  { path: '/knowledge', label: '知识库', icon: DatabaseOutlined, color: '#42A5F5' },
  { path: '/agent', label: '智能体', icon: RobotOutlined, color: '#5C6BC0' },
  { path: '/markdown-extension', label: 'Markdown 扩展', icon: ReadOutlined, color: '#409eff' },
  { path: '/build', label: '打包构建', icon: BuildOutlined, color: '#78909C' },
  { path: '/qa', label: 'Q&A', icon: QuestionCircleOutlined, color: '#FF9800' },
];
</script>

<template>
  <AConfigProvider
    :locale="zhCN"
    :theme="{
      token: {
        fontFamily: 'AlimamaFangYuan, sans-serif',
      },
    }"
  >
    <div class="doc-layout">
      <!-- 侧边栏 -->
      <aside class="doc-sidebar">
        <div class="doc-sidebar-header">
          <BookOutlined class="doc-sidebar-header-icon" />
          <span class="doc-sidebar-header-title">Apboa 使用手册</span>
        </div>
        <nav class="doc-nav">
          <RouterLink
            v-for="menu in docMenus"
            :key="menu.path"
            :to="menu.path"
            class="doc-nav-item"
            :class="{ active: route.path === menu.path }"
            :style="route.path === menu.path ? { color: menu.color, backgroundColor: menu.color + '14' } : {}"
          >
            <component :is="menu.icon" class="doc-nav-icon" :style="{ color: route.path === menu.path ? menu.color : '' }" />
            <span>{{ menu.label }}</span>
          </RouterLink>
        </nav>
      </aside>

      <!-- 内容区 -->
      <main class="doc-content">
        <RouterView />
      </main>
    </div>
  </AConfigProvider>
</template>

<style scoped lang="scss">
$doc-sidebar-width: 240px;

.doc-layout {
  height: 100vh;
  display: flex;
}

/* 左侧导航栏 - flex 子项，不滚动 */
.doc-sidebar {
  width: $doc-sidebar-width;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  flex-shrink: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

/* 侧边栏标题区域 */
.doc-sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 16px;
  border-bottom: 1px solid #f0f0f0;
}

.doc-sidebar-header-icon {
  font-size: 22px;
  color: #409eff;
}

.doc-sidebar-header-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  line-height: 1.4;
}

.doc-nav {
  display: flex;
  flex-direction: column;
  padding: 8px 0;
}

.doc-nav-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  color: #4a4a4a;
  text-decoration: none;
  font-size: 14px;
  transition: all 0.2s ease;
  position: relative;

  &:hover {
    background-color: #f0f7ff;
  }

  &.active {
    font-weight: 500;

    &::after {
      content: '';
      position: absolute;
      right: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 100%;
      background: currentColor;
      border-radius: 3px 0 0 3px;
    }
  }
}

.doc-nav-icon {
  font-size: 16px;
}

/* 内容区 - 独立滚动，白色背景 */
.doc-content {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  background-color: #fff;
}
</style>
