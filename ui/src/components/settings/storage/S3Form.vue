/**
 * S3 存储协议配置表单
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'

export interface S3Config {
  endpoint?: string
  appId?: string
  region?: string
  accessKey?: string
  secretKey?: string
  bucketName?: string
}

const props = defineProps<{
  config: S3Config
}>()

const formRef = ref()
const formData = ref<S3Config>({
  endpoint: '',
  appId: '',
  region: '',
  accessKey: '',
  secretKey: '',
  bucketName: 'etm'
})

watch(
  () => props.config,
  (val) => {
    if (val && typeof val === 'object') {
      formData.value = {
        endpoint: val.endpoint ?? '',
        appId: val.appId ?? '',
        region: val.region ?? '',
        accessKey: val.accessKey ?? '',
        secretKey: val.secretKey ?? '',
        bucketName: val.bucketName ?? 'etm'
      }
    }
  },
  { immediate: true }
)

const rules = {
  endpoint: [{ required: true, message: '请输入 endpoint', trigger: 'blur' }],
  accessKey: [{ required: true, message: '请输入 accessKey', trigger: 'blur' }],
  secretKey: [{ required: true, message: '请输入 secretKey', trigger: 'blur' }],
  bucketName: [{ required: true, message: '请输入 bucketName', trigger: 'blur' }]
}

function getConfig(): S3Config {
  return { ...formData.value }
}

defineExpose({
  form: formRef,
  getConfig
})
</script>

<template>
  <AForm ref="formRef" :model="formData" :rules="rules" layout="vertical">
    <AFormItem label="Endpoint" name="endpoint">
      <AInput v-model:value="formData.endpoint" placeholder="如 https://s3.amazonaws.com" />
    </AFormItem>
    <AFormItem label="AccessKey" name="accessKey">
      <AInput v-model:value="formData.accessKey" placeholder="请输入 AccessKey" />
    </AFormItem>
    <AFormItem label="SecretKey" name="secretKey">
      <AInputPassword v-model:value="formData.secretKey" placeholder="请输入 SecretKey" />
    </AFormItem>
    <AFormItem label="Bucket 名称" name="bucketName">
      <AInput v-model:value="formData.bucketName" placeholder="默认 etm" />
    </AFormItem>
    <AFormItem label="AppId" name="appId">
      <AInput v-model:value="formData.appId" placeholder="可选" />
    </AFormItem>
    <AFormItem label="Region" name="region">
      <AInput v-model:value="formData.region" placeholder="如 us-east-1" />
    </AFormItem>
  </AForm>
</template>
