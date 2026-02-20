/**
 * 知识库配置表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {type KnowledgeBaseConfig, type KnowledgeBaseConfigVO, type KbType, RAGMode} from '@/types'
import * as knowledgeApi from '@/api/knowledge'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  data?: KnowledgeBaseConfigVO
  initialKbType?: KbType
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:visible': [visible: boolean]
  success: []
}>()

/**
 * 是否编辑模式
 */
const isEdit = computed(() => !!props.data?.id)

/**
 * 弹窗标题
 */
const modalTitle = computed(() => isEdit.value ? '编辑知识库' : '新增知识库')

/**
 * 表单引用
 */
const formRef = ref()

/**
 * 表单数据
 */
const formData = reactive<Partial<KnowledgeBaseConfigVO>>({
  used: [],
  name: '',
  kbType: undefined,
  ragMode: RAGMode.AGENTIC,
  description: '',
  connectionConfig: {},
  endpointConfig: {},
  retrievalConfig: {},
  rerankingConfig: {},
  queryRewriteConfig: {},
  metadataFilters: {},
  httpConfig: {}
})

/**
 * 当前选中的配置区域
 */
const activeConfigSection = ref<string>('connection')

/**
 * 配置区域选项(根据类型动态显示)
 */
const configSectionOptions = computed(() => {
  if (!formData.kbType) return []

  const baseOptions = [
    { label: '* 连接配置', value: 'connection' }
  ]

  if (formData.kbType === 'BAILIAN') {
    return [
      ...baseOptions,
      { label: '端点配置', value: 'endpoint' },
      { label: '检索配置', value: 'retrieval' },
      { label: '重排序配置', value: 'reranking' },
      { label: '查询重写配置', value: 'queryRewrite' }
    ]
  } else if (formData.kbType === 'DIFY') {
    return [
      ...baseOptions,
      { label: '端点配置', value: 'endpoint' },
      { label: '检索配置', value: 'retrieval' },
      { label: '重排序配置', value: 'reranking' },
      { label: '元数据过滤', value: 'metadata' },
      { label: 'HTTP配置', value: 'http' }
    ]
  } else if (formData.kbType === 'RAGFLOW') {
    return [
      ...baseOptions,
      { label: '检索配置', value: 'retrieval' },
      { label: 'HTTP配置', value: 'http' }
    ]
  }

  return baseOptions
})

/**
 * 百炼连接配置
 */
const bailianConnection = reactive({
  accessKeyId: '',
  accessKeySecret: '',
  workspaceId: '',
  indexId: '',
  saveRetrieverHistory: false
})

/**
 * Dify连接配置
 */
const difyConnection = reactive({
  apiKey: '',
  datasetId: '',
  saveRetrieverHistory: false
})

/**
 * RAGFlow连接配置
 */
const ragflowConnection = reactive({
  apiKey: '',
  baseUrl: '',
  datasetIds: [] as string[],
  documentIds: [] as string[]
})

// 端点配置
const bailianEndpoint = reactive({ endpoint: '' })
const difyEndpoint = reactive({ apiBaseUrl: '' })

// 检索配置
const bailianRetrieval = reactive({
  denseSimilarityTopK: undefined as number | undefined,
  sparseSimilarityTopK: undefined as number | undefined
})

const difyRetrieval = reactive({
  retrievalMode: 'HYBRID_SEARCH',
  topK: undefined as number | undefined,
  scoreThreshold: undefined as number | undefined,
  weights: undefined as number | undefined
})

const ragflowRetrieval = reactive({
  topK: 1024,
  similarityThreshold: 0.2,
  vectorSimilarityWeight: 0.3,
  page: 1,
  pageSize: 30,
  useKg: false,
  tocEnhance: false,
  rerankId: undefined as number | undefined,
  keyword: false,
  highlight: false,
  crossLanguages: [] as string[]
})

// 重排序配置
const bailianReranking = reactive({
  enableReranking: false,
  modelName: '',
  rerankMinScore: undefined as number | undefined,
  rerankTopN: undefined as number | undefined
})

const difyReranking = reactive({
  enableRerank: false,
  providerName: '',
  modelName: '',
  topN: undefined as number | undefined
})

