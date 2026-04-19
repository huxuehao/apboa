/**
 * Doc 子应用路由配置
 *
 * @author huxuehao
 */

import { createRouter, createWebHashHistory } from 'vue-router';
import type { Router } from 'vue-router';

/**
 * 创建路由实例
 */
const router: Router = createRouter({
  history: createWebHashHistory(),
  routes: [
    {
      path: '/',
      name: 'DocHome',
      component: () => import('./views/Home.vue'),
      meta: {
        title: '概述',
      },
    },
    {
      path: '/markdown-extension',
      name: 'MarkdownExtension',
      component: () => import('./views/MarkdownExtensionDoc.vue'),
      meta: {
        title: 'Markdown 扩展指南',
      },
    },
    {
      path: '/qa',
      name: 'QA',
      component: () => import('./views/QADoc.vue'),
      meta: {
        title: 'Q&A',
      },
    },
    {
      path: '/build',
      name: 'Build',
      component: () => import('./views/BuildDoc.vue'),
      meta: {
        title: '打包构建',
      },
    },
    {
      path: '/sensitive',
      name: 'Sensitive',
      component: () => import('./views/SensitiveDoc.vue'),
      meta: {
        title: '敏感词',
      },
    },
    {
      path: '/prompt',
      name: 'Prompt',
      component: () => import('./views/PromptDoc.vue'),
      meta: {
        title: '提示词模板',
      },
    },
    {
      path: '/model',
      name: 'Model',
      component: () => import('./views/ModelDoc.vue'),
      meta: {
        title: '模型供应商',
      },
    },
    {
      path: '/hook',
      name: 'Hook',
      component: () => import('./views/HookDoc.vue'),
      meta: {
        title: '钩子',
      },
    },
    {
      path: '/tool',
      name: 'Tool',
      component: () => import('./views/ToolDoc.vue'),
      meta: {
        title: '工具',
      },
    },
    {
      path: '/skill',
      name: 'Skill',
      component: () => import('./views/SkillDoc.vue'),
      meta: {
        title: '技能包',
      },
    },
    {
      path: '/mcp',
      name: 'Mcp',
      component: () => import('./views/McpDoc.vue'),
      meta: {
        title: 'MCP',
      },
    },
    {
      path: '/knowledge',
      name: 'Knowledge',
      component: () => import('./views/KnowledgeDoc.vue'),
      meta: {
        title: '知识库',
      },
    },
    {
      path: '/agent',
      name: 'Agent',
      component: () => import('./views/AgentDoc.vue'),
      meta: {
        title: '智能体',
      },
    },
  ],
  scrollBehavior() {
    return { top: 0 };
  },
});

/**
 * 设置页面标题
 */
router.beforeEach((to, _from, next) => {
  const defaultTitle = 'Apboa 使用手册';
  const title = to.meta?.title as string | undefined;
  document.title = title ? `${title} - ${defaultTitle}` : defaultTitle;
  next();
});

export default router;
