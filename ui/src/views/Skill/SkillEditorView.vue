<script setup lang="ts">
/**
 * 技能编辑器页面
 * 左侧文件树 + 右侧编辑器布局
 */
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount, h } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { Modal, Input, Select, message } from 'ant-design-vue'
import {
  ArrowLeftOutlined,
  DeleteOutlined,
  PlusOutlined,
  FolderAddOutlined,
  DownloadOutlined,
} from '@ant-design/icons-vue'
import SkillTree from '@/components/skill/SkillTree.vue'
import SkillEditor from '@/components/skill/SkillEditor.vue'
import type { SkillFileTreeNode, SkillPackageVO } from '@/types'
import * as skillApi from '@/api/skill'

const route = useRoute()
const router = useRouter()

/**
 * 判断是否为新建模式（通过路由名判断）
 */
const isNew = computed(() => route.name === 'SkillEditorNew')
const skillId = computed(() => route.params.id as string)

// 技能包基本信息
const skillInfo = reactive({
  id: '' as string | number,
  name: '',
  description: '',
  category: '',
  enabled: true,
  tools: [] as string[],
})

// 文件树数据
const treeData = ref<SkillFileTreeNode[]>([])

// 当前选中的文件
const selectedFile = ref<SkillFileTreeNode | null>(null)

// 是否有未保存的修改
const hasDirty = ref(false)

// 编辑器引用
const editorRef = ref<InstanceType<typeof SkillEditor> | null>(null)
// 文件树引用
const skillTreeRef = ref<InstanceType<typeof SkillTree> | null>(null)

// 是否正在加载
const loading = ref(false)

// 分类列表
const categories = ref<string[]>([])

// 允许在编辑器中打开的文件扩展名白名单
const allowedExtensions = ref<string[]>([])

// 左侧面板宽度（可拖拽调整）
const leftPanelWidth = ref(280)
const isResizing = ref(false)

/**
 * 拖拽分割条开始
 */
function handleResizeStart(e: MouseEvent) {
  e.preventDefault()
  isResizing.value = true
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'
  document.addEventListener('mousemove', handleResizeMove)
  document.addEventListener('mouseup', handleResizeEnd)
}

/**
 * 拖拽过程中更新宽度
 */
function handleResizeMove(e: MouseEvent) {
  if (!isResizing.value) return
  const width = Math.max(240, Math.min(500, e.clientX))
  leftPanelWidth.value = width
}

/**
 * 拖拽结束
 */
function handleResizeEnd() {
  isResizing.value = false
  document.body.style.userSelect = ''
  document.body.style.cursor = ''
  document.removeEventListener('mousemove', handleResizeMove)
  document.removeEventListener('mouseup', handleResizeEnd)
}

onMounted(async () => {
  await loadCategories()
  await loadExtensions()

  if (isNew.value) {
    // 创建模式：先弹出基本信息填写
    showCreateModal()
  } else {
    await loadSkillDetail()
    await refreshTree()
  }
})

/**
 * 监听路由参数变化，处理新建完成后导航回编辑页的情况
 */
watch(() => route.params.id, async (newId) => {
  if (newId && newId !== 'new') {
    await loadSkillDetail()
    await refreshTree()
  }
})

/**
 * 加载分类列表
 */
async function loadCategories() {
  try {
    const res = await skillApi.listCategories()
    categories.value = res.data.data || []
  } catch {
    // ignore
  }
}

/**
 * 加载允许编辑的文件扩展名白名单
 */
async function loadExtensions() {
  try {
    const res = await skillApi.getAllowedExtensions()
    allowedExtensions.value = res.data.data || []
  } catch {
    // ignore
  }
}

/**
 * 加载技能包详情
 */
async function loadSkillDetail() {
  loading.value = true
  try {
    const res = await skillApi.detail(skillId.value)
    const vo = res.data.data as SkillPackageVO
    if (vo) {
      skillInfo.id = vo.id
      skillInfo.name = vo.name
      skillInfo.description = vo.description
      skillInfo.category = vo.category
      skillInfo.enabled = vo.enabled
      skillInfo.tools = vo.tools || []
    }
  } catch {
    message.error('加载技能包详情失败')
  } finally {
    loading.value = false
  }
}

/**
 * 刷新文件树
 */
async function refreshTree() {
  if (!skillId.value || isNew.value) return
  try {
    const res = await skillApi.getTree(skillId.value)
    treeData.value = res.data.data || []
    // 校验当前选中文件是否仍存在于树中
    if (selectedFile.value) {
      const stillExists = findNodeByPath(treeData.value, selectedFile.value.path)
      if (!stillExists) {
        selectedFile.value = null
      }
    }
    // 没有选中文件时，自动打开 SKILL.md
    if (!selectedFile.value) {
      const skillMd = findNodeByPath(treeData.value, 'SKILL.md')
      if (skillMd) {
        selectedFile.value = skillMd
      }
    }
  } catch {
    message.error('加载文件树失败')
  }
}

/**
 * 处理删除文件通知——如果被删的是当前打开的文件，清空编辑区
 */
