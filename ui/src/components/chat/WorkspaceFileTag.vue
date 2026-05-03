<script setup lang="ts">
import { computed } from 'vue'
import FileIcon from "@/components/workspace/FileIcon.vue";

const props = defineProps<{
  /** 文件在工作空间中的相对路径 */
  path: string
}>()

defineEmits(['inputTagPreview'])

/**
 * 从路径中提取文件名和父文件夹路径
 */
const fileInfo = computed(() => {
  const path = props.path
  const lastSlash = path.lastIndexOf('/')
  const name = lastSlash > -1 ? path.slice(lastSlash + 1) : path
  const folder = lastSlash > -1 ? path.slice(0, lastSlash) : ''
  return { name, folder }
})

/**
 * 从文件名解析扩展名（小写）
 */
const extension = computed(() => {
  const name = fileInfo.value.name
  const lastDot = name.lastIndexOf('.')
  return lastDot > -1 ? name.slice(lastDot + 1).toLowerCase() : ''
})
</script>

<template>
  <span class="workspace-file-tag" @click="$emit('inputTagPreview', {
    path: props.path,
    name: fileInfo.name,
    fullName: fileInfo.name,
    folderPath: fileInfo.folder,
    extension
  })">
    <FileIcon :file-name="fileInfo.name" width="14" />
    <span class="workspace-file-tag-name" :title="path">{{ fileInfo.name }}</span>
  </span>
</template>

<style scoped lang="scss">
.workspace-file-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  max-width: 100%;
  padding: 2px 8px;
  background: rgba(15, 116, 255, 0.1);
  border-radius: 4px;
  font-size: 13px;
  line-height: 1.4;
  color: #0F74FF;
  vertical-align: middle;
  white-space: nowrap;
  user-select: none;
  cursor: pointer;
}

.workspace-file-tag-icon {
  flex-shrink: 0;
  font-size: 14px;
  opacity: 0.85;
}

.workspace-file-tag-name {
  flex-shrink: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  //font-size: 14px;
}
</style>
