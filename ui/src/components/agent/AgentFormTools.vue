/**
 * 智能体工具与能力表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import Sortable from 'sortablejs'
import { HolderOutlined } from '@ant-design/icons-vue'
import { RoutePaths } from '@/router/constants.ts'
import * as toolApi from '@/api/tool'
import * as skillApi from '@/api/skill'
import * as sensitiveApi from '@/api/sensitive'
import * as hookApi from '@/api/hook'
import type { ToolVO, SkillPackageVO, SensitiveWordConfigVO, HookConfigVO } from '@/types'
import { ToolChoiceStrategy } from '@/types'
import { countCommonElements } from '@/utils/tools'

/**
 * Props定义
 */
const props = defineProps<{
  modelValue: {
    hook: string[]
    toolChoiceStrategy: ToolChoiceStrategy
    tool: string[]
    specificToolName: string
    skill: string[]
    sensitiveWordConfigId: string
    sensitiveFilterEnabled: boolean
  }
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: typeof props.modelValue]
}>()

const formRef = ref()
const loading = ref(false)

const toolCategories = ref<string[]>([])
const allTools = ref<ToolVO[]>([])
const allHooks = ref<HookConfigVO[]>([])
const skillCategories = ref<string[]>([])
const allSkills = ref<SkillPackageVO[]>([])
const sensitiveCategories = ref<string[]>([])
const allSensitives = ref<SensitiveWordConfigVO[]>([])

/**
 * 表单数据
 */
const formData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

/**
 * 工具选择策略选项
 */
const strategyOptions = [
  { label: '自动', value: ToolChoiceStrategy.AUTO },
  { label: '不使用', value: ToolChoiceStrategy.NONE },
  { label: '必须使用', value: ToolChoiceStrategy.REQUIRED },
  { label: '指定工具', value: ToolChoiceStrategy.SPECIFIC }
]

/**
 * 按分类分组的工具
 */
const toolsByCategory = computed(() => {
  const groups: Record<string, ToolVO[]> = {}
  toolCategories.value.forEach(cat => {
    groups[cat] = allTools.value.filter(t => t.category === cat)
  })
  return groups
})

/**
 * 按类型分组的钩子（内置、自定义）
 */
const hooksByType = computed(() => {
  const builtin = allHooks.value.filter(h => h.hookType === 'BUILTIN')
  const custom = allHooks.value.filter(h => h.hookType === 'CUSTOM')
  return { '内置': builtin, '自定义': custom } as Record<string, HookConfigVO[]>
})

/**
 * 按分类分组的技能包
 */
const skillsByCategory = computed(() => {
  const groups: Record<string, SkillPackageVO[]> = {}
  skillCategories.value.forEach(cat => {
    groups[cat] = allSkills.value.filter(s => s.category === cat)
  })
  return groups
})

/**
 * 按分类分组的敏感词
 */
const sensitivesByCategory = computed(() => {
  const groups: Record<string, SensitiveWordConfigVO[]> = {}
  sensitiveCategories.value.forEach(cat => {
    groups[cat] = allSensitives.value.filter(s => s.category === cat)
  })
  return groups
})

/**
 * 已选择的工具列表
 */
const selectedTools = computed(() => {
  return allTools.value.filter(t => formData.value.tool.includes(t.id))
})

/**
 * 已选择的钩子列表（保持选择顺序，用于拖拽排序展示）
 */
const selectedHooks = computed(() => {
  const idToHook = new Map(allHooks.value.map(h => [h.id, h]))
  return formData.value.hook
    .map(id => idToHook.get(id))
    .filter((h): h is HookConfigVO => !!h)
})

const selectedHooksListRef = ref<HTMLElement | null>(null)
let sortableInstance: ReturnType<typeof Sortable.create> | null = null

/**
 * 初始化钩子列表拖拽排序
 */
function initHookSortable() {
  if (!selectedHooksListRef.value || selectedHooks.value.length === 0) return
  if (sortableInstance) return

  sortableInstance = Sortable.create(selectedHooksListRef.value, {
    animation: 150,
    handle: '.hook-drag-handle',
    ghostClass: 'hook-sortable-ghost',
    chosenClass: 'hook-sortable-chosen',
    dragClass: 'hook-sortable-drag',
    forceFallback: false,
    fallbackClass: 'hook-sortable-fallback',
    preventOnFilter: false,
    onStart: () => {
      document.body.classList.add('dragging')
    },
    onEnd: async (evt: { oldIndex?: number; newIndex?: number }) => {
      document.body.classList.remove('dragging')
      const { oldIndex, newIndex } = evt
      if (oldIndex == null || newIndex == null || oldIndex === newIndex) return
      const newOrder = [...formData.value.hook]
      const [item] = newOrder.splice(oldIndex, 1)
      if (item === undefined) return
      newOrder.splice(newIndex, 0, item)
      formData.value = { ...formData.value, hook: newOrder }
      // 拖拽后销毁并重新初始化 Sortable，确保 DOM 与 Vue 数据同步
      destroyHookSortable()
      await nextTick()
      initHookSortable()
    }
  })
}

