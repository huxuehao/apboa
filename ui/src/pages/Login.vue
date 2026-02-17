<!-- eslint-disable vue/multi-word-component-names -->
<script setup lang="ts" name="LoginPage">
/**
 * 登录页面
 *
 * @author huxuehao
 */
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance, Rule } from 'ant-design-vue/es/form'
import type { LoginRequest } from '@/types'
import { AuthContainer } from '@/components/auth'
import { useAccountStore } from '@/stores'
import { useWebSocket } from '@/composables/useWebSocket';
import { md5 } from 'js-md5'

const { initWS } = useWebSocket();

interface LoginForm extends LoginRequest {
  remember: boolean
}

const router = useRouter()
const accountStore = useAccountStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const formState = reactive<LoginForm>({
  username: 'admin',
  password: 'Admin@123.com',
  remember: false,
})

const rules: Record<string, Rule[]> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度为4-20个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20个字符', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  try {
    loading.value = true
    await formRef.value?.validate()

    const { remember, ...loginData } = formState
    await accountStore.login({...loginData, password: md5(loginData.password)})

    message.success('登录成功')
    if (remember) {
      localStorage.setItem('remember', 'true')
      localStorage.setItem('username', formState.username)
    } else {
      localStorage.removeItem('remember')
      localStorage.removeItem('username')
    }
    initWS()
    await router.push('/agent')
  } catch (error) {
    console.error('登录失败:', error)
  } finally {
    loading.value = false
  }
}

const goToRegister = () => {
  router.push('/register')
}

const goToForgotPassword = () => {
  router.push('/forgot-password')
}
</script>

<template>
  <AuthContainer>
    <div class="auth-title">欢迎使用 Apboa 智能体平台</div>
    <AForm
      ref="formRef"
      :model="formState"
      :rules="rules"
      layout="vertical"
      @finish="handleLogin"
    >
      <AFormItem name="username" class="auth-form-item">
        <AInput
          v-model:value="formState.username"
          size="large"
          placeholder="请输入用户名"
        />
      </AFormItem>

      <AFormItem name="password" class="auth-form-item">
        <AInputPassword
          v-model:value="formState.password"
          size="large"
          placeholder="请输入密码"
        />
      </AFormItem>

      <AFormItem class="auth-form-item">
        <div class="flex justify-between items-center">
          <ACheckbox v-model:checked="formState.remember">
            记住密码
          </ACheckbox>
          <a @click="goToForgotPassword" class="text-primary cursor-pointer">
            忘记密码？
          </a>
        </div>
      </AFormItem>

      <AFormItem>
        <AButton
          type="primary"
          html-type="submit"
          size="large"
          :loading="loading"
          block
        >
          登录
        </AButton>
      </AFormItem>
    </AForm>

    <div class="text-center mt-md">
      <span class="text-secondary">还没有账号？</span>
      <a @click="goToRegister" class="text-primary cursor-pointer ml-sm">
        立即注册
      </a>
    </div>
  </AuthContainer>
</template>

<style scoped lang="scss">
@use '@/styles/modules/auth' as *;
</style>