// 查询重写配置(仅百炼)
const queryRewrite = reactive({
  enableRewrite: false,
  modelName: ''
})

// 元数据过滤(仅Dify)
const metadataFilters = reactive({
  logicalOperator: 'AND',
  conditions: [] as Array<{ name: string; comparisonOperator: string; value: string }>
})

// HTTP配置
const difyHttp = reactive({
  connectTimeout: '',
  readTimeout: '',
  maxRetries: undefined as number | undefined,
  customHeaders: '{}'
})

const ragflowHttp = reactive({
  timeout: '',
  maxRetries: undefined as number | undefined,
  customHeaders: '{}'
})

/**
 * 监听visible变化,初始化表单
 */
watch(() => props.visible, (visible) => {
  if (visible) {
    initForm()
  } else {
    resetForm()
  }
})

/**
 * 初始化表单
 */
function initForm() {
  if (props.data) {
    Object.assign(formData, props.data)
    loadConfigData()
  } else if (props.initialKbType) {
    formData.kbType = props.initialKbType
  }
  activeConfigSection.value = 'connection'
}

/**
 * 加载配置数据到各个配置对象
 */
function loadConfigData() {
  const { connectionConfig, endpointConfig, retrievalConfig, rerankingConfig, queryRewriteConfig, metadataFilters: mf, httpConfig } = props.data!

  if (formData.kbType === 'BAILIAN') {
    if (connectionConfig) {
      Object.assign(bailianConnection, connectionConfig)
    }
    if (endpointConfig) {
      Object.assign(bailianEndpoint, endpointConfig)
    }
    if (retrievalConfig) {
      Object.assign(bailianRetrieval, retrievalConfig)
    }
    if (rerankingConfig) {
      bailianReranking.enableReranking = !!rerankingConfig.enableReranking
      if (rerankingConfig.rerankConfig) {
        Object.assign(bailianReranking, rerankingConfig.rerankConfig)
      }
    }
    if (queryRewriteConfig) {
      queryRewrite.enableRewrite = !!queryRewriteConfig.enableRewrite
      if (queryRewriteConfig.rewriteConfig) {
        Object.assign(queryRewrite, queryRewriteConfig.rewriteConfig)
      }
    }
  } else if (formData.kbType === 'DIFY') {
    if (connectionConfig) {
      Object.assign(difyConnection, connectionConfig)
    }
    if (endpointConfig) {
      Object.assign(difyEndpoint, endpointConfig)
    }
    if (retrievalConfig) {
      Object.assign(difyRetrieval, retrievalConfig)
    }
    if (rerankingConfig) {
      difyReranking.enableRerank = !!rerankingConfig.enableRerank
      if (rerankingConfig.rerankConfig) {
        Object.assign(difyReranking, rerankingConfig.rerankConfig)
      }
    }
    if (mf) {
      Object.assign(metadataFilters, mf)
    }
    if (httpConfig) {
      Object.assign(difyHttp, httpConfig)
      if (httpConfig.customHeaders) {
        difyHttp.customHeaders = JSON.stringify(httpConfig.customHeaders, null, 2)
      }
    }
  } else if (formData.kbType === 'RAGFLOW') {
    if (connectionConfig) {
      Object.assign(ragflowConnection, connectionConfig)
    }
    if (retrievalConfig) {
      Object.assign(ragflowRetrieval, retrievalConfig)
    }
    if (httpConfig) {
      Object.assign(ragflowHttp, httpConfig)
      if (httpConfig.customHeaders) {
        ragflowHttp.customHeaders = JSON.stringify(httpConfig.customHeaders, null, 2)
      }
    }
  }
}

/**
 * 重置表单
 */