/**
 * 销毁拖拽实例
 */
function destroyHookSortable() {
  if (sortableInstance) {
    sortableInstance.destroy()
    sortableInstance = null
  }
}

/**
 * 是否显示特定工具选择
 */
const showSpecificTool = computed(() => {
  return formData.value.toolChoiceStrategy === ToolChoiceStrategy.SPECIFIC
})

/**
 * 加载工具分类
 */
async function loadToolCategories() {
  const response = await toolApi.listCategories()
  toolCategories.value = response.data.data || []
}

/**
 * 加载所有工具
 */
async function loadAllTools() {
  try {
    loading.value = true
    const response = await toolApi.page({ page: 1, size: 1000, enabled: true })
    allTools.value = response.data.data.records || []
  } finally {
    loading.value = false
  }
}

/**
 * 加载技能包分类
 */
async function loadSkillCategories() {
  const response = await skillApi.listCategories()
  skillCategories.value = response.data.data || []
}

/**
 * 加载所有技能包
 */
async function loadAllSkills() {
  const response = await skillApi.page({ page: 1, size: 1000, enabled: true })
  allSkills.value = response.data.data.records || []
}

/**
 * 加载敏感词分类
 */
async function loadSensitiveCategories() {
  const response = await sensitiveApi.listCategories()
  sensitiveCategories.value = response.data.data || []
}

/**
 * 加载所有敏感词
 */
async function loadAllSensitives() {
  const response = await sensitiveApi.page({ page: 1, size: 1000, enabled: true })
  allSensitives.value = response.data.data.records || []
}

/**
 * 加载所有钩子
 */
async function loadAllHooks() {
  const response = await hookApi.page({ page: 1, size: 1000, enabled: true })
  allHooks.value = response.data.data.records || []
}

/**
 * 验证表单
 */
async function validate(): Promise<boolean> {
  try {
    await formRef.value?.validate()
    return true
  } catch {
    return false
  }
}

const handleToolChange = (toolId: string, checked: boolean) => {
  if (checked) {
    formData.value.tool.push(toolId);
  } else {
    const index = formData.value.tool.indexOf(toolId);
    if (index > -1) {
      formData.value.tool.splice(index, 1);
    }
  }
};

const handleSkillChange = (skillId: string, checked: boolean) => {
  if (checked) {
    formData.value.skill.push(skillId);
  } else {
    const index = formData.value.skill.indexOf(skillId);
    if (index > -1) {
      formData.value.skill.splice(index, 1);
    }
  }
};

const handleSensitiveChange = (sensitiveId: string, checked: boolean) => {
  if (checked) {
    formData.value.sensitiveWordConfigId = sensitiveId;
    formData.value.sensitiveFilterEnabled = true;
  } else {
    formData.value.sensitiveWordConfigId = '-1';
    formData.value.sensitiveFilterEnabled = false;
  }
};

const handleHookChange = (hookId: string, checked: boolean) => {
  if (checked) {
    formData.value.hook.push(hookId);
  } else {
    const index = formData.value.hook.indexOf(hookId);
    if (index > -1) {
      formData.value.hook.splice(index, 1);
    }
  }
};

watch(
  () => selectedHooks.value.length,
  async (len) => {
    destroyHookSortable()
    if (len > 0) {
      await nextTick()
      initHookSortable()
    }
  }
)

onMounted(async () => {
  loadToolCategories()
  loadAllTools()
  loadAllHooks()
  loadSkillCategories()
  loadAllSkills()
  loadSensitiveCategories()
  loadAllSensitives()
  if (selectedHooks.value.length > 0) {
    await nextTick()
    initHookSortable()
  }
})

onUnmounted(() => {
  destroyHookSortable()
})

defineExpose({
  validate
})
</script>

