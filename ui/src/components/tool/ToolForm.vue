/**
 * 工具表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch, computed, defineComponent, nextTick, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  MinusCircleOutlined,
  HolderOutlined,
  ToolOutlined,
  InfoCircleOutlined
} from '@ant-design/icons-vue'
import Sortable from 'sortablejs'
import type { ToolVO, ToolConfig } from '@/types'
import { ToolType, CodeLanguage } from '@/types'
import * as toolApi from '@/api/tool'
import SmartCodeEditor from '@/components/editor/SmartCodeEditor.vue'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  data?: ToolVO
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
const loading = ref<boolean>(false)
const categorySearchText = ref<string>('')

/**
 * 输入参数schema项定义
 */
interface InputSchemaItem {
  name: string
  description: string
  type: string
  defaultValue: string
  required: boolean
  // 添加一个临时ID用于拖拽识别
  _tempId?: number
}

// 用于生成临时ID的计数器
let tempIdCounter = 0

const formData = ref<{
  used?: string[]
  category: string
  name: string
  toolId: string
  description: string
  needConfirm: boolean
  version: string
  inputSchema: InputSchemaItem[]
  code: string
}>({
  category: '',
  name: '',
  toolId: '',
  description: '',
  needConfirm: false,
  version: '1.0.0',
  inputSchema: [],
  code: ''
})

const inputRef = ref()
const name = ref()
const inputSchemaListRef = ref<HTMLElement | null>(null)
let sortableInstance: ReturnType<typeof Sortable.create> | null = null

const isEdit = computed(() => !!props.data?.id)
const isBuiltin = computed(() => props.data?.toolType === 'BUILTIN')

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

const typeOptions = [
  { label: '字符串', value: 'string' },
  { label: '整数', value: 'integer' },
  { label: '数字', value: 'number' },
  { label: '布尔', value: 'boolean' },
  { label: '对象', value: 'object' }
]

const codeTemplate = `import java.util.*;
import com.hxh.apboa.core.tool.dynamices.IDynamicAgentTool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 内置方法:
 * 1. 将对象转成字符串
 * String toJsonString(Object obj)
 *
 * 2. 将字符串转成 JsonNode
 * JsonNode parse(String string)
 *
 * 3. 将字符串转成自定义类型
 * <T> T parse(String string, Class<T> clazz)
 *
 *
 * 支持使用 @Autowired 载入 Springboot 管理的 Bean
 **/
@Component
public class CustomTool implements IDynamicAgentTool {

    @Override
    public Object execute(Object... args) {
        // 返回结果Map
        Map<String, Object> resMap = new HashMap<>();

        // TODO 处理参数

        // TODO 执行逻辑

        // 返回参数
        return resMap;
    }
}`

// 为每个输入参数添加临时ID
function addTempIdToSchema(schema: InputSchemaItem[]): InputSchemaItem[] {
  return schema.map(item => ({
    ...item,
    _tempId: tempIdCounter++
  }))
}

watch(
  () => props.visible,
  async (newVal) => {
    if (newVal) {
      if (props.data) {
        const inputSchemaList = props.data.inputSchema || []
        formData.value = {
          used: props.data.used,
          category: props.data.category,
          name: props.data.name,
          toolId: props.data.toolId,
          description: props.data.description,
          needConfirm: props.data.needConfirm,
          version: props.data.version,
          inputSchema: Array.isArray(inputSchemaList)
            ? addTempIdToSchema(inputSchemaList.map((item: InputSchemaItem) => ({
                name: item.name || '',
                description: item.description || '',
                type: item.type || 'string',
                defaultValue: item.defaultValue || '',
                required: item.required || false
              })))
            : [],
          code: props.data.code || ''
        }
      } else {
        resetForm()
      }
      await nextTick()
      destroyInputSchemaSortable()
      initInputSchemaSortable()
    } else {
      destroyInputSchemaSortable()
    }
  }
)

watch(
  () => formData.value.inputSchema.length,
  async () => {
    if (props.visible && !isBuiltin.value) {
      await nextTick()
      destroyInputSchemaSortable()
      initInputSchemaSortable()
    }
  }
)

onUnmounted(() => {
  destroyInputSchemaSortable()
})

/**
 * 表单验证规则
 */
const rules = computed(() => {
  const baseRules: Record<string, Array<{ required?: boolean; message?: string; trigger?: string; max?: number; pattern?: RegExp }>> = {
    category: [
      { required: true, message: '请输入标签', trigger: 'blur' },
      { max: 6, message: '标签长度不能超过6个字符', trigger: 'blur' }
    ],
    name: [
      { required: true, message: '请输入名称', trigger: 'blur' },
      { max: 100, message: '名称长度不能超过100个字符', trigger: 'blur' }
    ],
    description: [
      { required: true, message: '请输入描述', trigger: 'blur' },
      { max: 200, message: '描述长度不能超过200个字符', trigger: 'blur' }
    ],
    version: [
      { required: true, message: '请输入版本号', trigger: 'blur' }
    ]
  }

  if (!isBuiltin.value) {
    baseRules.toolId = [
      { required: true, message: '请输入工具编号', trigger: 'blur' },
      { pattern: /^[a-z_]+$/, message: '编号只能使用小写字母和下划线', trigger: 'blur' }
    ]
    baseRules.code = [
      { required: true, message: '请输入工具代码', trigger: 'blur' }
    ]
  }

  return baseRules
})

