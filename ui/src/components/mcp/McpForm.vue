/**
 * MCP服务器配置表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { message } from 'ant-design-vue'
import { MinusCircleOutlined, PlusOutlined, CloudServerOutlined } from '@ant-design/icons-vue'
import type { McpServerVO, McpServer } from '@/types'
import { McpProtocol, McpMode } from '@/types'
import * as mcpApi from '@/api/mcp'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  data?: McpServerVO
  initialProtocol?: McpProtocol
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
  used: [],
  name: '',
  description: '',
  protocol: McpProtocol.HTTP,
  mode: McpMode.SYNC,
  timeout: 30000
})

/**
 * HTTP/SSE协议配置
 */
const httpConfig = ref({
  url: '',
  queryParams: [] as Array<{ key: string; value: string }>,
  headers: [] as Array<{ key: string; value: string }>
})

/**
 * STDIO协议配置
 */
const stdioConfig = ref({
  command: '',
  args: [] as string[],
  env: [] as Array<{ key: string; value: string }>,
  cwd: '',
  encoding: 'UTF-8'
})

/**
 * 提交中状态
 */
const submitting = ref(false)

/**
 * 是否编辑模式
 */
const isEdit = computed(() => !!props.data)

/**
 * 表单验证规则
 */
const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  protocol: [{ required: true, message: '请选择协议', trigger: 'blur' }],
  mode: [{ required: true, message: '请选择运行模式', trigger: 'blur' }],
  description: [
    { required: true, message: '请输入描述', trigger: 'blur' },
    { max: 200, message: '描述长度不能超过200个字符', trigger: 'blur' }
  ],
}

/**
 * 协议选项
 */
const protocolOptions = [
  { label: 'HTTP', value: McpProtocol.HTTP },
  { label: 'SSE', value: McpProtocol.SSE },
  { label: 'STDIO', value: McpProtocol.STDIO }
]

/**
 * 运行模式选项
 */
const modeOptions = [
  { label: '同步', value: McpMode.SYNC },
  { label: '异步', value: McpMode.ASYNC }
]

/**
 * 监听弹窗显示状态
 */
watch(() => props.visible, (val) => {
  if (val) {
    initForm()
  }
})

/**
 * 初始化表单
 */
function initForm() {
  if (props.data) {
    formData.value = {
      used: props.data.used as [],
      name: props.data.name,
      description: props.data.description,
      protocol: props.data.protocol,
      mode: props.data.mode,
      timeout: props.data.timeout
    }
    parseProtocolConfig(props.data.protocolConfig)
  } else {
    formData.value = {
      used: [],
      name: '',
      description: '',
      protocol: props.initialProtocol || McpProtocol.HTTP,
      mode: McpMode.SYNC,
      timeout: 30000
    }
    resetProtocolConfig()
  }
}

/**
 * 解析协议配置
 */
function parseProtocolConfig(config: Record<string, unknown> | null) {
  if (!config) {
    resetProtocolConfig()
    return
  }

  if (formData.value.protocol === McpProtocol.HTTP || formData.value.protocol === McpProtocol.SSE) {
    httpConfig.value = {
      url: (config.url as string) || '',
      queryParams: Array.isArray(config.queryParams) ? config.queryParams : [],
      headers: Array.isArray(config.headers) ? config.headers : []
    }
  } else if (formData.value.protocol === McpProtocol.STDIO) {
    stdioConfig.value = {
      command: (config.command as string) || '',
      args: Array.isArray(config.args) ? config.args : [],
      env: Array.isArray(config.env) ? config.env : [],
      cwd: (config.cwd as string) || '',
      encoding: (config.encoding as string) || 'UTF-8'
    }
  }
}

/**
 * 重置协议配置
 */
function resetProtocolConfig() {
  httpConfig.value = {
    url: '',
    queryParams: [],
    headers: []
  }
  stdioConfig.value = {
    command: '',
    args: [],
    env: [],
    cwd: '',
    encoding: 'UTF-8'
  }
}

/**
 * 添加键值对
 */
