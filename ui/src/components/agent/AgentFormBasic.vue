/**
 * 智能体基本信息表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, defineComponent } from 'vue'
import { PlusOutlined } from '@ant-design/icons-vue'

/**
 * Props定义
 */
const props = defineProps<{
  modelValue: {
    name: string
    agentCode: string
    description: string
    tag: string
  }
  tags: string[]
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:modelValue': [value: {
    name: string
    agentCode: string
    description: string
    tag: string
  }]
}>()

const formRef = ref()
const inputRef = ref()
const newTagName = ref('')

/**
 * 表单数据
 */
const formData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

/**
 * 过滤后的标签列表
 */
const filteredTags = computed(() => {
  return props.tags || []
})

/**
 * 表单验证规则
 */
const rules = {
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { max: 100, message: '名称长度不能超过100个字符', trigger: 'blur' }
  ],
  agentCode: [
    { required: true, message: '请输入智能体编号', trigger: 'blur' },
    { max: 50, message: '智能体编号长度不能超过50个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '智能体编号只能包含字母、数字、下划线和连字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入描述', trigger: 'blur' },
    { max: 500, message: '描述长度不能超过500个字符', trigger: 'blur' }
  ]
}

/**
 * 添加新标签
 */
const addTag = (e: Event) => {
  e.preventDefault()
  if (newTagName.value && !filteredTags.value.includes(newTagName.value)) {
    formData.value.tag = newTagName.value
  }
  newTagName.value = ''
  setTimeout(() => {
    inputRef.value?.focus()
  }, 0)
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

/**
 * VNodes组件
 */
const VNodes = defineComponent({
  props: {
    vnodes: {
      type: Object,
      required: true
    }
  },
  render() {
    return this.vnodes
  }
})

defineExpose({
  validate
})
</script>

<template>
  <AForm
    ref="formRef"
    :model="formData"
    :rules="rules"
    layout="vertical"
    style="padding: 0 1px;">
    <AFormItem label="标签" name="tag">
      <ASelect
        v-model:value="formData.tag"
        placeholder="选择或输入标签"
        allow-clear
      >
        <ASelectOption v-for="tag in filteredTags" :key="tag" :value="tag">
          {{ tag }}
        </ASelectOption>
        <template #dropdownRender="{ menuNode: menu }">
          <VNodes :vnodes="menu" />
          <ADivider style="margin: 4px 0" />
          <ASpace style="padding: 4px 8px">
            <AInput
              ref="inputRef"
              v-model:value="newTagName"
              style="width: 300px"
              placeholder="输入新标签"
            />
            <AButton type="text" @click="addTag">
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
      <AInput v-model:value="formData.name" placeholder="请输入智能体名称" />
    </AFormItem>

    <AFormItem label="智能体编号" name="agentCode">
      <AInput v-model:value="formData.agentCode" placeholder="请输入智能体编号，如: my-agent-001" />
    </AFormItem>

    <AFormItem label="描述" name="description">
      <ATextarea
        v-model:value="formData.description"
        placeholder="请输入智能体描述"
        :rows="4"
      />
    </AFormItem>
  </AForm>
</template>

<style scoped lang="scss">
</style>
