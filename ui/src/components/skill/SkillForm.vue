/**
 * 技能包表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, watch, defineComponent } from 'vue'
import { message } from 'ant-design-vue'
import { AppstoreOutlined } from '@ant-design/icons-vue'
import SmartCodeEditor from '@/components/editor/SmartCodeEditor.vue'
import ResourceEditor from './ResourceEditor.vue'
import type { SkillPackageVO, SkillPackage } from '@/types'
import * as skillApi from '@/api/skill'

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
}>({
  name: '',
  category: '',
  description: '',
  skillContent: '',
  references: [],
  examples: [],
  scripts: []
})

const loading = ref<boolean>(false)
const referencesEditorRef = ref()
const examplesEditorRef = ref()
const scriptsEditorRef = ref()


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
      scripts: props.data.scripts || []
    }
  } else {
    form.value = {
      name: '',
      category: '',
      description: '',
      skillContent: '',
      references: [],
      examples: [],
      scripts: []
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
      scripts: form.value.scripts.length > 0 ? form.value.scripts : null
    } as SkillPackage

    if (isEdit.value && props.data) {
      entity.id = props.data.id as string
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

const addItem = (e: any) => {
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
</script>

<template>
  <ApboaModal
    :open="visible"
    :title-icon="AppstoreOutlined"
    :title="title"
    :confirm-loading="loading"
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
    </AForm>

    <ResourceEditor
      ref="examplesEditorRef"
      v-model="form.examples"
      title="编辑示例代码"
      prefix="examples"
      :show-language-selector="true"
    />

    <ResourceEditor
      ref="scriptsEditorRef"
      v-model="form.scripts"
      title="编辑执行脚本"
      prefix="scripts"
      :show-language-selector="true"
    />
  </ApboaModal>
</template>

<style scoped lang="scss">
</style>