<template>
  <ASpin :spinning="loading">
    <AForm ref="formRef" :model="formData" layout="vertical">
      <AFormItem label="钩子配置">
        <ACollapse>
          <ACollapsePanel
            v-for="(hooks, typeLabel) in hooksByType"
            :key="typeLabel"
            :header="`${typeLabel}（${countCommonElements(hooks.map(i => i.id), formData.hook)}/${hooks.length}）`">
            <div class="checkbox-grid">
              <ACheckbox
                v-for="hook in hooks"
                :checked="formData.hook.includes(hook.id)"
                @change="(e: any) => handleHookChange(hook.id, e.target.checked)"
                :key="hook.id"
                :value="hook.id"
                class="checkbox-item"
              >
                <div class="item-info">
                  <div class="item-name text-ellipsis" :title="hook.name">{{ hook.name }}</div>
                  <div class="item-desc text-placeholder text-xs text-ellipsis" :title="hook.description">{{ hook.description }}</div>
                </div>
              </ACheckbox>
            </div>
          </ACollapsePanel>
        </ACollapse>

        <div v-if="selectedHooks.length > 0" class="selected-hooks-section mt-md">
          <div class="selected-hooks-label text-secondary text-sm mb-sm">
            已选钩子（共 {{ selectedHooks.length }} 个，拖拽调整执行顺序）
          </div>
          <div ref="selectedHooksListRef" class="selected-hooks-list">
            <div
              v-for="hook in selectedHooks"
              :key="hook.id"
              class="selected-hook-item"
            >
              <span class="hook-drag-handle" title="拖拽排序">
                <HolderOutlined class="hook-drag-handle-icon" />
              </span>
              <div class="selected-hook-info">
                <span class="selected-hook-name" :title="hook.name">{{ hook.name }}</span>
                <span class="selected-hook-type" :class="hook.hookType === 'BUILTIN' ? 'type-builtin' : 'type-custom'">
                  {{ hook.hookType === 'BUILTIN' ? '内置' : '自定义' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </AFormItem>

      <AFormItem label="工具集">
        <ACollapse v-if="toolCategories?.length > 0">
          <ACollapsePanel
            v-for="category in toolCategories"
            :key="category"
            :header="`${category}（${countCommonElements(toolsByCategory[category]?.map(i => i.id) || [], formData.tool)}/${toolsByCategory[category]?.length}）`">
            <div class="checkbox-grid">
              <ACheckbox
                v-for="tool in toolsByCategory[category]"
                :checked="formData.tool.includes(tool.id)"
                @change="(e: any) => handleToolChange(tool.id, e.target.checked)"
                :key="tool.id"
                :value="tool.id"
                class="checkbox-item"
              >
                <div class="item-info">
                  <div class="item-name text-ellipsis" :title="tool.name">{{ tool.name }}</div>
                  <div class="item-desc text-placeholder text-xs text-ellipsis" :title="tool.description">{{ tool.description }}</div>
                </div>
              </ACheckbox>
            </div>
          </ACollapsePanel>
        </ACollapse>
        <div v-else class="text-placeholder mt-xs">
          <AButton type="text">未配置工具？</AButton>
          <AButton type="link" :href="`/#/${RoutePaths.TOOL}`" target="_blank">去配置</AButton>
          <AButton type="link" @click="loadToolCategories();loadAllTools()">刷新</AButton>
        </div>
      </AFormItem>
      <AFormItem label="工具选择策略" v-if="formData.tool?.length > 0">
        <ARadioGroup v-model:value="formData.toolChoiceStrategy" :options="strategyOptions" />
      </AFormItem>

      <AFormItem v-if="showSpecificTool" label="指定工具">
        <ASelect v-model:value="formData.specificToolName" placeholder="选择一个工具">
          <ASelectOption v-for="tool in selectedTools" :key="tool.toolId" :value="tool.toolId">
            {{ tool.name }}
          </ASelectOption>
        </ASelect>
        <div class="text-placeholder text-xs mt-xs">
          从已选择的工具中指定一个工具作为默认工具
        </div>
      </AFormItem>

      <AFormItem label="技能包">
        <ACollapse v-if="skillCategories?.length > 0">
          <ACollapsePanel
            v-for="category in skillCategories"
            :key="category"
            :header="`${category}（${countCommonElements(skillsByCategory[category]?.map(i => i.id) || [], formData.skill)}/${skillsByCategory[category]?.length}）`">
            <div class="checkbox-grid">
              <ACheckbox
                v-for="skill in skillsByCategory[category]"
                :checked="formData.skill.includes(skill.id)"
                @change="(e: any) => handleSkillChange(skill.id, e.target.checked)"
                :key="skill.id"
                :value="skill.id"
                class="checkbox-item"
              >
                <div class="item-info">
                  <div class="item-name text-ellipsis" :title="skill.name">{{ skill.name }}</div>
                  <div class="item-desc text-placeholder text-xs text-ellipsis" :title="skill.description">{{ skill.description }}</div>
                </div>
              </ACheckbox>
            </div>
          </ACollapsePanel>
        </ACollapse>
        <div v-else class="text-placeholder mt-xs">
          <AButton type="text">未配置技能包？</AButton>
          <AButton type="link" :href="`/#/${RoutePaths.SKILL}`" target="_blank">去配置</AButton>
          <AButton type="link" @click="loadSkillCategories();loadAllSkills()">刷新</AButton>
        </div>
      </AFormItem>

      <AFormItem label="敏感词配置">
        <ACollapse v-if="sensitiveCategories?.length > 0">
          <ACollapsePanel
            v-for="category in sensitiveCategories"
            :key="category"
            :header="`${category}（${countCommonElements(sensitivesByCategory[category]?.map(i => i.id) || [], [formData.sensitiveWordConfigId])}/${sensitivesByCategory[category]?.length}）`">
            <div class="checkbox-grid">
              <ACheckbox
                v-for="sensitive in sensitivesByCategory[category]"
                :checked="formData.sensitiveWordConfigId === sensitive.id"
                @change="(e: any) => handleSensitiveChange(sensitive.id, e.target.checked)"
                :key="sensitive.id"
                :value="sensitive.id"
                class="checkbox-item"
              >
                <div class="item-info">
                  <div class="item-name text-ellipsis" :title="sensitive.name">{{ sensitive.name }}</div>
                  <div class="item-desc text-placeholder text-xs text-ellipsis" :title="sensitive.description">{{ sensitive.description }}</div>
                </div>
              </ACheckbox>
            </div>
          </ACollapsePanel>
        </ACollapse>
        <div v-else class="text-placeholder mt-xs">
          <AButton type="text">未配置敏感词？</AButton>
          <AButton type="link" :href="`/#/${RoutePaths.SENSITIVE}`" target="_blank">去配置</AButton>
          <AButton type="link" @click="loadSensitiveCategories();loadAllSensitives()">刷新</AButton>
        </div>
      </AFormItem>

      <AFormItem label="启用敏感词过滤" v-if="formData.sensitiveWordConfigId && formData.sensitiveWordConfigId !== '-1'">
        <ASwitch v-model:checked="formData.sensitiveFilterEnabled" />
        <div class="text-placeholder text-xs mt-xs">
          开启后,将对输入和输出进行敏感词过滤
        </div>
      </AFormItem>
    </AForm>
  </ASpin>
</template>

<style scoped lang="scss">
.checkbox-grid,
.radio-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--spacing-sm);
}