/**
 * 重置表单
 */
function resetForm() {
  tempIdCounter = 0
  formData.value = {
    category: '',
    name: '',
    toolId: '',
    description: '',
    needConfirm: false,
    version: '1.0.0',
    inputSchema: [],
    code: codeTemplate
  }
  formRef.value?.resetFields()
}

/**
 * 添加输入参数
 */
function addInputParam() {
  formData.value.inputSchema.push({
    name: '',
    description: '',
    type: 'string',
    defaultValue: '',
    required: false,
    _tempId: tempIdCounter++
  })
}

/**
 * 删除输入参数（通过对象引用删除，避免拖拽后索引错位导致删除错误）
 */
function removeInputParam(param: InputSchemaItem) {
  const index = formData.value.inputSchema.indexOf(param)
  if (index >= 0) {
    formData.value.inputSchema.splice(index, 1)
  }
}

/**
 * 初始化输入参数拖拽排序
 */
function initInputSchemaSortable() {
  if (!inputSchemaListRef.value || formData.value.inputSchema.length === 0) return
  if (sortableInstance) return

  sortableInstance = Sortable.create(inputSchemaListRef.value, {
    animation: 150,
    handle: '.input-schema-drag-handle',
    ghostClass: 'input-schema-sortable-ghost',
    chosenClass: 'input-schema-sortable-chosen',
    dragClass: 'input-schema-sortable-drag',
    onStart: () => {
      // 拖拽开始时，禁用过渡动画，防止闪烁
      document.body.classList.add('dragging')
    },
    onEnd: async (evt: { oldIndex?: number; newIndex?: number }) => {
      document.body.classList.remove('dragging')

      const { oldIndex, newIndex } = evt
      if (oldIndex == null || newIndex == null || oldIndex === newIndex) return

      // 使用 Vue 的响应式数组方法
      const schema = [...formData.value.inputSchema]
      const [item] = schema.splice(oldIndex, 1)
      if (!item) return

      schema.splice(newIndex, 0, item)

      // 一次性更新整个数组，避免多次触发响应式更新
      formData.value.inputSchema = schema

      // 拖拽后销毁并重新初始化 Sortable，确保 DOM 与 Vue 数据同步
      // 避免快速拖拽时 Sortable 直接操作 DOM 与 Vue 响应式更新产生竞态
      destroyInputSchemaSortable()
      await nextTick()
      initInputSchemaSortable()
    },
    // 添加这个选项来提高拖拽性能
    forceFallback: false,
    // 使用原生HTML5拖拽
    fallbackClass: 'input-schema-sortable-fallback',
    // 防止拖拽时触发点击事件
    preventOnFilter: false
  })
}

/**
 * 销毁输入参数拖拽实例
 */
function destroyInputSchemaSortable() {
  if (sortableInstance) {
    sortableInstance.destroy()
    sortableInstance = null
  }
}

/**
 * 处理提交
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    loading.value = true

    // 提交前移除临时ID
    const inputSchema = formData.value.inputSchema.map(({ _tempId, ...item }) => {
      void _tempId // 解构仅用于排除该字段
      return item
    })

    const entity: ToolConfig = {
      category: formData.value.category || '',
      name: formData.value.name,
      toolId: formData.value.toolId,
      description: formData.value.description,
      toolType: isBuiltin.value ? ToolType.BUILTIN : ToolType.CUSTOM,
      needConfirm: formData.value.needConfirm,
      inputSchema: isBuiltin.value ? null : inputSchema || [],
      outputSchema: null,
      classPath: null,
      language: CodeLanguage.JAVA,
      code: isBuiltin.value ? null : formData.value.code,
      version: formData.value.version
    } as ToolConfig

    if (isEdit.value && props.data) {
      entity.id = props.data.id
      await toolApi.update(entity)
      message.success('更新成功')
    } else {
      await toolApi.save(entity)
      message.success('创建成功')
    }

    emit('success')
    handleCancel()
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 处理取消
 */
function handleCancel() {
  emit('update:visible', false)
  resetForm()
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

  formData.value.category = name.value
  name.value = '';
};

</script>

