<script setup lang="ts">
import { computed } from 'vue'
import {
  FileOutlined,
  FileImageOutlined,
  FileTextOutlined,
  FilePdfOutlined,
  FileZipOutlined,
  FileMarkdownOutlined,
  FileExcelOutlined,
  FileWordOutlined,
  FilePptOutlined
} from '@ant-design/icons-vue'

const props = defineProps<{
  /** 文件在工作空间中的相对路径 */
  path: string
}>()

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

/**
 * 根据文件后缀返回对应图标组件
 */
const fileIcon = computed(() => {
  const e = extension.value
  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp', 'ico'].includes(e)) return FileImageOutlined
  if (['pdf'].includes(e)) return FilePdfOutlined
  if (['zip', 'tar', 'gz', 'rar', '7z', 'bz2', 'xz', 'tgz'].includes(e)) return FileZipOutlined
  if (['md', 'markdown'].includes(e)) return FileMarkdownOutlined
  if (['xls', 'xlsx', 'csv'].includes(e)) return FileExcelOutlined
  if (['doc', 'docx'].includes(e)) return FileWordOutlined
  if (['ppt', 'pptx'].includes(e)) return FilePptOutlined
  if (['txt', 'log', 'json', 'xml', 'yaml', 'yml', 'toml', 'ini', 'conf', 'sh', 'bash', 'py', 'js', 'ts', 'java', 'go', 'rs', 'c', 'cpp', 'h'].includes(e)) return FileTextOutlined
  return FileOutlined
})
</script>

<template>
  <span class="workspace-file-tag">
    <span class="workspace-file-tag-name" :title="fileInfo.folder ? fileInfo.folder + '/' + fileInfo.name : fileInfo.name">{{ fileInfo.name }}</span>
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
}
</style>
