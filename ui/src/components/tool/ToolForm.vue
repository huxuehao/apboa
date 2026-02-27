/**
 * 工具表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch, computed, defineComponent } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
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
}

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
public class Demo implements IDynamicAgentTool {

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

watch(
  () => props.visible,
  (newVal) => {
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
          inputSchema: Array.isArray(inputSchemaList) ? inputSchemaList.map((item: InputSchemaItem) => ({
            name: item.name || '',
            description: item.description || '',
            type: item.type || 'string',
            defaultValue: item.defaultValue || '',
            required: item.required || false
          })) : [],
          code: props.data.code || ''
        }
      } else {
        resetForm()
      }
    }
  }
)

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
    required: false
  })
}

/**
 * 删除输入参数
 */
function removeInputParam(index: number) {
  formData.value.inputSchema.splice(index, 1)
}

/**
 * 处理提交
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    loading.value = true

    const entity: ToolConfig = {
      category: formData.value.category || '',
      name: formData.value.name,
      toolId: formData.value.toolId,
      description: formData.value.description,
      toolType: isBuiltin.value ? ToolType.BUILTIN : ToolType.CUSTOM,
      needConfirm: formData.value.needConfirm,
      inputSchema: isBuiltin.value ? null : formData.value.inputSchema || [],
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
  <AModal
    :open="visible"
    :title="isEdit ? '编辑工具' : '新增工具'"
    :confirm-loading="loading"
    width="900px"
    style="top: 0"
    destroyOnClose
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
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
            <div v-for="(param, index) in formData.inputSchema" :key="index" class="input-schema-item">
              <ARow :gutter="8">
                <ACol :span="5">
                  <AInput v-model:value="param.name" placeholder="参数名" />
                </ACol>
                <ACol :span="7">
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
                  <AButton type="text" danger @click="removeInputParam(index)">
                    <MinusCircleOutlined />
                  </AButton>
                </ACol>
              </ARow>
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
  </AModal>
</template>

<style scoped lang="scss">
.input-schema-list {
  .input-schema-item {
    margin-bottom: 8px;
  }
}
</style>
