/**
 * 技能包卡片组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, defineComponent } from 'vue'
import { EllipsisOutlined, AppstoreOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import type { SkillPackageVO } from '@/types'
import * as skillApi from '@/api/skill'
import {
  createViewItem,
  createEditItem,
  createEnableItem,
  createDeleteItem,
  createSetCategoryItem,
  createDivider,
} from '@/composables/useCardMenuItems'

/**
 * Props定义
 */
const props = defineProps<{
  data: SkillPackageVO
  categories: string[]
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  view: [id: string]
  edit: [id: string]
  delete: [id: string]
  enable: [id: string]
  setCategory: [id: string, category: string]
}>()

/**
 * 格式化更新时间
 */
const formattedTime = computed(() => {
  if (!props.data.updatedAt) return ''
  const date = new Date(props.data.updatedAt)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
})

/**
 * 操作菜单项
 */
const menuItems = computed(() => [
  createViewItem(),
  createEditItem(),
  createSetCategoryItem(),
  createEnableItem(props.data.enabled),
  createDivider(),
  createDeleteItem(),
])

// 分类设置弹窗
const categoryModalVisible = ref(false)
const categoryValue = ref('')
const categorySearchText = ref('')
const categoryNewName = ref('')
// 本地分类列表副本，支持运行时新增
const localCategories = ref<string[]>([...props.categories])

const filteredCategories = computed(() => {
  if (!categorySearchText.value) {
    return localCategories.value
  }
  const searchLower = categorySearchText.value.toLowerCase()
  const filtered = localCategories.value.filter(cat =>
    cat.toLowerCase().includes(searchLower)
  )
  if (!filtered.includes(categorySearchText.value)) {
    filtered.unshift(categorySearchText.value)
  }
  return filtered
})

const VNodes = defineComponent({
  props: {
    vnodes: { type: Object, required: true },
  },
  render() {
    return this.vnodes
  },
})

function addCategory(e: Event) {
  e.preventDefault()
  if (!categoryNewName.value) return
  if (!localCategories.value.includes(categoryNewName.value)) {
    localCategories.value.push(categoryNewName.value)
  }
  categoryValue.value = categoryNewName.value
  categoryNewName.value = ''
}

function openCategoryModal() {
  categoryValue.value = props.data.category || ''
  categorySearchText.value = ''
  categoryNewName.value = ''
  categoryModalVisible.value = true
}

async function handleCategoryConfirm() {
  if (!categoryValue.value.trim()) {
    message.warning('请选择或输入分类')
    return
  }
  try {
    const detailRes = await skillApi.detail(String(props.data.id))
    const vo = detailRes.data.data as SkillPackageVO
    if (vo) {
      await skillApi.update({
        id: String(vo.id),
        name: vo.name,
        description: vo.description,
        category: categoryValue.value,
        skillContent: '',
        references: null,
        examples: null,
        scripts: null,
        tools: vo.tools || [],
      })
      message.success('分类设置成功')
      categoryModalVisible.value = false
      emit('setCategory', String(props.data.id), categoryValue.value)
    }
  } catch {
    message.error('设置分类失败')
  }
}

/**
 * 处理菜单点击
 */
function handleMenuClick({ key }: { key: string }) {
  switch (key) {
    case 'view':
      emit('view', props.data.id as string)
      break
    case 'edit':
      emit('edit', props.data.id as string)
      break
    case 'setCategory':
      openCategoryModal()
      break
    case 'enable':
      emit('enable', props.data.id as string)
      break
    case 'delete':
      emit('delete', props.data.id as string)
      break
  }
}
</script>

<template>
  <div class="skill-card">
    <div class="card-header flex items-center gap-sm">
      <div class="card-avatar flex-center" :class="{ disabled: !data.enabled }"><AppstoreOutlined /></div>
      <div class="card-name flex-1 truncate" :title="data.name" @click="emit('view', data.id as string)">{{ data.name }}</div>
      <ADropdown :trigger="['hover']">
        <AButton type="text" size="small" v-permission="['EDIT','ADMIN']">
          <EllipsisOutlined />
        </AButton>
        <template #overlay>
          <AMenu @click="handleMenuClick" :items="menuItems"></AMenu>
        </template>
      </ADropdown>
    </div>

    <div class="card-content line-clamp-3" :title="data.description">
      {{ data.description }}
    </div>

    <div class="card-footer flex items-center justify-between">
      <div class="card-tags flex items-center gap-xs">
        <ATag color="default" class="tag">{{ data.category || '未设置标签' }}</ATag>
      </div>
      <div class="card-time text-placeholder text-xs">更新于 {{ formattedTime }}</div>
    </div>
  </div>

  <!-- 分类设置弹窗 -->
  <a-modal
    v-model:open="categoryModalVisible"
    title="设置分类"
    :ok-text="'确定'"
    :cancel-text="'取消'"
    @ok="handleCategoryConfirm"
    destroyOnClose
  >
    <a-form layout="vertical">
      <a-form-item label="选择分类">
        <a-select
          v-model:value="categoryValue"
          placeholder="选择或输入分类"
          show-search
          @search="categorySearchText = $event"
        >
          <a-select-option v-for="cat in filteredCategories" :key="cat" :value="cat">
            {{ cat }}
          </a-select-option>
          <template #dropdownRender="{ menuNode: menu }">
            <VNodes :vnodes="menu" />
            <a-divider style="margin: 4px 0" />
            <a-space style="padding: 4px 8px">
              <a-input v-model:value="categoryNewName" style="width: 260px" placeholder="请输入新分类" />
              <a-button type="text" @click="addCategory">
                <template #icon>
                  <PlusOutlined />
                </template>
                添加
              </a-button>
            </a-space>
          </template>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<style scoped lang="scss">
.skill-card {
  min-height: 180px;
  padding: var(--spacing-md);
  background-color: var(--color-bg-white);
  border-radius: var(--border-radius-lg);
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);
  transition: all var(--transition-base);
  display: flex;
  flex-direction: column;
  gap: var(--spacing-sm);

  &:hover {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
    transform: translateY(-2px);
  }

  .card-header {
    .card-avatar {
      width: 40px;
      height: 40px;
      background-color: #f3e5f5;
      color: #ab47bc;
      border-radius: var(--border-radius-xl);
      font-size: var(--font-size-2xl);
      font-weight: 600;
      flex-shrink: 0;
    }

    .card-name {
      font-size: var(--font-size-base);
      font-weight: 600;
      color: var(--color-text-primary);
      cursor: pointer;
      transition: color var(--transition-base);

      //&:hover {
      //  color: #ab47bc;
      //}
    }
  }

  .card-content {
    font-size: var(--font-size-sm);
    color: var(--color-text-regular);
    line-height: 1.6;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
    overflow: hidden;
    text-overflow: ellipsis;
    word-break: break-all;
    min-height: 65px;
    max-height: 65px;
  }

  .card-footer {
    padding-top: var(--spacing-xs);

    .card-tags {
      flex-wrap: wrap;
    }

    .card-time {
      white-space: nowrap;
    }
  }

  .disabled {
    color: #757575 !important;
    background-color: #e7e7e7 !important;
  }
}
</style>
