/**
 * 代码执行配置表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { CodeExecutionConfig, CodeExecutionConfigVO } from '@/types'
import * as codeExecutionConfigApi from '@/api/codeExecutionConfig'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  data?: CodeExecutionConfigVO
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

/**
 * 表单引用
 */
const formRef = ref()

/**
 * 表单数据
 */
const formData = ref({
  configName: '',
  workDir: '.apboa/skill-scripts/skills',
  uploadDir: '.apboa/skill-scripts/skills',
  autoUpload: false,
  enableShell: true,
  enableRead: false,
  enableWrite: false,
  command: ['python3', 'python','bash','node', 'npm'] as string[]
})

/**
 * 新命令输入
 */
const newCommand = ref('')

/**
 * 提交中状态
 */
const submitting = ref(false)

/**
 * 是否为编辑模式
 */
const isEdit = computed(() => !!props.data?.id)

/**
 * 表单验证规则
 */
const rules = {
  configName: [
    { required: true, message: '请输入配置名称', trigger: 'blur' }
  ]
}

/**
 * 重置表单
 */
function resetForm() {
  formData.value = {
    configName: '',
    workDir: '.apboa/skill-scripts/skills',
    uploadDir: '.apboa/skill-scripts/skills',
    autoUpload: false,
    enableShell: true,
    enableRead: false,
    enableWrite: false,
    command: ['python3', 'python', 'bash','node', 'npm']
  }
  newCommand.value = ''
  formRef.value?.resetFields()
}

/**
 * 监听弹窗显示状态
 */
watch(() => props.visible, (val) => {
  if (val) {
    if (props.data) {
      formData.value = {
        configName: props.data.configName,
        workDir: props.data.workDir || '',
        uploadDir: props.data.uploadDir || '.apboa/skill-scripts/skills',
        autoUpload: props.data.autoUpload || false,
        enableShell: props.data.enableShell || false,
        enableRead: props.data.enableRead || false,
        enableWrite: props.data.enableWrite || false,
        command: props.data.command || []
      }
    } else {
      resetForm()
    }
  }
})

/**
 * 添加命令
 */
function handleAddCommand() {
  const cmd = newCommand.value.trim()
  if (cmd && !formData.value.command.includes(cmd)) {
    formData.value.command.push(cmd)
    newCommand.value = ''
  }
}

/**
 * 删除命令
 */
function handleRemoveCommand(index: number) {
  formData.value.command.splice(index, 1)
}

/**
 * 处理提交
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const entity: CodeExecutionConfig = {
      configName: formData.value.configName,
      workDir: formData.value.workDir || undefined,
      uploadDir: formData.value.uploadDir || '.apboa/skill-scripts/skills',
      autoUpload: formData.value.autoUpload,
      enableShell: formData.value.enableShell,
      enableRead: formData.value.enableRead,
      enableWrite: formData.value.enableWrite,
      command: formData.value.command.length > 0 ? formData.value.command : null
    }

    if (isEdit.value && props.data) {
      entity.id = props.data.id
      await codeExecutionConfigApi.update(entity)
      message.success('更新成功')
    } else {
      await codeExecutionConfigApi.save(entity)
      message.success('创建成功')
    }

    emit('update:visible', false)
    emit('success')
  } catch (error) {
    console.error('提交失败:', error)
  } finally {
    submitting.value = false
  }
}

/**
 * 处理取消
 */
function handleCancel() {
  emit('update:visible', false)
}
</script>

<template>
  <AModal
    :open="visible"
    :title="isEdit ? '编辑代码执行配置' : '新增代码执行配置'"
    :confirm-loading="submitting"
    :destroy-on-close="true"
    width="640px"
    style="top: 50px"
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <AForm
      ref="formRef"
      :model="formData"
      :rules="rules"
      layout="vertical"
    >
      <AFormItem label="配置名称" name="configName">
        <AInput v-model:value="formData.configName" placeholder="请输入配置名称" />
      </AFormItem>

      <AFormItem label="工作目录">
        <AInput v-model:value="formData.workDir" placeholder="留空则使用临时目录" />
        <div class="text-placeholder text-xs mt-xs">
          指定代码执行的工作目录，留空则使用系统临时目录
        </div>
      </AFormItem>

      <AFormItem label="Skill目录（只读）">
        <AInput v-model:value="formData.uploadDir" readonly/>
      </AFormItem>

      <AFormItem label="工具开关配置">
        <ARow :gutter="16">
<!--          <ACol :span="12">-->
<!--            <AFormItem>-->
<!--              <ASwitch v-model:checked="formData.autoUpload" />-->
<!--              <span class="ml-sm">自动上传Skill文件</span>-->
<!--            </AFormItem>-->
<!--          </ACol>-->
          <ACol :span="24">
            <AFormItem>
              <ASwitch v-model:checked="formData.enableShell" />
              <span class="ml-sm">启用Shell执行</span>
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem>
              <ASwitch v-model:checked="formData.enableRead" />
              <span class="ml-sm">启用文件读取</span>
            </AFormItem>
          </ACol>
          <ACol :span="12">
            <AFormItem>
              <ASwitch v-model:checked="formData.enableWrite" />
              <span class="ml-sm">启用文件写入</span>
            </AFormItem>
          </ACol>
        </ARow>
      </AFormItem>

      <AFormItem label="允许执行的命令" v-if="formData.enableShell">
        <div class="command-input">
          <AInput
            v-model:value="newCommand"
            placeholder="输入命令名称，如 python3、bash"
            @pressEnter="handleAddCommand"
          />
          <AButton type="primary" @click="handleAddCommand">
            <template #icon><PlusOutlined /></template>
          </AButton>
        </div>
        <div v-if="formData.command.length > 0" class="command-tags mt-sm">
          <ATag
            v-for="(cmd, index) in formData.command"
            :key="cmd"
            closable
            @close="handleRemoveCommand(index)"
          >
            {{ cmd }}
          </ATag>
        </div>
        <div class="text-placeholder text-xs mt-xs">
          配置允许智能体执行的命令列表，如 python3、bash、node 等
        </div>
      </AFormItem>
    </AForm>
  </AModal>
</template>

<style scoped lang="scss">
.command-input {
  display: flex;
  gap: 8px;
  align-items: center;
}

.command-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
</style>