function resetForm() {
  formRef.value?.resetFields()
  Object.assign(formData, {
    name: '',
    kbType: undefined,
    description: '',
    connectionConfig: {},
    endpointConfig: {},
    retrievalConfig: {},
    rerankingConfig: {},
    queryRewriteConfig: {},
    metadataFilters: {},
    httpConfig: {}
  })

  // 重置所有配置对象
  Object.assign(bailianConnection, { accessKeyId: '', accessKeySecret: '', workspaceId: '', indexId: '', saveRetrieverHistory: false })
  Object.assign(difyConnection, { apiKey: '', datasetId: '', saveRetrieverHistory: false })
  Object.assign(ragflowConnection, { apiKey: '', baseUrl: '', datasetIds: [], documentIds: [] })
  Object.assign(bailianEndpoint, { endpoint: '' })
  Object.assign(difyEndpoint, { apiBaseUrl: '' })
  Object.assign(bailianRetrieval, { denseSimilarityTopK: undefined, sparseSimilarityTopK: undefined })
  Object.assign(difyRetrieval, { retrievalMode: 'HYBRID_SEARCH', topK: undefined, scoreThreshold: undefined, weights: undefined })
  Object.assign(ragflowRetrieval, { topK: 1024, similarityThreshold: 0.2, vectorSimilarityWeight: 0.3, page: 1, pageSize: 30, useKg: false, tocEnhance: false, rerankId: undefined, keyword: false, highlight: false, crossLanguages: [] })
  Object.assign(bailianReranking, { enableReranking: false, modelName: '', rerankMinScore: undefined, rerankTopN: undefined })
  Object.assign(difyReranking, { enableRerank: false, providerName: '', modelName: '', topN: undefined })
  Object.assign(queryRewrite, { enableRewrite: false, modelName: '' })
  Object.assign(metadataFilters, { logicalOperator: 'AND', conditions: [] })
  Object.assign(difyHttp, { connectTimeout: '', readTimeout: '', maxRetries: undefined, customHeaders: '{}' })
  Object.assign(ragflowHttp, { timeout: '', maxRetries: undefined, customHeaders: '{}' })
}

/**
 * 构建提交数据
 */
function buildSubmitData(): KnowledgeBaseConfig {
  const data: Record<string, unknown> = {
    name: formData.name,
    kbType: formData.kbType,
    description: formData.description,
    ragMode: formData.ragMode
  }

  if (isEdit.value) {
    data.id = formData.id
  }

  // 根据类型构建配置
  if (formData.kbType === 'BAILIAN') {
    data.connectionConfig = { ...bailianConnection }
    data.endpointConfig = bailianEndpoint.endpoint ? { endpoint: bailianEndpoint.endpoint } : null
    data.retrievalConfig = (bailianRetrieval.denseSimilarityTopK || bailianRetrieval.sparseSimilarityTopK) ? { ...bailianRetrieval } : null

    if (bailianReranking.enableReranking) {
      data.rerankingConfig = {
        enableReranking: true,
        rerankConfig: {
          modelName: bailianReranking.modelName,
          rerankMinScore: bailianReranking.rerankMinScore,
          rerankTopN: bailianReranking.rerankTopN
        }
      }
    } else {
      data.rerankingConfig = null
    }

    if (queryRewrite.enableRewrite) {
      data.queryRewriteConfig = {
        enableRewrite: true,
        rewriteConfig: {
          modelName: queryRewrite.modelName
        }
      }
    } else {
      data.queryRewriteConfig = null
    }
  } else if (formData.kbType === 'DIFY') {
    data.connectionConfig = { ...difyConnection }
    data.endpointConfig = difyEndpoint.apiBaseUrl ? { apiBaseUrl: difyEndpoint.apiBaseUrl } : null
    data.retrievalConfig = { ...difyRetrieval }

    if (difyReranking.enableRerank) {
      data.rerankingConfig = {
        enableRerank: true,
        rerankConfig: {
          providerName: difyReranking.providerName,
          modelName: difyReranking.modelName,
          topN: difyReranking.topN
        }
      }
    } else {
      data.rerankingConfig = null
    }

    data.metadataFilters = metadataFilters.conditions.length > 0 ? { ...metadataFilters } : null

    if (difyHttp.connectTimeout || difyHttp.readTimeout || difyHttp.maxRetries) {
      data.httpConfig = {
        connectTimeout: difyHttp.connectTimeout || undefined,
        readTimeout: difyHttp.readTimeout || undefined,
        maxRetries: difyHttp.maxRetries || undefined,
        customHeaders: difyHttp.customHeaders ? JSON.parse(difyHttp.customHeaders) : undefined
      }
    } else {
      data.httpConfig = null
    }
  } else if (formData.kbType === 'RAGFLOW') {
    data.connectionConfig = { ...ragflowConnection }
    data.retrievalConfig = { ...ragflowRetrieval }

    if (ragflowHttp.timeout || ragflowHttp.maxRetries) {
      data.httpConfig = {
        timeout: ragflowHttp.timeout || undefined,
        maxRetries: ragflowHttp.maxRetries || undefined,
        customHeaders: ragflowHttp.customHeaders ? JSON.parse(ragflowHttp.customHeaders) : undefined
      }
    } else {
      data.httpConfig = null
    }
  }

  return data as unknown as KnowledgeBaseConfig
}

