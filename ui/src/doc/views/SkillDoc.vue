/**
 * 技能包使用手册视图
 *
 * @author huxuehao
 * @component
 */
<script setup lang="ts">
import ModuleDoc from './ModuleDoc.vue'

const markdownFile = () => import('../content/skill/index.md?raw')
</script>

<template>
  <ModuleDoc :markdown-file="markdownFile" primary-color="#AB47BC" />
</template>
