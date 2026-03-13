/**
 * 新增技能包卡片组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { PlusOutlined, ImportOutlined, DownloadOutlined, CloudDownloadOutlined } from '@ant-design/icons-vue'

/**
 * Emits定义
 */
defineEmits<{
  click: [],
  importLocal: []
  importGit: []
  importUpload: []
}>()


</script>

<template>
  <div class="create-card flex-col flex-center cursor-pointer" @click="$emit('click')">
    <PlusOutlined class="create-icon" />
<!--    <div class="create-text text-secondary">添加新技能</div>-->
<!--    <div class="create-desc text-placeholder text-sm">创建技能包赋予智能体专业领域知识</div>-->
    <div class="import text-placeholder text-sm" @click.stop="$emit('importLocal')">
      <DownloadOutlined /> <span>装载本地技能包</span>
    </div>
    <div class="import text-placeholder text-sm" @click.stop="$emit('importUpload')">
      <ImportOutlined /> <span>导入技能压缩包</span>
    </div>
    <div class="import text-placeholder text-sm" @click.stop="$emit('importGit')">
      <CloudDownloadOutlined /> <span>下载 Git 技能包</span>
    </div>
  </div>
</template>

<style scoped lang="scss">
.create-card {
  min-height: 180px;
  padding: var(--spacing-md);
  background-color: var(--color-bg-white);
  border: 2px dotted var(--color-border-base);
  border-radius: var(--border-radius-lg);
  transition: all var(--transition-base);

  &:hover {
    border-color: var(--color-primary);
    background-color: var(--color-bg-light);

    .create-icon {
      color: var(--color-primary);
      transform: scale(1.1);
    }
  }

  .create-icon {
    font-size: 32px;
    color: var(--color-text-secondary);
    margin-bottom: var(--spacing-sm);
    transition: all var(--transition-base);
  }

  .create-text {
    font-size: var(--font-size-base);
    font-weight: 500;
    margin-bottom: var(--spacing-xs);
  }

  .create-desc {
    text-align: center;
  }

  .import {
    cursor: pointer;
    margin-top: 10px;
    //text-decoration: underline wavy var(--color-primary);  /* 下划线、波浪线、红色 */
    //text-underline-offset: 5px;           /* 下划线偏移距离（CSS3） */
    //text-decoration-thickness: 1px;       /* 下划线粗细（CSS3） */

    &:hover {
      color: var(--color-primary);
    }
  }
}
</style>
