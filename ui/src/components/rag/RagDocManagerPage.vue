/**
 * RAG文档管理主页组件
 * 采用侧边栏+内容区布局，包含"文档"和"检索"两个功能菜单
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref } from 'vue'
import { FileTextOutlined, SearchOutlined } from '@ant-design/icons-vue'
import DocumentList from './DocumentList.vue'
import SearchTest from './SearchTest.vue'

defineProps<{
  docManagerKbName: string
  knowledgeBaseConfigId: string
}>()

/**
 * 菜单项配置
 */
const menuItems = [
  { key: 'documents', label: '文档分块', icon: FileTextOutlined },
  { key: 'search', label: '检索测试', icon: SearchOutlined }
]

/**
 * 当前激活的菜单项
 */
const activeMenu = ref<string>('documents')
</script>

<template>
  <div class="rag-doc-manager-container">
    <!-- 左侧菜单栏 -->
    <div class="rag-doc-manager-sidebar">
      <div class="rag-doc-manager-menu-title">{{docManagerKbName}}</div>
      <div class="rag-doc-manager-menu-list">
        <div
          v-for="item in menuItems"
          :key="item.key"
          class="rag-doc-manager-menu-item"
          :class="{ active: activeMenu === item.key }"
          @click="activeMenu = item.key"
        >
          <component :is="item.icon" class="rag-doc-manager-menu-icon" />
          <span class="rag-doc-manager-menu-label">{{ item.label }}</span>
        </div>
      </div>
    </div>

    <!-- 分割线 -->
    <div class="rag-doc-manager-divider"></div>

    <!-- 右侧内容区 -->
    <div class="rag-doc-manager-content">
      <DocumentList
        v-if="activeMenu === 'documents'"
        :knowledge-base-config-id="knowledgeBaseConfigId"
      />
      <SearchTest
        v-else-if="activeMenu === 'search'"
        :knowledge-base-config-id="knowledgeBaseConfigId"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/rag/_doc-manager.scss' as *;
</style>