function handleFileDeleted(deletedPath: string) {
  if (selectedFile.value?.path === deletedPath) {
    selectedFile.value = null
  }
}

/** 大文件阈值：500KB */
const LARGE_FILE_SIZE = 500 * 1024

/**
 * 下载技能包为压缩包
 */
async function handleDownloadZip() {
  try {
    const res = await skillApi.downloadZip(skillId.value)
    const blob = res.data as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${skillInfo.name || 'skill'}.zip`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    message.error('下载失败')
  }
}

/**
 * 处理文件选择（先判断扩展名合法性，再判断文件大小）
 */
function handleFileSelect(node: SkillFileTreeNode) {
  // 1. 检查扩展名是否在白名单中（非白名单文件引导下载）
  const ext = (node.extension || '').toLowerCase()
  if (ext && !allowedExtensions.value.includes(ext)) {
    Modal.confirm({
      title: '不支持的文件类型',
      content: '文件类型 .' + ext + ' 不支持在线预览，请下载到本地进行查看。',
      okText: '下载',
      cancelText: '取消',
      onOk: () => triggerFileDownload(node.name, node.path),
    })
    return
  }

  // 2. 检查文件大小（过大文件引导下载）
  if (node.fileSize > LARGE_FILE_SIZE) {
    Modal.confirm({
      title: '文件过大',
      content: '文件过大（' + formatFileSize(node.fileSize) + '），请下载到本地进行查看。',
      okText: '下载',
      cancelText: '取消',
      onOk: () => triggerFileDownload(node.name, node.path),
    })
    return
  }

  // 3. 正常打开，检查是否有未保存的修改
  if (selectedFile.value && hasDirty.value && editorRef.value?.isDirty()) {
    Modal.confirm({
      title: '未保存的更改',
      content: '当前文件有未保存的修改，是否放弃更改？',
      okText: '放弃更改',
      cancelText: '继续编辑',
      okButtonProps: { danger: true },
      onOk: () => {
        selectedFile.value = node
        hasDirty.value = false
      },
    })
  } else {
    selectedFile.value = node
  }
}

/**
 * 触发文件下载
 */
async function triggerFileDownload(fileName: string, filePath: string) {
  try {
    const res = await skillApi.downloadFile(skillId.value, filePath)
    const blob = res.data as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    message.error('下载失败')
  }
}

/**
 * 格式化文件大小
 */
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/**
 * 递归查找指定路径的树节点
 */
function findNodeByPath(nodes: SkillFileTreeNode[], targetPath: string): SkillFileTreeNode | null {
  for (const node of nodes) {
    if (node.path === targetPath) return node
    if (node.children) {
      const found = findNodeByPath(node.children, targetPath)
      if (found) return found
    }
  }
  return null
}

/**
 * 处理 dirty 状态变化
 */
function handleDirtyChange(dirty: boolean) {
  hasDirty.value = dirty
}

/**
 * 返回列表
 */
async function handleGoBack() {
  if (hasDirty.value) {
    Modal.confirm({
      title: '未保存的更改',
      content: '您有未保存的文件修改，确定要离开吗？',
      okText: '确定离开',
      cancelText: '继续编辑',
      okButtonProps: { danger: true },
      onOk: () => {
        router.push({ name: 'Skill' })
      },
    })
  } else {
    router.push({ name: 'Skill' })
  }
}

/**
 * 新建文件（工具栏按钮）
 */
function handleNewFile() {
  skillTreeRef.value?.showCreateFile()
}

/**
 * 新建文件夹（工具栏按钮）
 */
function handleNewFolder() {
  skillTreeRef.value?.showCreateDir()
}

/**
 * 删除技能包
 */
async function handleDeleteSkill() {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除技能包 "${skillInfo.name}" 吗？此操作不可恢复。`,
    okText: '确定删除',
    cancelText: '取消',
    okButtonProps: { danger: true },
    async onOk() {
      try {
        await skillApi.remove([String(skillInfo.id)])
        message.success('删除成功')
        router.push({ name: 'Skill' })
      } catch {
        message.error('删除失败')
      }
    },
  })
}

/**
 * 显示创建技能包弹窗
 */
function showCreateModal() {
  let name = ''
  let description = ''
  let category = ''

  Modal.confirm({
    title: '新建技能包',
    content: () =>
      h('div', { style: 'padding: 12px 0' }, [
        h('div', { style: 'margin-bottom: 12px' }, [
          h('label', { style: 'display: block; margin-bottom: 4px; font-size: 13px' }, '技能包名称'),
          h(Input, {
            placeholder: '请输入技能包名称',
            onChange: (e: Event) => { name = (e.target as HTMLInputElement).value },
          }),
        ]),
        h('div', { style: 'margin-bottom: 12px' }, [
          h('label', { style: 'display: block; margin-bottom: 4px; font-size: 13px' }, '描述'),
          h(Input.TextArea, {
            placeholder: '请输入技能包描述',
            rows: 3,
            onChange: (e: Event) => { description = (e.target as HTMLTextAreaElement).value },
          }),
        ]),
        h('div', [
          h('label', { style: 'display: block; margin-bottom: 4px; font-size: 13px' }, '分类'),
          h(Select, {
            placeholder: '请选择分类',
            style: 'width: 100%',
            options: categories.value.map(c => ({ value: c, label: c })),
            onChange: (val: string) => { category = val },
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          } as any),
        ]),
      ]),
    okText: '创建',
    cancelText: '取消',
    async onOk() {
      if (!name.trim()) {
        message.warning('请输入技能包名称')
        return Promise.reject()
      }
      try {
        const res = await skillApi.save({
          name: name.trim(),
          description,
          category,
          enabled: true,
          tools: [],
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        } as any)
        if (res.data.code === 200) {
          const newId = String(res.data.data)
          message.success('创建成功')
          router.replace({ name: 'SkillEditor', params: { id: newId } })
        }
      } catch {
        message.error('创建失败')
        return Promise.reject()
      }
    },
    onCancel() {
      // 用户取消创建，返回列表
      router.push({ name: 'Skill' })
    },
  })
}

