/**
 * 提示词模板表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch, computed, defineComponent } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, FileTextOutlined } from '@ant-design/icons-vue';
import type { SystemPromptTemplateVO, SystemPromptTemplate } from '@/types'
import * as promptApi from '@/api/prompt'
import SmartCodeEditor from '@/components/editor/SmartCodeEditor.vue'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  data?: SystemPromptTemplateVO
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
  content: string
}>({
  category: '',
  name: '',
  description: '',
  content: ''
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
        formData.value = {
          used: props.data.used,
          category: props.data.category,
          name: props.data.name,
          description: props.data.description,
          content: props.data.content
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
const rules = {
  category: [
    { required: true, message: '请选择或输入标签', trigger: 'blur' },
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
  content: [
    { required: true, message: '请输入提示词内容', trigger: 'blur' }
  ]
}

/**
 * 重置表单
 */
function resetForm() {
  formData.value = {
    category: '',
    name: '',
    description: '',
    content: ''
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

    const entity: SystemPromptTemplate = {
      category: formData.value.category || '',
      name: formData.value.name,
      description: formData.value.description,
      content: formData.value.content,
      usageCount: 0
    } as SystemPromptTemplate

    if (isEdit.value && props.data) {
      entity.id = props.data.id
      await promptApi.update(entity)
      message.success('更新成功')
    } else {
      await promptApi.save(entity)
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

/**
 * 处理分类搜索
 */
function handleCategorySearch(value: string) {
  categorySearchText.value = value
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
    :title-icon="FileTextOutlined"
    :title="isEdit ? '编辑提示词模板' : '新增提示词模板'"
    :confirm-loading="loading"
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
        <AInput v-model:value="formData.name" placeholder="请输入模板名称" />
      </AFormItem>

      <AFormItem label="描述" name="description">
        <ATextarea
          v-model:value="formData.description"
          placeholder="请输入模板描述"
          :rows="3"
        />
      </AFormItem>

      <AFormItem label="提示词内容" name="content">
        <SmartCodeEditor
          v-if="visible"
          v-model="formData.content"
          language="markdown"
          height="350px"
        />
      </AFormItem>
    </AForm>
  </ApboaModal>
</template>

<style scoped lang="scss"></style>
