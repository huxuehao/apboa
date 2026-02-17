<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts" name="RegisterPage">
/**
 * 注册页面
 *
 * @author huxuehao
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import type { RegisterRequest } from '@/types'
import { AuthContainer } from '@/components/auth'
import { useAccountStore } from '@/stores'
import {md5} from "js-md5";

interface RegisterForm extends RegisterRequest {
  confirmPassword: string
}

const router = useRouter()
const accountStore = useAccountStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const formState = reactive<RegisterForm>({
  nickname: '',
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = (_rule: Rule, value: string) => {
  if (value === '') {
    return Promise.reject('请再次输入密码')
  } else if (value !== formState.password) {
    return Promise.reject('两次输入的密码不一致')
  } else {
    return Promise.resolve()
  }
}

const rules: Record<string, Rule[]> = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度为2-20个字符', trigger: 'blur' },
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度为4-20个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  try {
    loading.value = true
    await formRef.value?.validate()

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { confirmPassword: _confirmPassword, ...registerData } = formState
    await accountStore.register({
      ...registerData,
      password: md5(registerData.password)
    })

    message.success('注册成功,请登录')
    await router.push('/login')
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <AuthContainer :show-back="true" back-to="/login">
    <div class="auth-title">欢迎使用 Apboa 智能体平台</div>
    <AForm
      ref="formRef"
      :model="formState"
      :rules="rules"
      layout="vertical"
      @finish="handleRegister"
    >
      <AFormItem name="nickname" class="auth-form-item">
        <AInput
          v-model:value="formState.nickname"
          size="large"
          placeholder="请输入昵称"
        />
      </AFormItem>

      <AFormItem name="username" class="auth-form-item">
        <AInput
          v-model:value="formState.username"
          size="large"
          placeholder="请输入用户名"
        />
      </AFormItem>

      <AFormItem name="email" class="auth-form-item">
        <AInput
          v-model:value="formState.email"
          size="large"
          placeholder="请输入邮箱"
        />
      </AFormItem>

      <AFormItem name="password" class="auth-form-item">
        <AInputPassword
          v-model:value="formState.password"
          size="large"
          placeholder="请输入密码"
        />
      </AFormItem>

      <AFormItem name="confirmPassword" class="auth-form-item">
        <AInputPassword
          v-model:value="formState.confirmPassword"
          size="large"
          placeholder="请再次输入密码"
        />
      </AFormItem>

      <AFormItem>
        <AButton
          type="primary"
          html-type="submit"
          size="large"
          :loading="loading"
          block
        >
          注册
        </AButton>
      </AFormItem>
    </AForm>

    <div class="text-center mt-md">
      <span class="text-secondary">已有账号？</span>
      <a @click="goToLogin" class="text-primary cursor-pointer ml-sm">
        去登录
      </a>
    </div>
  </AuthContainer>
</template>

<style scoped lang="scss">
@use '@/styles/modules/auth' as *;
</style>
