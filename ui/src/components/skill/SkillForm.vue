/**
 * 技能包表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, watch, defineComponent, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { AppstoreOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import SmartCodeEditor from '@/components/editor/SmartCodeEditor.vue'
import ResourceEditor from './ResourceEditor.vue'
import type { SkillPackage, SkillPackageVO, ToolVO } from '@/types'
import * as skillApi from '@/api/skill'
import * as toolApi from '@/api/tool'
import { RoutePaths } from '@/router/constants.ts'

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
  visible: boolean
  data?: SkillPackageVO
  categories: string[]
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const formRef = ref()
const form = ref<{
  used?: string[]
  name: string
  category: string
  description: string
  skillContent: string
  references: ResourceItem[]
  examples: ResourceItem[]
  scripts: ResourceItem[]
  tools: string[]
}>({
  name: '',
  category: '',
  description: '',
  skillContent: '',
  references: [],
  examples: [],
  scripts: [],
  tools: []
})

const loading = ref<boolean>(false)
const toolsLoading = ref<boolean>(false)
const referencesEditorRef = ref()
const examplesEditorRef = ref()
const scriptsEditorRef = ref()

// 工具相关数据
const toolCategories = ref<string[]>([])
const allTools = ref<ToolVO[]>([])
// 左右两侧独立搜索
const leftSearchText = ref<string>('')
const rightSearchText = ref<string>('')

/**
 * Transfer 穿梭框数据源
 */
const transferDataSource = computed(() => {
  return allTools.value.map(tool => ({
    key: String(tool.id),
    title: tool.name,
    description: tool.description,
    category: tool.category
  }))
})

/**
 * 根据搜索过滤后的 Transfer 数据源（左侧可选列表，排除已选工具）
 */
const filteredLeftDataSource = computed(() => {
  const unselected = transferDataSource.value.filter(t => !form.value.tools.includes(t.key))
  if (!leftSearchText.value) {
    return unselected
  }
  const searchLower = leftSearchText.value.toLowerCase()
  return unselected.filter(item =>
    item.title.toLowerCase().includes(searchLower) ||
    item.description?.toLowerCase().includes(searchLower) ||
    item.category?.toLowerCase().includes(searchLower)
  )
})

/**
 * 已选工具列表（右侧，支持搜索过滤）
 */
const filteredRightDataSource = computed(() => {
  const selected = transferDataSource.value.filter(t => form.value.tools.includes(t.key))
  if (!rightSearchText.value) {
    return selected
  }
  const searchLower = rightSearchText.value.toLowerCase()
  return selected.filter(item =>
    item.title.toLowerCase().includes(searchLower) ||
    item.description?.toLowerCase().includes(searchLower) ||
    item.category?.toLowerCase().includes(searchLower)
  )
})


/**
 * 表单校验规则
 */
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [
    { required: true, message: '请输入标签', trigger: 'blur' },
    { max: 6, message: '标签长度不能超过6个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入描述', trigger: 'blur' }
  ],
  skillContent: [{ required: true, message: '请输入技能内容', trigger: 'blur' }]
}

const inputRef = ref();
const name = ref();
const categorySearchText = ref<string>('')

const filteredCategories = computed(() => {
  if (!categorySearchText.value) {
    return props.categories
  }
  const searchLower = categorySearchText.value.toLowerCase()
  const filtered = props.categories.filter(cat =>
    cat.toLowerCase().includes(searchLower)
  )
  if (!filtered.includes(categorySearchText.value)) {
    filtered.unshift(categorySearchText.value)
  }
  return filtered
})

/**
 * 是否为编辑模式
 */
const isEdit = computed(() => !!props.data?.id)

/**
 * 弹窗标题
 */
const title = computed(() => isEdit.value ? '编辑技能包' : '新增技能包')

/**
 * 统计数量
 */
const referencesCount = computed(() => form.value.references?.length || 0)
const examplesCount = computed(() => form.value.examples?.length || 0)
const scriptsCount = computed(() => form.value.scripts?.length || 0)

/**
 * 初始化表单数据
 */
function initForm() {
  if (props.data) {
    form.value = {
      used: props.data.used,
      name: props.data.name || '',
      category: props.data.category || '',
      description: props.data.description || '',
      skillContent: props.data.skillContent || '',
      references: props.data.references || [],
      examples: props.data.examples || [],
      scripts: props.data.scripts || [],
      tools: (props.data.tools || []).map(String)
    }
  } else {
    form.value = {
      name: '',
      category: '',
      description: '',
      skillContent: '',
      references: [],
      examples: [],
      scripts: [],
      tools: []
    }
  }
}

/**
 * 打开资源编辑器
 */
function openReferencesEditor() {
  referencesEditorRef.value?.open()
}

function openExamplesEditor() {
  examplesEditorRef.value?.open()
}

function openScriptsEditor() {
  scriptsEditorRef.value?.open()
}

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
    toolsLoading.value = true
    const response = await toolApi.page({ page: 1, size: 1000, enabled: true })
    allTools.value = response.data.data.records || []
  } finally {
    toolsLoading.value = false
  }
}

