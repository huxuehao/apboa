<script setup lang="ts">
/**
 * 技能包文件树组件
 * 渲染文件系统 + DB 入库文件的完整树结构，支持右键菜单操作
 */
import { ref, reactive, watch, onMounted } from 'vue'
import {
  Modal,
  message,
} from 'ant-design-vue'
import {
  FolderFilled,
  FolderOpenFilled,
  CheckOutlined,
  EditOutlined,
  PlusOutlined,
  FolderAddOutlined,
  UploadOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import FileIcon from '@/components/workspace/FileIcon.vue'
import type { SkillFileTreeNode } from '@/types'
import * as skillApi from '@/api/skill'

const props = defineProps<{
  skillId: string
  treeData: SkillFileTreeNode[]
}>()

const emit = defineEmits<{
  (e: 'select', node: SkillFileTreeNode): void
  (e: 'refresh'): void
  (e: 'file-deleted', path: string): void
}>()

// 当前选中的节点 key
const selectedKeys = ref<string[]>([])
// 展开的节点 key 列表
const expandedKeys = ref<string[]>([])

// 右键菜单状态
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  node: null as SkillFileTreeNode | null,
  isRoot: false,
})

// 新建文件/文件夹弹窗
const createModal = reactive({
  visible: false,
  mode: 'file' as 'file' | 'dir',
  parentPath: '',
  name: '',
})

// 新建文件夹预设候选
const folderPresets = ['scripts', 'references', 'examples']

// 上传文件
const uploadInputRef = ref<HTMLInputElement | null>(null)
const uploadParentPath = ref('')

onMounted(() => {
  expandAll(props.treeData)
})

watch(() => props.treeData, (newVal) => {
  if (newVal && newVal.length > 0) {
    expandAll(newVal)
  }
})

function expandAll(nodes: SkillFileTreeNode[]) {
  const keys: string[] = []
  function collect(node: SkillFileTreeNode) {
    if (node.directory) {
      keys.push(getNodeKey(node))
    }
    if (node.children) {
      node.children.forEach(collect)
    }
  }
  nodes.forEach(collect)
  expandedKeys.value = keys
}

function getNodeKey(node: SkillFileTreeNode): string {
  return node.directory ? `dir:${node.path}` : `file:${node.path}`
}

/**
 * 处理节点点击
 */
/**
 * 处理节点右键菜单
 */
function handleContextMenu(e: MouseEvent, node: SkillFileTreeNode) {
  // SKILL.md 不显示任何右键菜单
  if (node.name === 'SKILL.md') {
    return
  }
  e.preventDefault()
  e.stopPropagation()
  contextMenu.visible = true
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.node = node
  contextMenu.isRoot = false
}

/**
 * 处理树空白区域右键菜单（根级创建）
 */
function handleTreeContextMenu(e: MouseEvent) {
  // 只在点击容器空白区域时触发
  const target = e.target as HTMLElement
  if (target.closest('.ant-tree-node-content-wrapper')) return
  e.preventDefault()
  contextMenu.visible = true
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.node = null
  contextMenu.isRoot = true
}

function closeContextMenu() {
  contextMenu.visible = false
  contextMenu.node = null
  contextMenu.isRoot = false
}

/**
 * 新建文件
 */
function showCreateFile(parentNode?: SkillFileTreeNode) {
  createModal.mode = 'file'
  createModal.parentPath = parentNode?.path || ''
  createModal.name = ''
  createModal.visible = true
  closeContextMenu()
}

/**
 * 新建文件夹
 */
function showCreateDir(parentNode?: SkillFileTreeNode) {
  createModal.mode = 'dir'
  createModal.parentPath = parentNode?.path || ''
  createModal.name = ''
  createModal.visible = true
  closeContextMenu()
}

/**
 * 确认创建
 */
async function handleCreateConfirm() {
  if (!createModal.name.trim()) {
    message.warning('请输入名称')
    return
  }

  if (createModal.mode === 'dir') {
    await skillApi.createDirectory(props.skillId, {
      parentPath: createModal.parentPath,
      dirName: createModal.name.trim(),
    })
    message.success('文件夹创建成功')
  } else {
    // 校验扩展名
    const extRes = await skillApi.getAllowedExtensions()
    const extensions = extRes.data.data || []
    const dotIdx = createModal.name.lastIndexOf('.')
    if (dotIdx > 0) {
      const ext = createModal.name.substring(dotIdx + 1).toLowerCase()
      if (!extensions.includes(ext)) {
        message.error(`不允许的文件类型: .${ext}，请使用白名单内的扩展名`)
        return
      }
    }
    await skillApi.createFile(props.skillId, {
      parentPath: createModal.parentPath,
      fileName: createModal.name.trim(),
      content: '',
    })
    message.success('文件创建成功')
  }
  createModal.visible = false
  emit('refresh')
}

