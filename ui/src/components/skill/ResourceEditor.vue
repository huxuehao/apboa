/**
 * 通用资源项编辑器组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Modal, message } from 'ant-design-vue'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import SmartCodeEditor from '@/components/editor/SmartCodeEditor.vue'

/**
 * 资源项接口
 */
interface ResourceItem {
  prefix: string
  name: string
  content: string
}

/**
 * Props定义
 */
const props = defineProps<{
  modelValue: ResourceItem[]
  title: string
  prefix: string
  showLanguageSelector?: boolean
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: ResourceItem[]]
}>()

const visible = ref<boolean>(false)
const itemFormVisible = ref<boolean>(false)
const currentItemIndex = ref<number>(-1)
const itemForm = ref<ResourceItem>({
  prefix: props.prefix,
  name: '',
  content: ''
})

/**
 * 本地数据副本
 */
const localItems = computed({
  get: () => props.modelValue || [],
  set: (value) => emit('update:modelValue', value)
})

/**
 * 打开主弹窗
 */
function open() {
  visible.value = true
}

/**
 * 关闭主弹窗
 */
function close() {
  visible.value = false
}

/**
 * 打开添加项弹窗
 */
function handleAdd() {
  currentItemIndex.value = -1
  itemForm.value = {
    prefix: props.prefix,
    name: '',
    content: ''
  }
  itemFormVisible.value = true
}

/**
 * 打开编辑项弹窗
 */
function handleEdit(index: number) {
  currentItemIndex.value = index
  const item = localItems.value[index]
  itemForm.value = {
    prefix: item?.prefix || props.prefix,
    name: item?.name || '',
    content: item?.content || ''
  }
  itemFormVisible.value = true
}

/**
 * 删除项
 */
function handleDelete(index: number) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除这一项吗？',
    onOk: () => {
      const newItems = [...localItems.value]
      newItems.splice(index, 1)
      localItems.value = newItems
      message.success('删除成功')
    }
  })
}

/**
 * 保存项
 */
function handleItemSave() {
  if (!itemForm.value.name.trim()) {
    message.warning('请输入文件名')
    return
  }

  if (!itemForm.value.content.trim()) {
    message.warning('请输入内容')
    return
  }

  const newItems = [...localItems.value]
  if (currentItemIndex.value === -1) {
    newItems.push({ ...itemForm.value })
  } else {
    newItems[currentItemIndex.value] = { ...itemForm.value }
  }

  localItems.value = newItems
  itemFormVisible.value = false
  message.success(currentItemIndex.value === -1 ? '添加成功' : '修改成功')
}

/**
 * 暴露方法
 */
defineExpose({
  open,
  close
})
</script>

<template>
  <ApboaModal
    v-model:open="visible"
    :title="title"
    @ok="close"
  >
    <div class="resource-editor">
      <AButton type="primary" @click="handleAdd" class="mb-md">
        <PlusOutlined />
        添加
      </AButton>

      <AList
        v-if="localItems.length > 0"
        :data-source="localItems"
        bordered
      >
        <template #renderItem="{ item, index }">
          <AListItem>
            <template #actions>
              <AButton type="text" size="small" @click="handleEdit(index)">
                <EditOutlined />
              </AButton>
              <AButton type="text" size="small" danger @click="handleDelete(index)">
                <DeleteOutlined />
              </AButton>
            </template>
            <div class="item-name">{{ item.name }}</div>
          </AListItem>
        </template>
      </AList>

      <AEmpty v-else description="暂无数据" class="mt-md" />
    </div>
  </ApboaModal>

  <ApboaModal
    v-model:open="itemFormVisible"
    :title="currentItemIndex === -1 ? '添加项' : '编辑项'"
    @ok="handleItemSave"
  >
    <AForm layout="vertical">
      <AFormItem label="文件名" required>
        <AInput
          v-model:value="itemForm.name"
          placeholder="请输入文件名"
        />
        <div class="text-placeholder">文件名需要带有后缀，例如 xxx.md、xxx.py、xxx.sh</div>
      </AFormItem>

      <AFormItem label="内容" required>
        <SmartCodeEditor
          v-if="itemFormVisible"
          v-model="itemForm.content"
          :language="showLanguageSelector ? 'java' : 'markdown'"
          :show-change-language="showLanguageSelector"
          height="350px"
        />
      </AFormItem>
    </AForm>
  </ApboaModal>
</template>

<style scoped lang="scss">
.resource-editor {
  .item-name {
    font-size: var(--font-size-base);
    color: var(--color-text-primary);
  }
}
</style>
