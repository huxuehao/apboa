/**
 * 全部账户管理组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, CheckOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useAccountStore } from '@/stores'
import * as accountApi from '@/api/account'
import * as authApi from '@/api/auth'
import type {AccountVO, RegisterRequest, Role} from '@/types'
import {md5} from "js-md5";
const accountStore = useAccountStore()

const tagColor = {
  ADMIN: 'blue',
  EDIT: 'cyan',
  READ_ONLY: 'default',
}

/**
 * 当前用户信息
 */
const userInfo = computed(() => accountStore.userInfo)

/**
 * 是否为管理员
 */
const isAdmin = computed(() => accountStore.isAdmin)

/**
 * 账户列表
 */
const accountList = ref<AccountVO[]>([])
const loading = ref(false)

/**
 * 搜索关键词
 */
const searchKeyword = ref('')

/**
 * 过滤后的账户列表
 */
const filteredAccounts = computed(() => {
  if (!searchKeyword.value.trim()) {
    return accountList.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return accountList.value.filter(
    (account) =>
      account.nickname.toLowerCase().includes(keyword) ||
      account.username.toLowerCase().includes(keyword) ||
      account.email.toLowerCase().includes(keyword)
  )
})

/**
 * 操作菜单项
 */
const menuItems = [
  { key: 'READ_ONLY', label: '只读' },
  { key: 'EDIT', label: '编辑' },
  { key: 'ADMIN', label: '管理员' }
]

/**
 * 处理切换角色
 */
async function handleMenuClick({ key }: { key: string }, accountId:string) {
  await accountApi.changeRole(accountId, [key as Role])
  message.success('修改成功')
  await fetchAccounts()
}

/**
 * 获取账户列表
 */
async function fetchAccounts() {
  try {
    loading.value = true
    const response = await accountApi.list({ enabled: undefined })
    accountList.value = response.data.data
  } catch {
    console.error('获取账户列表失败')
  } finally {
    loading.value = false
  }
}

/**
 * 新增账户模态窗
 */
const createModalVisible = ref(false)
const createFormRef = ref()
const createForm = ref<RegisterRequest>({
  nickname: '',
  username: '',
  email: '',
  password: ''
})
const createLoading = ref(false)

/**
 * 新增表单规则
 */
const createRules = {
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

/**
 * 打开新增模态窗
 */
function openCreateModal() {
  createForm.value = {
    nickname: '',
    username: '',
    email: '',
    password: ''
  }
  createModalVisible.value = true
}

/**
 * 提交新增账户
 */
async function handleCreateSubmit() {
  try {
    await createFormRef.value.validate()
    createLoading.value = true

    await authApi.adminCreateAccount({
      ...createForm.value,
      password: md5(createForm.value.password)
    })
    message.success('账户创建成功')

    createModalVisible.value = false
    await fetchAccounts()
  } catch (error: unknown) {
    if (error && typeof error === 'object' && 'errorFields' in error) {
      return
    }
    console.error('创建失败')
  } finally {
    createLoading.value = false
  }
}

/**
 * 禁用/激活账户
 */
async function handleToggleEnabled(account: AccountVO) {
  const action = account.enabled ? '禁用' : '激活'
  Modal.confirm({
    title: '确认操作',
    icon: null,
    content: `确定要${action}账户 "${account.nickname}" 吗？`,
    onOk: async () => {
      try {
        await accountApi.toggleEnabled(account.id as string, !account.enabled)
        message.success(`${action}成功`)
        await fetchAccounts()
      } catch {
        console.error(`${action}失败`)
      }
    }
  })
}

/**
 * 重置密码模态窗
 */
const resetPasswordModalVisible = ref(false)
const resetPasswordForm = ref({ newPassword: '' })
const resetPasswordFormRef = ref()
const resetPasswordLoading = ref(false)
const currentResetAccountId = ref('')

/**
 * 重置密码表单规则
 */
const resetPasswordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ]
}

/**
 * 打开重置密码模态窗
 */
function openResetPasswordModal(accountId: string) {
  currentResetAccountId.value = accountId
  resetPasswordForm.value = { newPassword: '' }
  resetPasswordModalVisible.value = true
}

/**
 * 提交重置密码
 */
async function handleResetPasswordSubmit() {
  try {
    await resetPasswordFormRef.value.validate()
    resetPasswordLoading.value = true

    await accountApi.adminChangePassword(
      currentResetAccountId.value,
      md5(resetPasswordForm.value.newPassword)
    )
    message.success('密码重置成功')

    resetPasswordModalVisible.value = false
  } catch (error: unknown) {
    if (error && typeof error === 'object' && 'errorFields' in error) {
      return
    }
    console.error('密码重置失败')
  } finally {
    resetPasswordLoading.value = false
  }
}

/**
 * 删除账户
 */
function handleDelete(account: AccountVO) {
  if (account.id === userInfo.value?.id) {
    message.warning('不能删除当前登录账户')
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: `确定要删除账户 "${account.nickname}" 吗？此操作不可恢复。`,
    icon: null,
    onOk: async () => {
      try {
        await accountApi.remove([account.id] as string[])
        message.success('删除成功')
        await fetchAccounts()
      } catch {
        console.error('删除失败')
      }
    }
  })
}

