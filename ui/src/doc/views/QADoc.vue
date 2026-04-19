/**
 * Q&A 常见问题视图
 *
 * @author huxuehao
 * @component
 */
<script setup lang="ts">
import ModuleDoc from './ModuleDoc.vue'

const markdownFile = () => import('../content/qa/index.md?raw')
</script>

<template>
  <ModuleDoc :markdown-file="markdownFile" primary-color="#FF9800" />
</template>
