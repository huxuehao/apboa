/**
 * 敏感词使用手册视图
 *
 * @author huxuehao
 * @component
 */
<script setup lang="ts">
import ModuleDoc from './ModuleDoc.vue'

const markdownFile = () => import('../content/sensitive/index.md?raw')
</script>

<template>
  <ModuleDoc :markdown-file="markdownFile" primary-color="#E55B5B" />
</template>