.checkbox-item,
.radio-item {
  padding: var(--spacing-sm);
  border: 1px solid var(--color-border-base);
  border-radius: var(--border-radius-md);
  margin: 0 !important;
  transition: all var(--transition-base);
  width: 300px;

  &:hover {
    border-color: var(--color-primary);
    background-color: var(--color-bg-light);
  }
}

.item-info {
  .item-name {
    font-weight: 500;
    margin-bottom: 4px;
    width:250px;
  }

  .item-desc {
    line-height: 1.4;
  }
}

.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  width:250px;
}

.selected-hooks-section {
  padding: var(--spacing-md);
  background-color: var(--color-bg-light);
  border-radius: var(--border-radius-md);
  border: 1px solid var(--color-border-base);
}

.selected-hooks-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-xs);
}

.selected-hook-item {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--color-bg-white);
  border: 1px solid var(--color-border-base);
  border-radius: var(--border-radius-md);
  transition: all var(--transition-base);
  cursor: default;

  &:hover {
    border-color: var(--color-primary);
    background-color: var(--color-bg-light);
  }
}

.hook-drag-handle {
  display: flex;
  align-items: center;
  cursor: grab;
  padding: 2px;

  &:active {
    cursor: grabbing;
  }

  .hook-drag-handle-icon {
    color: #999;
    font-size: 16px;

    &:hover {
      color: #666;
    }
  }
}

.selected-hook-info {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.selected-hook-name {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-hook-type {
  flex-shrink: 0;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: var(--border-radius-sm);

  &.type-builtin {
    background-color: #e3f2fd;
    color: #1976d2;
  }

  &.type-custom {
    background-color: #fde6f6;
    color: #db2781;
  }
}

:deep(.hook-sortable-ghost) {
  opacity: 0.5;
  background-color: #f0f0f0;
  border: 1px dashed #1890ff;
}

:deep(.hook-sortable-chosen) {
  background-color: #fafafa;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

:deep(.hook-sortable-drag) {
  opacity: 0.9;
  transform: rotate(2deg);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
}

:deep(.hook-sortable-fallback) {
  opacity: 1 !important;
}

/* 防止拖拽时页面滚动 */
:global(body.dragging) {
  user-select: none;
  -webkit-user-select: none;
}
</style>