/**
 * 删除节点
 */
async function handleDelete(node: SkillFileTreeNode) {
  if (node.directory && node.children && node.children.length > 0) {
    Modal.confirm({
      title: '确认删除',
      content: `目录 "${node.name}" 下包含文件，确定要递归删除吗？`,
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: () => doDelete(node),
    })
  } else {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除 "${node.name}" 吗？`,
      okText: '确定',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: () => doDelete(node),
    })
  }
  closeContextMenu()
}

async function doDelete(node: SkillFileTreeNode) {
  if (node.fileId) {
    await skillApi.deleteDbFile(node.fileId)
  } else {
    await skillApi.deleteFileSystemNode(props.skillId, {
      path: node.path,
      directory: node.directory,
    })
  }
  message.success('删除成功')
  emit('file-deleted', node.path)
  emit('refresh')
}

/**
 * 上传文件
 */
function showUpload(parentNode?: SkillFileTreeNode) {
  uploadParentPath.value = parentNode?.path || ''
  closeContextMenu()
  uploadInputRef.value?.click()
}

async function handleUpload(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  try {
    await skillApi.uploadFile(props.skillId, uploadParentPath.value, file)
    message.success('上传成功')
    emit('refresh')
  } finally {
    target.value = ''
  }
}

/**
 * 获取文件节点的保存状态图标
 */
function getStatusIcon(node: SkillFileTreeNode) {
  // 只对入库文件显示状态
  if (!node.fileId || node.directory) return null

  if (node.dirty) return EditOutlined
  return CheckOutlined
}

function getStatusIconColor(node: SkillFileTreeNode): string {
  if (node.dirty) return '#faad14'
  return '#bfbfbf'
}

/**
 * 右键菜单项
 */
function getContextMenuItems(): any[] {
  const node = contextMenu.node

  // 根级右键菜单（空白区域）
  if (!node) {
    return [
      { key: 'new-file', icon: PlusOutlined, label: '新建文件' },
      { key: 'new-dir', icon: FolderAddOutlined, label: '新建文件夹' },
    ]
  }

  const items: any[] = []

  if (node.directory) {
    items.push(
      { key: 'new-file', icon: PlusOutlined, label: '新建文件' },
      { key: 'new-dir', icon: FolderAddOutlined, label: '新建文件夹' },
      { key: 'upload', icon: UploadOutlined, label: '上传文件' },
      { type: 'divider' },
      { key: 'delete', icon: DeleteOutlined, label: '删除', danger: true },
    )
  } else {
    // 文件节点
    items.push(
      { key: 'delete', icon: DeleteOutlined, label: '删除', danger: true },
    )
  }

  return items
}

function handleMenuClick({ key }: { key: string }) {
  const node = contextMenu.node

  // 根级操作：parentPath 为空
  const parentNode = node?.directory ? node : undefined

  switch (key) {
    case 'new-file':
      showCreateFile(parentNode)
      break
    case 'new-dir':
      showCreateDir(parentNode)
      break
    case 'upload':
      showUpload(parentNode)
      break
    case 'delete':
      if (node) handleDelete(node)
      break
  }
}

/**
 * 将扁平树结构转为 ant-design-vue Tree 需要的格式
 */
function buildTreeData(nodes: SkillFileTreeNode[]): any[] {
  return nodes.map(node => ({
    key: getNodeKey(node),
    title: node.name,
    isLeaf: !node.directory,
    directory: node.directory,
    fileName: node.name,
    rawNode: node,
    children: node.children ? buildTreeData(node.children) : undefined,
  }))
}

/**
 * 暴露方法给父组件
 */
defineExpose({
  showCreateFile,
  showCreateDir,
})
</script>

<template>
  <div class="skill-tree" @click="closeContextMenu" @contextmenu="handleTreeContextMenu">
    <!-- 隐藏的上传 input -->
    <input
      ref="uploadInputRef"
      type="file"
      style="display: none"
      @change="handleUpload"
    />

    <a-tree
      v-model:selectedKeys="selectedKeys"
      v-model:expandedKeys="expandedKeys"
      :tree-data="buildTreeData(treeData)"
      :show-icon="true"
      :block-node="true"
      @select="(keys: string[], { node }: any) => {
        const raw = node.rawNode as SkillFileTreeNode
        const k = keys[0]
        if (!k) return
        if (raw.directory) {
          selectedKeys = []
          const i = expandedKeys.indexOf(k)
          if (i >= 0) expandedKeys.splice(i, 1)
          else expandedKeys.push(k)
        } else {
          emit('select', raw)
        }
      }"
    >
      <template #icon="{ directory, fileName, key }: any">
        <span v-if="directory" style="color: #FAAD14; font-size: 20px; display: inline-flex; align-items: center">
          <FolderOpenFilled v-if="expandedKeys.includes(key)" />
          <FolderFilled v-else />
        </span>
        <FileIcon v-else :file-name="fileName" :width="16" />
      </template>
      <template #title="{ title, rawNode: node }: any">
        <span
          class="tree-node-title"
          @contextmenu="(e: MouseEvent) => handleContextMenu(e, node)"
        >
          <span class="node-name">{{ title }}</span>
          <span v-if="node.fileId && !node.directory" class="node-status">
            <component
              :is="getStatusIcon(node)"
              :style="{ color: getStatusIconColor(node), fontSize: '12px', marginLeft: '6px' }"
            />
          </span>
        </span>
      </template>
    </a-tree>

    <!-- 自定义右键菜单（固定定位） -->
    <Teleport to="body">
      <div
        v-if="contextMenu.visible"
        class="context-menu-overlay"
        @click="closeContextMenu"
        @contextmenu.prevent="closeContextMenu"
      >
        <div
          class="context-menu-panel"
          :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        >
          <div
            v-for="item in getContextMenuItems()"
            :key="item.key"
            class="context-menu-item"
            :class="{ 'context-menu-item--danger': item.danger }"
            @click.stop="handleMenuClick({ key: item.key }); closeContextMenu()"
          >
            <component :is="item.icon" style="font-size: 16px; margin-right: 8px" />
            <span>{{ item.label }}</span>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 新建文件/文件夹弹窗 -->
    <a-modal
      v-model:open="createModal.visible"
      :title="createModal.mode === 'file' ? '新建文件' : '新建文件夹'"
      :ok-text="'确定'"
      :cancel-text="'取消'"
      @ok="handleCreateConfirm"
    >
      <a-form layout="vertical">
        <a-form-item label="父目录">
          <a-input :value="createModal.parentPath || '(根目录)' " disabled />
        </a-form-item>
        <a-form-item :label="createModal.mode === 'file' ? '文件名' : '文件夹名'">
          <div v-if="createModal.mode === 'dir'" style="margin-bottom: 6px">
            <span style="font-size: 12px; color: #999; margin-right: 4px">快捷选择：</span>
            <span
              v-for="p in folderPresets"
              :key="p"
              style="cursor: pointer; margin-right: 10px;"
              @click="createModal.name = p"
            >
              {{ p }}
            </span>
          </div>
          <a-input
            v-model:value="createModal.name"
            :placeholder="createModal.mode === 'file' ? '例如: helper.py' : '例如: utils'"
            @pressEnter="handleCreateConfirm"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.skill-tree {
  height: 100%;
  overflow: auto;
  padding: 8px;
}

.tree-node-title {
  display: flex;
  align-items: center;
  width: 100%;
  font-size: 16px;
  padding: 2px 0;
  cursor: default;
}

.node-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.node-status {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
}
</style>

<style>
/* 右键菜单覆盖层（全局样式，不使用 scoped） */
.context-menu-overlay {
  position: fixed;
  inset: 0;
  z-index: 1050;
}

.context-menu-panel {
  position: fixed;
  z-index: 1051;
  min-width: 140px;
  background: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 4px 0;
}

.context-menu-item {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.2s;
}

.context-menu-item:hover {
  background: #f5f5f5;
}

.context-menu-item--danger {
  color: #ff4d4f;
}

.context-menu-item--danger:hover {
  background: #fff2f0;
}

/* 树节点内部对齐：图标与文字同行 */
.skill-tree .ant-tree-node-content-wrapper {
  display: flex !important;
  align-items: center !important;
}

.skill-tree .ant-tree-iconEle {
  display: inline-flex !important;
  align-items: center !important;
}

.skill-tree .ant-tree-title {
  display: flex !important;
  align-items: center !important;
  flex: 1;
}

/* 隐藏展开箭头 */
.skill-tree .ant-tree-switcher {
  display: none !important;
}
</style>