function addKeyValue(list: Array<{ key: string; value: string }>) {
  list.push({ key: '', value: '' })
}

/**
 * 删除键值对
 */
function removeKeyValue(list: Array<{ key: string; value: string }>, index: number) {
  list.splice(index, 1)
}

/**
 * 添加参数值
 */
function addValue(list: string[]) {
  list.push('')
}

/**
 * 删除参数值
 */
function removeValue(list: string[], index: number) {
  list.splice(index, 1)
}

/**
 * 组装协议配置
 */
function buildProtocolConfig(): Record<string, unknown> {
  if (formData.value.protocol === McpProtocol.HTTP || formData.value.protocol === McpProtocol.SSE) {
    return {
      url: httpConfig.value.url,
      queryParams: httpConfig.value.queryParams,
      headers: httpConfig.value.headers
    }
  } else if (formData.value.protocol === McpProtocol.STDIO) {
    const config: Record<string, unknown> = {
      command: stdioConfig.value.command,
      args: stdioConfig.value.args,
      env: stdioConfig.value.env
    }
    if (stdioConfig.value.cwd) {
      config.cwd = stdioConfig.value.cwd
    }
    if (stdioConfig.value.encoding) {
      config.encoding = stdioConfig.value.encoding
    }
    return config
  }
  return {}
}

/**
 * 提交表单
 */
