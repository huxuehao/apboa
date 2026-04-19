/**
 * 钩子使用手册视图
 *
 * @author huxuehao
 * @component
 */
<script setup lang="ts">
import ModuleDoc from './ModuleDoc.vue'

const markdownFile = () => import('../content/hook/index.md?raw')
</script>

<template>
  <ModuleDoc :markdown-file="markdownFile" primary-color="#DB2781" />
</template>
