/**
* 敏感词配置表单组件
*
* @author huxuehao
*/
<script setup lang="ts">
import {ref, watch, computed, defineComponent} from 'vue'
import { message } from 'ant-design-vue'
import type { SensitiveWordConfigVO, SensitiveWordConfig } from '@/types'
import { SensitiveWordAction } from '@/types'
import * as sensitiveApi from '@/api/sensitive'
import WordsInput from './WordsInput.vue'
import { PlusOutlined, SafetyCertificateOutlined } from "@ant-design/icons-vue";

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  data?: SensitiveWordConfigVO
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

const formData = ref<{
  used?: string[]
  category: string
  name: string
  description: string
  words: string[]
  action: SensitiveWordAction
  replacement: string
}>({
  category: '',
  name: '',
  description: '',
  words: [],
  action: SensitiveWordAction.BLOCK,
  replacement: ''
})

const inputRef = ref();
const name = ref();

const isEdit = computed(() => !!props.data?.id)

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

watch(
  () => props.visible,
  (newVal) => {
    if (newVal) {
      if (props.data) {
        const wordsArray = props.data.words || []
        formData.value = {
          used: props.data.used,
          category: props.data.category,
          name: props.data.name,
          description: props.data.description,
          words: Array.isArray(wordsArray) ? wordsArray : [],
          action: props.data.action,
          replacement: props.data.replacement || ''
        }
      } else {
        resetForm()
      }
    }
  }
)

/**
 * 处理分类搜索
 */
function handleCategorySearch(value: string) {
  categorySearchText.value = value
}

/**
 * 表单验证规则
 */
const rules = {
  category: [
    { required: true, message: '请选择或输入标签', trigger: 'blur' },
    { max: 6, message: '标签长度不能超过6个字符', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  description: [
    { required: true, message: '请输入描述', trigger: 'blur' },
    { max: 200, message: '描述长度不能超过200个字符', trigger: 'blur' }
  ],
  words: [{ required: true, message: '请添加敏感词', trigger: 'blur' }],
  action: [{ required: true, message: '请选择处理动作', trigger: 'blur' }],
  replacement: [
    {
      required: true,
      message: '请输入替换文本',
      trigger: 'blur',
      validator: (_rule: unknown, value: string) => {
        if (formData.value.action === SensitiveWordAction.REPLACE && !value) {
          return Promise.reject('请输入替换文本')
        }
        return Promise.resolve()
      }
    }
  ]
}

/**
 * 重置表单
 */
function resetForm() {
  formData.value = {
    used:[],
    category: '',
    name: '',
    description: '',
    words: [],
    action: SensitiveWordAction.BLOCK,
    replacement: ''
  }
  formRef.value?.resetFields()
}

/**
 * 处理提交
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    loading.value = true

    const entity: SensitiveWordConfig = {
      category: formData.value.category || '',
      name: formData.value.name,
      description: formData.value.description,
      words: formData.value.words,
      action: formData.value.action,
      replacement: formData.value.replacement
    } as SensitiveWordConfig

    if (isEdit.value && props.data) {
      entity.id = props.data.id as string
      await sensitiveApi.update(entity)
      message.success('更新成功')
    } else {
      await sensitiveApi.save(entity)
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

const addItem = (e: any) => {
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
    :title-icon="SafetyCertificateOutlined"
    :title="isEdit ? '编辑敏感词配置' : '新增敏感词配置'"
    :confirm-loading="loading"
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
        <AInput v-model:value="formData.name" placeholder="请输入配置名称" />
      </AFormItem>

      <AFormItem label="描述" name="description">
        <ATextarea
          v-model:value="formData.description"
          placeholder="请输入配置描述"
          :rows="3"
        />
      </AFormItem>

      <AFormItem label="敏感词" name="words">
        <WordsInput v-model="formData.words" />
      </AFormItem>

      <ARow :gutter="16">
        <ACol :span="12">
          <AFormItem label="处理动作" name="action">
            <ARadioGroup v-model:value="formData.action">
              <ARadio :value="SensitiveWordAction.BLOCK">阻止</ARadio>
              <ARadio :value="SensitiveWordAction.WARN">警告</ARadio>
            </ARadioGroup>
          </AFormItem>
        </ACol>
        <ACol :span="12" v-if="formData.action === SensitiveWordAction.REPLACE">
          <AFormItem
            label="替换文本"
            name="replacement"
          >
            <AInput v-model:value="formData.replacement" placeholder="请输入替换文本" />
          </AFormItem>
        </ACol>
      </ARow>
    </AForm>
  </ApboaModal>
</template>

<style scoped lang="scss"></style>