/**
 * Transfer 穿梭框选择变化
 */
function handleTransferChange(targetKeys: string[]) {
  form.value.tools = targetKeys
}

/**
 * Transfer 渲染项
 */
function transferRender(item: TransferItem) {
  return item.title
}

/**
 * Transfer 搜索过滤
 */
function transferFilterOption(inputValue: string, option: TransferItem) {
  const searchLower = inputValue.toLowerCase()
  return (
    option.title.toLowerCase().includes(searchLower) ||
    option.description?.toLowerCase().includes(searchLower) ||
    option.category?.toLowerCase().includes(searchLower)
  )
}

/**
 * Transfer 搜索事件处理（左右独立搜索）
 */
function handleTransferSearch(direction: 'left' | 'right', value: string) {
  if (direction === 'left') {
    leftSearchText.value = value
  } else {
    rightSearchText.value = value
  }
}

/**
 * 切换工具选择状态（点击即移动）
 * @param direction 当前方向
 * @param key 工具key
 */
function handleToolClick(direction: 'left' | 'right', key: string) {
  if (direction === 'left') {
    // 左侧点击：添加到已选
    if (!form.value.tools.includes(key)) {
      form.value.tools.push(key)
    }
  } else {
    // 右侧点击：从已选移除
    const index = form.value.tools.indexOf(key)
    if (index > -1) {
      form.value.tools.splice(index, 1)
    }
  }
}

/**
 * Transfer 数据项类型
 */
interface TransferItem {
  key: string
  title: string
  description?: string
  category?: string
}

/**
 * 提交表单
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()

    loading.value = true

    const entity: SkillPackage = {
      name: form.value.name,
      category: form.value.category || '',
      description: form.value.description,
      skillContent: form.value.skillContent,
      references: form.value.references.length > 0 ? form.value.references : null,
      examples: form.value.examples.length > 0 ? form.value.examples : null,
      scripts: form.value.scripts.length > 0 ? form.value.scripts : null,
      tools: form.value.tools
    } as SkillPackage

    if (isEdit.value && props.data) {
      entity.id = String(props.data.id)
      await skillApi.update(entity)
      message.success('修改成功')
    } else {
      await skillApi.save(entity)
      message.success('新增成功')
    }

    emit('update:visible', false)
    emit('success')
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 取消
 */
function handleCancel() {
  emit('update:visible', false)
}

const VNodes = defineComponent({
  props: {
    vnodes: {
      type: Object,
      required: true,
    },
  },
  render() {
    return this.vnodes;
  },
});

const addItem = (e: Event) => {
  e.preventDefault();

  if (!name.value)  return
  if (!filteredCategories.value.includes(name.value)) {
    filteredCategories.value.push(name.value);
  }

  form.value.category = name.value
  name.value = '';
};

/**
 * 监听弹窗显示状态
 */
watch(() => props.visible, (val) => {
  if (val) {
    initForm()
  } else {
    formRef.value?.resetFields()
  }
}, { immediate: true })

onMounted(() => {
  loadToolCategories()
  loadAllTools()
})
</script>

