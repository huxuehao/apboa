/**
 * 本地存储协议配置表单
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'

export interface LocalConfig {
  localDir?: string
}

const props = defineProps<{
  config: LocalConfig
}>()

const formRef = ref()
const formData = ref<LocalConfig>({
  localDir: '/home'
})

watch(
  () => props.config,
  (val) => {
    if (val && typeof val === 'object') {
      formData.value = {
        localDir: val.localDir ?? '/home'
      }
    }
  },
  { immediate: true }
)

const rules = {
  localDir: [{ required: true, message: '请输入本地存储目录', trigger: 'blur' }]
}

function getConfig(): LocalConfig {
  return { ...formData.value }
}

defineExpose({
  form: formRef,
  getConfig
})
</script>

<template>
  <AForm ref="formRef" :model="formData" :rules="rules" layout="vertical">
    <AFormItem label="本地存储目录" name="localDir">
      <AInput v-model:value="formData.localDir" placeholder="如 /home 或 /data/storage" />
    </AFormItem>
  </AForm>
</template>
