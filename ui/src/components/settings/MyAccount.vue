/**
 * 我的账户组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useAccountStore } from '@/stores'
import * as authApi from '@/api/auth'
import type { UpdateProfileRequest, ChangePasswordRequest } from '@/types'
import {md5} from "js-md5";

const accountStore = useAccountStore()

/**
 * 当前用户信息
 */
const userInfo = computed(() => accountStore.userInfo)

/**
 * 头像文本
 */
const avatarText = computed(() => {
  if (!userInfo.value?.username) return '?'
  return userInfo.value.username.charAt(0).toUpperCase()
})

// ========== 基本信息编辑 ==========

const editFormRef = ref()
const editForm = ref<UpdateProfileRequest>({
  nickname: '',
  email: ''
})
const editLoading = ref(false)

/**
 * 编辑信息表单规则
 */
const editRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

/**
 * 同步用户信息到编辑表单
 */
watch(() => userInfo.value, (val) => {
  if (val) {
    editForm.value = {
      nickname: val.nickname,
      email: val.email
    }
  }
}, { immediate: true })

/**
 * 提交基本信息修改
 */
async function handleEditSubmit() {
  try {
    await editFormRef.value.validate()
    editLoading.value = true
    await authApi.updateProfile(editForm.value)
    message.success('信息更新成功')
    await accountStore.fetchCurrentUserInfo()
  } catch (error: unknown) {
    if (error && typeof error === 'object' && 'errorFields' in error) return
  } finally {
    editLoading.value = false
  }
}

// ========== 修改密码 ==========

const passwordModalVisible = ref(false)
const passwordFormRef = ref()
const passwordForm = ref<ChangePasswordRequest & { confirmPassword: string }>({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})
const passwordLoading = ref(false)

/**
 * 重置密码表单规则
 */
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string) => {
        if (value !== passwordForm.value.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ]
}

/**
 * 打开修改密码模态窗
 */
function openPasswordModal() {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
  }
  passwordModalVisible.value = true
}

/**
 * 提交修改密码
 */
async function handlePasswordSubmit() {
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true
    const { oldPassword, newPassword } = passwordForm.value
    await authApi.changePassword({ oldPassword: md5(oldPassword), newPassword: md5(newPassword) })
    message.success('密码修改成功')
    passwordModalVisible.value = false
  } catch (error: unknown) {
    if (error && typeof error === 'object' && 'errorFields' in error) return
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div>
    <h2 class="settings-page-title">我的账户</h2>

    <!-- 头像与身份信息 -->
    <div class="my-account-profile">
      <AAvatar :size="64" class="my-account-avatar">
        <span style="font-size: 34px">{{ avatarText }}</span>
      </AAvatar>
      <div>
        <div class="my-account-name">{{ userInfo?.nickname }}</div>
        <div class="my-account-username">@{{ userInfo?.username }}</div>
      </div>
    </div>

    <!-- 基本信息表单 -->
    <AForm
      ref="editFormRef"
      :model="editForm"
      :rules="editRules"
      layout="vertical"
      class="my-account-form"
    >
      <AFormItem label="昵称" name="nickname">
        <AInput v-model:value="editForm.nickname" placeholder="请输入昵称" />
      </AFormItem>
      <AFormItem label="邮箱" name="email">
        <AInput v-model:value="editForm.email" placeholder="请输入邮箱" />
      </AFormItem>
      <AFormItem>
        <AButton type="primary" :loading="editLoading" @click="handleEditSubmit">
          保存修改
        </AButton>
      </AFormItem>
    </AForm>

    <!-- 安全设置 -->
    <div class="my-account-section-title">安全设置</div>
    <AButton @click="openPasswordModal">修改密码</AButton>

    <!-- 修改密码模态窗 -->
    <AModal
      v-model:open="passwordModalVisible"
      title="修改密码"
      :confirm-loading="passwordLoading"
      @ok="handlePasswordSubmit"
    >
      <AForm
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        layout="vertical"
      >
        <AFormItem label="旧密码" name="oldPassword">
          <AInputPassword v-model:value="passwordForm.oldPassword" placeholder="请输入旧密码" />
        </AFormItem>
        <AFormItem label="新密码" name="newPassword">
          <AInputPassword v-model:value="passwordForm.newPassword" placeholder="请输入新密码" />
        </AFormItem>
        <AFormItem label="确认密码" name="confirmPassword">
          <AInputPassword v-model:value="passwordForm.confirmPassword" placeholder="请再次输入新密码" />
        </AFormItem>
      </AForm>
    </AModal>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/_settings.scss' as *;
</style>