async function handleSubmit() {
  try {
    await formRef.value.validate()
    submitting.value = true

    const entity: Partial<McpServer> & { name: string; description: string; protocol: McpProtocol; mode: McpMode; timeout: number; protocolConfig: Record<string, unknown> } = {
      name: formData.value.name,
      description: formData.value.description,
      protocol: formData.value.protocol,
      mode: formData.value.mode,
      timeout: formData.value.timeout,
      protocolConfig: buildProtocolConfig()
    }

    if (isEdit.value) {
      entity.id = props.data!.id
      entity.enabled = props.data!.enabled
      entity.healthStatus = props.data!.healthStatus
      entity.lastHealthCheck = props.data!.lastHealthCheck
      entity.createdAt = props.data!.createdAt
      entity.updatedAt = props.data!.updatedAt
      entity.createdBy = props.data!.createdBy
      entity.updatedBy = props.data!.updatedBy
      await mcpApi.update(entity as McpServer)
      message.success('更新成功')
    } else {
      await mcpApi.save(entity as McpServer)
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
 * 取消
 */
function handleCancel() {
  emit('update:visible', false)
}
</script>

<template>
  <ApboaModal
    :open="visible"
    :title-icon="CloudServerOutlined"
    :title="isEdit ? '编辑MCP服务器' : '新增MCP服务器'"
    destroyOnClose
    @cancel="handleCancel"
  >
    <AForm
      ref="formRef"
      :model="formData"
      :rules="rules"
      layout="vertical"
    >
      <AFormItem label="关联智能体" v-if="isEdit">
        <div class="code-wrapper ">
          {{ formData?.used?.join('、') || '无' }}
        </div>
      </AFormItem>

      <AFormItem label="名称" name="name">
        <AInput v-model:value="formData.name" placeholder="请输入名称" />
      </AFormItem>

      <AFormItem label="描述" name="description">
        <ATextarea v-model:value="formData.description" placeholder="请输入描述" :rows="3" />
      </AFormItem>

      <AFormItem label="协议" name="protocol">
        <ASelect v-model:value="formData.protocol" :disabled="isEdit" placeholder="请选择协议">
          <ASelectOption v-for="opt in protocolOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </ASelectOption>
        </ASelect>
      </AFormItem>

      <AFormItem label="运行模式" name="mode">
        <ASelect v-model:value="formData.mode" placeholder="请选择运行模式">
          <ASelectOption v-for="opt in modeOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </ASelectOption>
        </ASelect>
      </AFormItem>

      <AFormItem label="超时时间(毫秒)" name="timeout">
        <AInputNumber v-model:value="formData.timeout" :min="1000" :step="1000" style="width: 100%" />
      </AFormItem>

      <ADivider orientation="left">协议配置</ADivider>

      <template v-if="formData.protocol === McpProtocol.HTTP || formData.protocol === McpProtocol.SSE">
        <AFormItem label="URL">
          <AInput v-model:value="httpConfig.url" placeholder="请输入URL" />
        </AFormItem>

        <AFormItem label="查询参数">
          <div class="param-list">
            <div v-for="(item, index) in httpConfig.queryParams" :key="index" class="param-item flex gap-sm items-center mb-sm">
              <AInput v-model:value="item.key" placeholder="参数名" style="flex: 1" />
              <AInput v-model:value="item.value" placeholder="参数值" style="flex: 1" />
              <AButton type="text" danger @click="removeKeyValue(httpConfig.queryParams, index)">
                <MinusCircleOutlined />
              </AButton>
            </div>
            <AButton type="dashed" block @click="addKeyValue(httpConfig.queryParams)">
              <PlusOutlined /> 添加参数
            </AButton>
          </div>
        </AFormItem>

        <AFormItem label="请求头">
          <div class="param-list">
            <div v-for="(item, index) in httpConfig.headers" :key="index" class="param-item flex gap-sm items-center mb-sm">
              <AInput v-model:value="item.key" placeholder="Header名" style="flex: 1" />
              <AInput v-model:value="item.value" placeholder="Header值" style="flex: 1" />
              <AButton type="text" danger @click="removeKeyValue(httpConfig.headers, index)">
                <MinusCircleOutlined />
              </AButton>
            </div>
            <AButton type="dashed" block @click="addKeyValue(httpConfig.headers)">
              <PlusOutlined /> 添加Header
            </AButton>
          </div>
        </AFormItem>
      </template>

      <template v-else-if="formData.protocol === McpProtocol.STDIO">
        <AFormItem label="命令">
          <AInput v-model:value="stdioConfig.command" placeholder="请输入可执行命令路径" />
        </AFormItem>

        <AFormItem label="命令参数">
          <div class="param-list">
            <div v-for="(item, index) in stdioConfig.args" :key="index" class="param-item flex gap-sm items-center mb-sm">
              <AInput v-model:value="stdioConfig.args[index]" placeholder="参数值" style="flex: 1" />
              <AButton type="text" danger @click="removeValue(stdioConfig.args, index)">
                <MinusCircleOutlined />
              </AButton>
            </div>
            <AButton type="dashed" block @click="addValue(stdioConfig.args)">
              <PlusOutlined /> 添加参数
            </AButton>
          </div>
        </AFormItem>

        <AFormItem label="环境变量">
          <div class="param-list">
            <div v-for="(item, index) in stdioConfig.env" :key="index" class="param-item flex gap-sm items-center mb-sm">
              <AInput v-model:value="item.key" placeholder="变量名" style="flex: 1" />
              <AInput v-model:value="item.value" placeholder="变量值" style="flex: 1" />
              <AButton type="text" danger @click="removeKeyValue(stdioConfig.env, index)">
                <MinusCircleOutlined />
              </AButton>
            </div>
            <AButton type="dashed" block @click="addKeyValue(stdioConfig.env)">
              <PlusOutlined /> 添加环境变量
            </AButton>
          </div>
        </AFormItem>

        <AFormItem label="工作目录(可选)">
          <AInput v-model:value="stdioConfig.cwd" placeholder="请输入工作目录路径" />
        </AFormItem>

        <AFormItem label="字符编码(可选)">
          <AInput v-model:value="stdioConfig.encoding" placeholder="默认UTF-8" />
        </AFormItem>
      </template>
    </AForm>

    <template #footer>
      <AButton @click="handleCancel">取消</AButton>
      <AButton type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEdit ? '更新' : '创建' }}
      </AButton>
    </template>
  </ApboaModal>
</template>

<style scoped lang="scss">
.param-list {
  width: 100%;

  .param-item {
    margin-bottom: var(--spacing-sm);
  }
}
</style>
