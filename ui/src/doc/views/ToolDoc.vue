/**
 * 工具使用手册视图
 *
 * @author huxuehao
 * @component
 */
<script setup lang="ts">
import ModuleDoc from './ModuleDoc.vue'

const markdownFile = () => import('../content/tool/index.md?raw')
</script>

<template>
  <ModuleDoc :markdown-file="markdownFile" primary-color="#399DF2" />
</template>