/**
 * 获取头像文本
 */
function getAvatarText(username: string) {
  return username.charAt(0).toUpperCase()
}

/**
 * 获取角色标签文本
 */
function getRoleLabel(role: string) {
  const labelMap: Record<string, string> = {
    ADMIN: '管理员',
    EDIT: '编辑',
    READ_ONLY: '只读'
  }
  return labelMap[role] || role
}

onMounted(() => {
  fetchAccounts()
})
</script>

<template>
  <div>
    <div class="all-accounts-header">
      <h2 class="settings-page-title m-0">全部账户</h2>
      <div class="all-accounts-toolbar">
        <AInput
          v-model:value="searchKeyword"
          placeholder="输入关键字"
          style="width: 250px; ">
          <template #suffix>
            <SearchOutlined />
          </template>
        </AInput>
        <AButton v-if="isAdmin" type="primary" @click="openCreateModal">
          新增账户
        </AButton>
      </div>
    </div>

    <ASpin :spinning="loading">
      <div class="account-list">
        <div
          v-for="account in filteredAccounts"
          :key="account.id"
          class="account-list-item"
        >
          <AAvatar :size="36" class="account-list-avatar">
            {{ getAvatarText(account.username) }}
          </AAvatar>
          <div class="account-list-info">
            <div class="account-list-name-row">
              <span class="account-list-name">{{ account.nickname }}</span>
              <span class="account-list-username">@{{ account.username }}</span>
            </div>
            <div class="account-list-email">{{ account.email }}</div>
          </div>
          <div class="account-list-meta">
            <ATag
              v-for="role in account.roles"
              :key="role"
              :color="tagColor[role]"
              :bordered="false"
            >
              <UserOutlined />{{ getRoleLabel(role) }}
            </ATag>
            <ATag :bordered="false" :color="account.enabled ? 'success' : 'error'">
              {{ account.enabled ? '正常' : '禁用' }}
            </ATag>
          </div>
          <div v-if="isAdmin" class="account-list-actions">
            <a-divider type="vertical" />
            <AButton
              type="text"
              size="small"
              :style="{
                color: account.enabled ? '#FF2727' : '#5EC829'
              }"
              @click="handleToggleEnabled(account)"
            >
              {{ account.enabled ? '禁用' : '激活' }}
            </AButton>
            <AButton
              type="text"
              size="small"
              @click="openResetPasswordModal(account.id as string)"
            >
              重置密码
            </AButton>
            <ADropdown :trigger="['click']">
              <AButton type="text" size="small">
                角色
              </AButton>
              <template #overlay>
                <AMenu @click="(e:any) => handleMenuClick(e, account.id as string)">
                  <AMenuItem v-for="item in menuItems" :key="item.key">
                     <span style="display: flex; align-items: center; gap: 8px;">
                       {{ item.label }}
                       <CheckOutlined v-if="account.roles?.includes(item.key as Role)" />
                     </span>
                  </AMenuItem>
                </AMenu>
              </template>
            </ADropdown>
            <AButton
              type="text"
              size="small"
              :style="{
                color: account.id === userInfo?.id ? '#B7B9BF' : '#FF2727'
              }"
              :disabled="account.id === userInfo?.id"
              @click="handleDelete(account)"
            >
              删除
            </AButton>
          </div>
        </div>
      </div>

      <AEmpty v-if="!loading && filteredAccounts.length === 0" description="暂无账户数据" />
    </ASpin>

    <!-- 新增账户模态窗 -->
    <AModal
      v-model:open="createModalVisible"
      title="新增账户"
      :confirm-loading="createLoading"
      @ok="handleCreateSubmit"
    >
      <AForm
        ref="createFormRef"
        :model="createForm"
        :rules="createRules"
        layout="vertical"
      >
        <AFormItem label="昵称" name="nickname">
          <AInput v-model:value="createForm.nickname" placeholder="请输入昵称" />
        </AFormItem>
        <AFormItem label="账号" name="username">
          <AInput v-model:value="createForm.username" placeholder="请输入账号" />
        </AFormItem>
        <AFormItem label="邮箱" name="email">
          <AInput v-model:value="createForm.email" placeholder="请输入邮箱" />
        </AFormItem>
        <AFormItem label="初始密码" name="password">
          <AInputPassword v-model:value="createForm.password" placeholder="请输入初始密码" />
        </AFormItem>
      </AForm>
    </AModal>

    <!-- 重置密码模态窗 -->
    <AModal
      v-model:open="resetPasswordModalVisible"
      title="重置密码"
      :confirm-loading="resetPasswordLoading"
      @ok="handleResetPasswordSubmit"
    >
      <AForm
        ref="resetPasswordFormRef"
        :model="resetPasswordForm"
        :rules="resetPasswordRules"
        layout="vertical"
      >
        <AFormItem label="新密码" name="newPassword">
          <AInputPassword
            v-model:value="resetPasswordForm.newPassword"
            placeholder="请输入新密码"
          />
        </AFormItem>
      </AForm>
    </AModal>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/modules/_settings.scss' as *;
</style>