<template>
  <ApboaModal
    :open="visible"
    :title-icon="AppstoreOutlined"
    :title="title"
    :confirm-loading="loading"
    defaultWidth="800px"
    destroyOnClose
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <AForm
      ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
    >
      <AFormItem label="关联智能体" v-if="isEdit">
        <div class="code-wrapper ">
          {{ form?.used?.join('、') || '无' }}
        </div>
      </AFormItem>
      <AFormItem label="标签" name="category">
        <ASelect
          v-model:value="form.category"
          placeholder="选择或输入标签"
        >
          <ASelectOption v-for="cat in filteredCategories" :key="cat" :value="cat">
            {{ cat }}
          </ASelectOption>
          <template #dropdownRender="{ menuNode: menu }">
            <VNodes :vnodes="menu" />
            <ADivider style="margin: 4px 0" />
            <ASpace style="padding: 4px 8px">
              <AInput ref="inputRef" v-model:value="name" style="width: 300px" placeholder="请输入" />
              <AButton type="text" @click="addItem">
                <template #icon>
                  <PlusOutlined />
                </template>
                添加
              </AButton>
            </ASpace>
          </template>
        </ASelect>
      </AFormItem>

      <AFormItem label="名称" name="name">
        <AInput v-model:value="form.name" placeholder="请输入技能包名称" />
      </AFormItem>

      <AFormItem label="描述" name="description">
        <ATextarea
          v-model:value="form.description"
          placeholder="请输入技能包描述"
          :rows="3"
        />
      </AFormItem>

      <AFormItem label="技能内容" name="skillContent">
        <SmartCodeEditor
          v-if="visible"
          v-model="form.skillContent"
          language="markdown"
          height="300px"
        />
      </AFormItem>
      <ARow :gutter="16">
        <ACol :span="8">
          <AFormItem label="参考资料">
            <div class="flex items-center gap-sm">
              <span class="text-secondary" :class="{'text-success': referencesCount > 0}">{{ referencesCount }} 项</span>
              <AButton type="text" @click="openReferencesEditor">编辑</AButton>
            </div>
          </AFormItem>
        </ACol>
        <ACol :span="8">
          <AFormItem label="示例代码">
            <div class="flex items-center gap-sm">
              <span class="text-secondary" :class="{'text-success': examplesCount > 0}">{{ examplesCount }} 项</span>
              <AButton type="text" @click="openExamplesEditor">编辑</AButton>
            </div>
          </AFormItem>
        </ACol>
        <ACol :span="8">
          <AFormItem label="执行脚本">
            <div class="flex items-center gap-sm">
              <span class="text-secondary" :class="{'text-success': scriptsCount > 0}">{{ scriptsCount }} 项</span>
              <AButton type="text" @click="openScriptsEditor">编辑</AButton>
            </div>
          </AFormItem>
        </ACol>
      </ARow>

      <!-- 关联工具选择器 -->
      <AFormItem>
        <template #label>
          <div class="flex items-center gap-xs">

            <ATooltip title="避免预先注册所有 Tool，仅在 Skill 被 LLM 使用时才传递相关 Tool。">
              <span>关联工具</span><InfoCircleOutlined class="text-secondary cursor-pointer" />
            </ATooltip>
          </div>
        </template>
        <ASpin :spinning="toolsLoading">
          <div v-if="allTools.length === 0" class="empty-tools">
            <span class="text-secondary">暂无可用工具</span>
            <AButton type="link" :href="`/#/${RoutePaths.TOOL}`" target="_blank">去配置</AButton>
            <AButton type="link" @click="loadToolCategories();loadAllTools()">刷新</AButton>
          </div>
          <ATransfer
            v-else
            v-model:target-keys="form.tools"
            :data-source="transferDataSource"
            :titles="['可选工具', '已选工具']"
            :render="transferRender"
            :list-style="{ width: '280px', height: '360px' }"
            show-search
            :filter-option="transferFilterOption"
            :search-placeholder="'搜索工具名称、描述或分类'"
            @change="handleTransferChange"
            @search="handleTransferSearch"
          >
            <template #children="{ direction }">
              <template v-if="direction === 'left'">
                <div class="transfer-tools-list">
                  <div
                    v-for="item in filteredLeftDataSource"
                    :key="item.key"
                    class="transfer-tool-item"
                    @click="handleToolClick('left', item.key)"
                  >
                    <div class="tool-item-header">
                      <span class="tool-item-name" :title="item.title">{{ item.title }}</span>
                      <ATag :bordered="false" size="small" color="blue" class="tool-item-tag">{{ item.category }}</ATag>
                    </div>
                    <div class="tool-item-desc" :title="item.description || '暂无描述'">{{ item.description || '暂无描述' }}</div>
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="transfer-tools-list">
                  <div
                    v-for="item in filteredRightDataSource"
                    :key="item.key"
                    class="transfer-tool-item transfer-tool-item-selected"
                    @click="handleToolClick('right', item.key)"
                  >
                    <div class="tool-item-header">
                      <span class="tool-item-name" :title="item.title">{{ item.title }}</span>
                      <ATag :bordered="false" size="small" color="blue" class="tool-item-tag">{{ item.category }}</ATag>
                    </div>
                    <div class="tool-item-desc" :title="item.description || '暂无描述'">{{ item.description || '暂无描述' }}</div>
                  </div>
                </div>
              </template>
            </template>
          </ATransfer>
        </ASpin>
      </AFormItem>
    </AForm>

    <ResourceEditor
      ref="referencesEditorRef"
      v-model="form.references"
      title="编辑参考资料"
      prefix="references"
      :show-language-selector="false"
    />

    <ResourceEditor
      ref="examplesEditorRef"
      v-model="form.examples"
      title="编辑示例代码"
      prefix="examples"
      :show-language-selector="false"
    />

    <ResourceEditor
      ref="scriptsEditorRef"
      v-model="form.scripts"
      title="编辑执行脚本"
      prefix="scripts"
      :show-language-selector="false"
    />
  </ApboaModal>
</template>

<style scoped lang="scss">
.selected-tools-section {
  padding: var(--spacing-sm) var(--spacing-md);
  background-color: var(--color-bg-light);
  border-radius: var(--border-radius-md);
  border: 1px solid var(--color-border-base);
}

.selected-tools-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-sm);
}

.selected-tools-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-xs);
}

.empty-tools {
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
  padding: var(--spacing-lg);
  background-color: var(--color-bg-light);
  border-radius: var(--border-radius-md);
}

.transfer-tools-list {
  height: 260px;
  overflow-y: auto;
  padding: var(--spacing-xs);
}

.transfer-tool-item {
  padding: var(--spacing-sm);
  border-radius: var(--border-radius-sm);
  cursor: pointer;
  transition: all var(--transition-base);

  &:hover {
    background-color: var(--color-bg-light);
  }
}

.transfer-tool-item-selected {
  background-color: var(--color-primary-bg);
}

.tool-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 4px;
  gap: var(--spacing-xs);
}

.tool-item-name {
  flex: 1;
  min-width: 0;
  font-weight: 500;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-item-tag {
  flex-shrink: 0;
}

.tool-item-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
