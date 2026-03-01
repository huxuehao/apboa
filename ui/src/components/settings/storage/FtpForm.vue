/**
 * FTP 存储协议配置表单
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'

export interface FtpConfig {
  host?: string
  port?: number
  userName?: string
  password?: string
}

const props = defineProps<{
  config: FtpConfig
}>()

const formRef = ref()
const formData = ref<FtpConfig>({
  host: '',
  port: 21,
  userName: '',
  password: '',
})

watch(
  () => props.config,
  (val) => {
    if (val && typeof val === 'object') {
      formData.value = {
        host: val.host ?? '',
        port: val.port ?? 21,
        userName: val.userName ?? '',
        password: val.password ?? '',
      }
    }
  },
  { immediate: true }
)

const rules = {
  host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function getConfig(): FtpConfig {
  return { ...formData.value }
}

defineExpose({
  form: formRef,
  getConfig
})
</script>

<template>
  <AForm ref="formRef" :model="formData" :rules="rules" layout="vertical">
    <AFormItem label="主机地址" name="host">
      <AInput v-model:value="formData.host" placeholder="如 192.168.1.100 或 ftp.example.com" />
    </AFormItem>
    <AFormItem label="端口" name="port">
      <AInputNumber v-model:value="formData.port" :min="1" :max="65535" placeholder="默认 21" style="width: 100%" />
    </AFormItem>
    <AFormItem label="用户名" name="userName">
      <AInput v-model:value="formData.userName" placeholder="请输入 FTP 用户名" />
    </AFormItem>
    <AFormItem label="密码" name="password">
      <AInputPassword v-model:value="formData.password" placeholder="请输入 FTP 密码" />
    </AFormItem>
  </AForm>
</template>