/**
 * 处理提交
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()

    const submitData = buildSubmitData()

    if (isEdit.value) {
      await knowledgeApi.update(submitData)
      message.success('编辑成功')
    } else {
      await knowledgeApi.save(submitData)
      message.success('新增成功')
    }

    emit('update:visible', false)
    emit('success')
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

/**
 * 处理取消
 */
function handleCancel() {
  emit('update:visible', false)
}

/**
 * 添加元数据条件
 */
function addMetadataCondition() {
  metadataFilters.conditions.push({
    name: '',
    comparisonOperator: '=',
    value: ''
  })
}

/**
 * 删除元数据条件
 */
function removeMetadataCondition(index: number) {
  metadataFilters.conditions.splice(index, 1)
}
</script>

<template>
  <AModal
    :open="visible"
    :title="modalTitle"
    width="900px"
    style="top: 5vh"
    destroyOnClose
    @cancel="handleCancel"
  >
    <AForm
      ref="formRef"
      :model="formData"
      layout="vertical"
      style="max-height: 70vh; overflow-y: auto; padding: 0 1px;"
    >
      <AFormItem label="关联智能体" v-if="isEdit">
        <div class="code-wrapper ">
          {{ formData?.used?.join('、') || '无' }}
        </div>
      </AFormItem>

      <AFormItem label="类型" name="kbType" :rules="[{ required: true, message: '请选择类型' }]">
        <ASelect v-model:value="formData.kbType" placeholder="请选择知识库类型" :disabled="isEdit">
          <ASelectOption value="BAILIAN">百炼</ASelectOption>
          <ASelectOption value="DIFY">Dify</ASelectOption>
          <ASelectOption value="RAGFLOW">RAGFlow</ASelectOption>
        </ASelect>
      </AFormItem>

      <AFormItem label="RAG模式" name="ragMode" :rules="[{ required: true, message: '请选择RAG模式' }]">
        <ASelect v-model:value="formData.ragMode" placeholder="请选择RAG模式">
          <ASelectOption value="GENERIC">Generic（在每个推理步骤之前自动检索和注入知识）</ASelectOption>
          <ASelectOption value="AGENTIC">Agentic（Agent 使用工具决定何时检索）</ASelectOption>
        </ASelect>
      </AFormItem>

      <AFormItem label="名称" name="name" :rules="[{ required: true, message: '请输入名称' }]">
        <AInput v-model:value="formData.name" placeholder="请输入知识库名称" />
      </AFormItem>

      <AFormItem label="描述" name="description" :rules="[
                 { required: true, message: '请输入描述', trigger: 'blur' },
                 { max: 200, message: '描述长度不能超过200个字符', trigger: 'blur' }]">
        <ATextarea
          v-model:value="formData.description"
          placeholder="请输入描述"
          :rows="3"
        />
      </AFormItem>

      <ADivider style="margin: 16px 0" />

      <div v-if="formData.kbType" class="config-section">
        <ASegmented
          v-model:value="activeConfigSection"
          :options="configSectionOptions"
          block
          style="margin-bottom: 16px; background-color: #F2F4F7"
        />

        <!-- 连接配置 -->
        <div v-show="activeConfigSection === 'connection'" class="config-content">
          <h4 style="margin-bottom: 12px">连接配置</h4>

          <!-- 百炼连接配置 -->
          <template v-if="formData.kbType === 'BAILIAN'">
            <AFormItem label="Access Key ID" :rules="[{ required: true, message: '请输入Access Key ID' }]">
              <AInput v-model:value="bailianConnection.accessKeyId" placeholder="请输入Access Key ID" />
            </AFormItem>
            <AFormItem label="Access Key Secret" :rules="[{ required: true, message: '请输入Access Key Secret' }]">
              <AInputPassword v-model:value="bailianConnection.accessKeySecret" placeholder="请输入Access Key Secret" />
            </AFormItem>
            <AFormItem label="Workspace ID" :rules="[{ required: true, message: '请输入Workspace ID' }]">
              <AInput v-model:value="bailianConnection.workspaceId" placeholder="请输入Workspace ID" />
            </AFormItem>
            <AFormItem label="Index ID" :rules="[{ required: true, message: '请输入Index ID' }]">
              <AInput v-model:value="bailianConnection.indexId" placeholder="请输入Index ID" />
            </AFormItem>
            <AFormItem label="保存检索历史">
              <ASwitch v-model:checked="bailianConnection.saveRetrieverHistory" />
            </AFormItem>
          </template>

          <!-- Dify连接配置 -->
          <template v-if="formData.kbType === 'DIFY'">
            <AFormItem label="API Key" :rules="[{ required: true, message: '请输入API Key' }]">
              <AInputPassword v-model:value="difyConnection.apiKey" placeholder="请输入API Key" />
            </AFormItem>
            <AFormItem label="Dataset ID" :rules="[{ required: true, message: '请输入Dataset ID' }]">
              <AInput v-model:value="difyConnection.datasetId" placeholder="请输入Dataset ID" />
            </AFormItem>
            <AFormItem label="保存检索历史">
              <ASwitch v-model:checked="difyConnection.saveRetrieverHistory" />
            </AFormItem>
          </template>

          <!-- RAGFlow连接配置 -->
          <template v-if="formData.kbType === 'RAGFLOW'">
            <AFormItem label="API Key" :rules="[{ required: true, message: '请输入API Key' }]">
              <AInputPassword v-model:value="ragflowConnection.apiKey" placeholder="请输入API Key" />
            </AFormItem>
            <AFormItem label="Base URL" :rules="[{ required: true, message: '请输入Base URL' }]">
              <AInput v-model:value="ragflowConnection.baseUrl" placeholder="请输入Base URL" />
            </AFormItem>
            <AFormItem label="Dataset IDs">
              <ASelect
                v-model:value="ragflowConnection.datasetIds"
                mode="tags"
                placeholder="输入后按回车添加"
                :token-separators="[',']"
              />
            </AFormItem>
            <AFormItem label="Document IDs">
              <ASelect
                v-model:value="ragflowConnection.documentIds"
                mode="tags"
                placeholder="输入后按回车添加"
                :token-separators="[',']"
              />
            </AFormItem>
          </template>
        </div>

        <!-- 端点配置 -->
        <div v-show="activeConfigSection === 'endpoint'" class="config-content">
          <h4 style="margin-bottom: 12px">端点配置(可选)</h4>

          <template v-if="formData.kbType === 'BAILIAN'">
            <AFormItem label="Endpoint">
              <AInput v-model:value="bailianEndpoint.endpoint" placeholder="例如: bailian.cn-beijing.aliyuncs.com" />
            </AFormItem>
          </template>

          <template v-if="formData.kbType === 'DIFY'">
            <AFormItem label="API Base URL">
              <AInput v-model:value="difyEndpoint.apiBaseUrl" placeholder="例如: https://api.dify.ai/v1" />
            </AFormItem>
          </template>
        </div>

        <!-- 检索配置 -->
        <div v-show="activeConfigSection === 'retrieval'" class="config-content">
          <h4 style="margin-bottom: 12px">检索配置(可选)</h4>

          <template v-if="formData.kbType === 'BAILIAN'">
            <AFormItem label="Dense Similarity Top K">
              <AInputNumber v-model:value="bailianRetrieval.denseSimilarityTopK" :min="1" :max="1000" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Sparse Similarity Top K">
              <AInputNumber v-model:value="bailianRetrieval.sparseSimilarityTopK" :min="1" :max="1000" style="width: 100%" />
            </AFormItem>
          </template>

          <template v-if="formData.kbType === 'DIFY'">
            <AFormItem label="Retrieval Mode">
              <ASelect v-model:value="difyRetrieval.retrievalMode">
                <ASelectOption value="HYBRID_SEARCH">混合检索</ASelectOption>
                <ASelectOption value="VECTOR">向量检索</ASelectOption>
                <ASelectOption value="FULL_TEXT">全文检索</ASelectOption>
              </ASelect>
            </AFormItem>
            <AFormItem label="Top K">
              <AInputNumber v-model:value="difyRetrieval.topK" :min="1" :max="100" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Score Threshold">
              <AInputNumber v-model:value="difyRetrieval.scoreThreshold" :min="0" :max="1" :step="0.1" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Weights">
              <AInputNumber v-model:value="difyRetrieval.weights" :min="0" :max="1" :step="0.1" style="width: 100%" />
            </AFormItem>
          </template>

          <template v-if="formData.kbType === 'RAGFLOW'">
            <AFormItem label="Top K">
              <AInputNumber v-model:value="ragflowRetrieval.topK" :min="1" :max="2048" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Similarity Threshold">
              <AInputNumber v-model:value="ragflowRetrieval.similarityThreshold" :min="0" :max="1" :step="0.1" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Vector Similarity Weight">
              <AInputNumber v-model:value="ragflowRetrieval.vectorSimilarityWeight" :min="0" :max="1" :step="0.1" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Page">
              <AInputNumber v-model:value="ragflowRetrieval.page" :min="1" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Page Size">
              <AInputNumber v-model:value="ragflowRetrieval.pageSize" :min="1" :max="100" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Use KG">
              <ASwitch v-model:checked="ragflowRetrieval.useKg" />
            </AFormItem>
            <AFormItem label="TOC Enhance">
              <ASwitch v-model:checked="ragflowRetrieval.tocEnhance" />
            </AFormItem>
            <AFormItem label="Rerank ID">
              <AInputNumber v-model:value="ragflowRetrieval.rerankId" :min="0" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Keyword">
              <ASwitch v-model:checked="ragflowRetrieval.keyword" />
            </AFormItem>
            <AFormItem label="Highlight">
              <ASwitch v-model:checked="ragflowRetrieval.highlight" />
            </AFormItem>
            <AFormItem label="Cross Languages">
              <ASelect
                v-model:value="ragflowRetrieval.crossLanguages"
                mode="tags"
                placeholder="输入语言代码后按回车添加, 如: en, zh"
                :token-separators="[',']"
              />
            </AFormItem>
          </template>
        </div>

        <!-- 重排序配置 -->
        <div v-show="activeConfigSection === 'reranking'" class="config-content">
          <h4 style="margin-bottom: 12px">重排序配置(可选)</h4>

          <template v-if="formData.kbType === 'BAILIAN'">
            <AFormItem label="启用重排序">
              <ASwitch v-model:checked="bailianReranking.enableReranking" />
            </AFormItem>
            <template v-if="bailianReranking.enableReranking">
              <AFormItem label="Model Name" :rules="[{ required: true, message: '请输入Model Name' }]">
                <AInput v-model:value="bailianReranking.modelName" placeholder="例如: gte-rerank-hybrid" />
              </AFormItem>
              <AFormItem label="Rerank Min Score">
                <AInputNumber v-model:value="bailianReranking.rerankMinScore" :min="0" :max="1" :step="0.1" style="width: 100%" />
              </AFormItem>
              <AFormItem label="Rerank Top N">
                <AInputNumber v-model:value="bailianReranking.rerankTopN" :min="1" :max="100" style="width: 100%" />
              </AFormItem>
            </template>
          </template>

          <template v-if="formData.kbType === 'DIFY'">
            <AFormItem label="启用重排序">
              <ASwitch v-model:checked="difyReranking.enableRerank" />
            </AFormItem>
            <template v-if="difyReranking.enableRerank">
              <AFormItem label="Provider Name" :rules="[{ required: true, message: '请输入Provider Name' }]">
                <AInput v-model:value="difyReranking.providerName" placeholder="例如: cohere" />
              </AFormItem>
              <AFormItem label="Model Name" :rules="[{ required: true, message: '请输入Model Name' }]">
                <AInput v-model:value="difyReranking.modelName" placeholder="例如: rerank-english-v2.0" />
              </AFormItem>
              <AFormItem label="Top N">
                <AInputNumber v-model:value="difyReranking.topN" :min="1" :max="100" style="width: 100%" />
              </AFormItem>
            </template>
          </template>
        </div>

        <!-- 查询重写配置(仅百炼) -->
        <div v-show="activeConfigSection === 'queryRewrite'" class="config-content">
          <h4 style="margin-bottom: 12px">查询重写配置(可选)</h4>

          <AFormItem label="启用查询重写">
            <ASwitch v-model:checked="queryRewrite.enableRewrite" />
          </AFormItem>
          <template v-if="queryRewrite.enableRewrite">
            <AFormItem label="Model Name" :rules="[{ required: true, message: '请输入Model Name' }]">
              <AInput v-model:value="queryRewrite.modelName" placeholder="例如: conv-rewrite-qwen-1.8b" />
            </AFormItem>
          </template>
        </div>

        <!-- 元数据过滤(仅Dify) -->
        <div v-show="activeConfigSection === 'metadata'" class="config-content">
          <h4 style="margin-bottom: 12px">元数据过滤(可选)</h4>

          <AFormItem label="Logical Operator">
            <ASelect v-model:value="metadataFilters.logicalOperator">
              <ASelectOption value="AND">AND</ASelectOption>
              <ASelectOption value="OR">OR</ASelectOption>
            </ASelect>
          </AFormItem>

          <AFormItem label="Conditions">
            <div v-for="(condition, index) in metadataFilters.conditions" :key="index" class="flex gap-sm mb-sm">
              <AInput v-model:value="condition.name" placeholder="Name" style="flex: 1" />
              <ASelect v-model:value="condition.comparisonOperator" style="width: 100px">
                <ASelectOption value="=">=</ASelectOption>
                <ASelectOption value=">">&gt;</ASelectOption>
                <ASelectOption value="<">&lt;</ASelectOption>
                <ASelectOption value=">=">&gt;=</ASelectOption>
                <ASelectOption value="<=">&lt;=</ASelectOption>
                <ASelectOption value="!=">!=</ASelectOption>
              </ASelect>
              <AInput v-model:value="condition.value" placeholder="Value" style="flex: 1" />
              <AButton type="text" danger @click="removeMetadataCondition(index)">删除</AButton>
            </div>
            <AButton type="dashed" block @click="addMetadataCondition">添加条件</AButton>
          </AFormItem>
        </div>

        <!-- HTTP配置 -->
        <div v-show="activeConfigSection === 'http'" class="config-content">
          <h4 style="margin-bottom: 12px">HTTP配置(可选)</h4>

          <template v-if="formData.kbType === 'DIFY'">
            <AFormItem label="Connect Timeout">
              <AInput v-model:value="difyHttp.connectTimeout" placeholder="例如: PT30S" />
            </AFormItem>
            <AFormItem label="Read Timeout">
              <AInput v-model:value="difyHttp.readTimeout" placeholder="例如: PT60S" />
            </AFormItem>
            <AFormItem label="Max Retries">
              <AInputNumber v-model:value="difyHttp.maxRetries" :min="0" :max="10" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Custom Headers (JSON)">
              <ATextarea v-model:value="difyHttp.customHeaders" :rows="4" placeholder='{"X-Custom-Header": "value"}' />
            </AFormItem>
          </template>

          <template v-if="formData.kbType === 'RAGFLOW'">
            <AFormItem label="Timeout">
              <AInput v-model:value="ragflowHttp.timeout" placeholder="例如: PT30S" />
            </AFormItem>
            <AFormItem label="Max Retries">
              <AInputNumber v-model:value="ragflowHttp.maxRetries" :min="0" :max="10" style="width: 100%" />
            </AFormItem>
            <AFormItem label="Custom Headers (JSON)">
              <ATextarea v-model:value="ragflowHttp.customHeaders" :rows="4" placeholder='{"X-Custom-Header": "value"}' />
            </AFormItem>
          </template>
        </div>
      </div>
    </AForm>

    <template #footer>
      <AButton @click="handleCancel">取消</AButton>
      <AButton type="primary" @click="handleSubmit">确定</AButton>
    </template>
  </AModal>
</template>

<style scoped lang="scss">
.config-section {
  .config-content {
    padding: 12px;
    background-color: var(--color-bg-light);
    border-radius: var(--border-radius-base);

    h4 {
      font-weight: 600;
      color: var(--color-text-primary);
    }
  }
}
</style>