<template>
  <ApboaModal
    :open="visible"
    :title-icon="ToolOutlined"
    :title="isEdit ? '编辑工具' : '新增工具'"
    :confirm-loading="loading"
    destroyOnClose
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <AAlert
      message="说明：在线工具目前支持同步工具。异步或流式工具请自行在com.hxh.apboa.core.tool.builtins包下进行编写"
      closable
      style="margin-bottom: 20px"
    />
    <AForm ref="formRef" :model="formData" :rules="rules" layout="vertical">
      <AFormItem label="关联智能体" v-if="isEdit">
        <div class="code-wrapper ">
          {{ formData?.used?.join('、') || '无' }}
        </div>
      </AFormItem>
      <AFormItem label="标签" name="category">
        <ASelect
          v-model:value="formData.category"
          placeholder="选择或输入标签"
          allow-clear
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
        <AInput v-model:value="formData.name" placeholder="请输入工具名称" />
      </AFormItem>

      <AFormItem v-if="!isBuiltin" label="编号" name="toolId">
        <AInput v-model:value="formData.toolId" placeholder="请输入工具编号（小写字母+下划线）" />
      </AFormItem>

      <AFormItem label="描述" name="description">
        <ATextarea
          v-model:value="formData.description"
          placeholder="请输入工具描述"
          :rows="3"
        />
      </AFormItem>

      <ARow :gutter="16">
        <ACol :span="12">
          <AFormItem label="是否需要确认" name="needConfirm">
            <ASwitch v-model:checked="formData.needConfirm" />
          </AFormItem>
        </ACol>
        <ACol :span="12">
          <AFormItem label="版本号" name="version">
            <AInput v-model:value="formData.version" placeholder="请输入版本号" />
          </AFormItem>
        </ACol>
      </ARow>

      <template v-if="!isBuiltin">
        <AFormItem label="输入参数">
          <div class="input-schema-list">
            <!-- 使用 :key="_tempId" 确保Vue能够正确识别每个元素 -->
            <div ref="inputSchemaListRef" class="input-schema-sortable">
              <div
                v-for="(param, index) in formData.inputSchema"
                :key="param._tempId ?? `fallback-${index}`"
                class="input-schema-item"
              >
                <ARow :gutter="8">
                  <ACol :span="1" class="input-schema-drag-handle">
                    <HolderOutlined class="drag-handle-icon" />
                  </ACol>
                  <ACol :span="5">
                    <AInput v-model:value="param.name" placeholder="参数名" />
                  </ACol>
                  <ACol :span="6">
                    <AInput v-model:value="param.description" placeholder="参数描述" />
                  </ACol>
                  <ACol :span="4">
                    <ASelect v-model:value="param.type" placeholder="类型">
                      <ASelectOption v-for="opt in typeOptions" :key="opt.value" :value="opt.value">
                        {{ opt.label }}
                      </ASelectOption>
                    </ASelect>
                  </ACol>
                  <ACol :span="4">
                    <AInput v-model:value="param.defaultValue" placeholder="默认值" />
                  </ACol>
                  <ACol :span="2">
                    <ACheckbox v-model:checked="param.required">必填</ACheckbox>
                  </ACol>
                  <ACol :span="2">
                    <AButton type="text" danger @click="removeInputParam(param)">
                      <MinusCircleOutlined />
                    </AButton>
                  </ACol>
                </ARow>
              </div>
            </div>
            <AButton type="dashed" block @click="addInputParam">
              <PlusOutlined />
              添加参数
            </AButton>
            <div v-if="formData.inputSchema.length > 1" class="text-placeholder">注意：参数顺序需要和代码中接收顺序保持一致</div>
          </div>
        </AFormItem>

        <AFormItem label="代码" name="code">
          <SmartCodeEditor
            v-if="visible"
            v-model="formData.code"
            language="java"
            height="350px"
          />
        </AFormItem>
      </template>
    </AForm>
  </ApboaModal>
</template>

<style scoped lang="scss">
.input-schema-list {
  .input-schema-sortable {
    min-height: 4px;
  }

  .input-schema-item {
    margin-bottom: 8px;
    padding: 4px 8px;
    border-radius: 4px;
    transition: background-color 0.2s;

    &:hover {
      background-color: rgba(0, 0, 0, 0.02);
    }
  }

  .input-schema-drag-handle {
    display: flex;
    align-items: center;
    cursor: grab;

    &:active {
      cursor: grabbing;
    }

    .drag-handle-icon {
      color: #999;
      font-size: 16px;

      &:hover {
        color: #666;
      }
    }
  }

  :deep(.input-schema-sortable-ghost) {
    opacity: 0.5;
    background-color: #f0f0f0;
    border: 1px dashed #1890ff;
  }

  :deep(.input-schema-sortable-chosen) {
    background-color: #fafafa;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }

  :deep(.input-schema-sortable-drag) {
    opacity: 0.9;
    transform: rotate(2deg);
    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.3);
  }

  :deep(.input-schema-sortable-fallback) {
    opacity: 1 !important;
  }
}

/* 防止拖拽时页面滚动 */
:global(body.dragging) {
  user-select: none;
  -webkit-user-select: none;
}
</style>