/**
 * 浏览器关闭/刷新前检查
 */
function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (hasDirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

onMounted(() => {
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  document.removeEventListener('mousemove', handleResizeMove)
  document.removeEventListener('mouseup', handleResizeEnd)
  document.body.style.userSelect = ''
  document.body.style.cursor = ''
})

/**
 * 路由离开守卫
 */
onBeforeRouteLeave((_to, _from, next) => {
  if (hasDirty.value) {
    Modal.confirm({
      title: '未保存的更改',
      content: '您有未保存的文件修改，确定要离开吗？',
      okText: '确定离开',
      cancelText: '继续编辑',
      okButtonProps: { danger: true },
      onOk: () => next(),
      onCancel: () => next(false),
    })
  } else {
    next()
  }
})
</script>

<template>
  <div class="skill-editor-view" :style="{ height: 'calc(100vh - 56px)', gridTemplateColumns: `${leftPanelWidth}px 4px 1fr` }">
    <!-- 左侧面板 -->
    <div class="left-panel">
      <!-- 顶部工具栏 -->
      <div class="panel-toolbar">
        <a-button type="text" size="small" @click="handleGoBack">
          <ArrowLeftOutlined />
        </a-button>
        <span class="skill-name">
          <span v-if="!isNew" class="skill-name-text">{{ skillInfo.name }}</span>
          <span v-else>新建技能包</span>
        </span>
        <span style="flex:1"></span>
        <a-button v-if="!isNew" type="text" size="small" title="新建文件" @click="handleNewFile">
          <PlusOutlined />
        </a-button>
        <a-button v-if="!isNew" type="text" size="small" title="新建文件夹" @click="handleNewFolder">
          <FolderAddOutlined />
        </a-button>
        <a-button v-if="!isNew" type="text" size="small" title="下载技能包" @click="handleDownloadZip">
          <DownloadOutlined />
        </a-button>
      </div>

      <!-- 文件树 -->
      <div class="tree-container" v-if="!isNew">
        <SkillTree
          ref="skillTreeRef"
          :skill-id="skillId"
          :tree-data="treeData"
          @select="handleFileSelect"
          @refresh="refreshTree"
          @file-deleted="handleFileDeleted"
        />
      </div>

      <!-- 删除按钮 -->
      <div class="panel-footer" v-if="!isNew">
        <a-button type="text" danger size="small" @click="handleDeleteSkill">
          <DeleteOutlined />
          <span>删除技能包</span>
        </a-button>
      </div>
    </div>

    <!-- 可拖拽分割条 -->
    <div
      class="resize-handle"
      :class="{ 'resize-handle--active': isResizing }"
      @mousedown="handleResizeStart"
    />

    <!-- 右侧面板 -->
    <div class="right-panel">
      <!-- 编辑区域 -->
      <div class="editor-container" v-if="!isNew">
        <SkillEditor
          ref="editorRef"
          :skill-id="skillId"
          :file="selectedFile"
          @dirty-change="handleDirtyChange"
          @saved="loadSkillDetail"
        />
      </div>

      <!-- 创建模式占位 -->
      <div v-else class="create-placeholder">
        请先填写技能包基本信息
      </div>
    </div>
  </div>
</template>

<style scoped>
.skill-editor-view {
  display: grid;
  overflow: hidden;
}

.left-panel {
  display: flex;
  flex-direction: column;
  background: #fafafa;
  border-right: 1px solid #f0f0f0;
  overflow: hidden;
}

.panel-toolbar {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  gap: 8px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
  background-color: #fff;
}

.skill-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tree-container {
  flex: 1;
  overflow: auto;
  background-color: #fff;
}

.panel-footer {
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
  background-color: #fff;
  text-align: center;
}

.right-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
}

.editor-container {
  flex: 1;
  overflow: hidden;
}

.create-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bfbfbf;
  font-size: 14px;
}

/* 可拖拽分割条 */
.resize-handle {
  width: 4px;
  cursor: col-resize;
  background: transparent;
  transition: background 0.2s;
  z-index: 10;
}

.resize-handle:hover,
.resize-handle--active {
  background: #1890ff;
}
</style>
