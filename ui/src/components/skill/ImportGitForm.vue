/**
 * Git 技能包导入表单组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { GithubOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import * as skillApi from '@/api/skill'
import type { GitImportConfig } from '@/types'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
}>()

/**
 * Emits定义
 */
const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

const formRef = ref()
const loading = ref(false)

const form = ref<GitImportConfig>({
  category: 'Git导入',
  repoUrl: '',
  cover: false
})

/**
 * 表单校验规则
 */
const rules = {
  category: [{ required: true, message: '请输入技能分类', trigger: 'blur' }],
  repoUrl: [
    { required: true, message: '请输入 Git 仓库地址', trigger: 'blur' },
    {
      pattern: /^(https?:\/\/|git@).+/,
      message: '请输入合法的 Git 仓库地址（支持 HTTPS 或 SSH）',
      trigger: 'blur'
    }
  ]
}

/**
 * 重置表单
 */
function resetForm() {
  form.value = {
    category: 'Git导入',
    repoUrl: '',
    cover: false
  }
}

/**
 * 提交表单
 */
async function handleSubmit() {
  try {
    await formRef.value?.validate()
    loading.value = true
    await skillApi.importFromGit(form.value)
    message.success('Git 技能包导入成功')
    emit('update:visible', false)
    emit('success')
  } catch (error) {
    console.error('导入失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 取消
 */
function handleCancel() {
  emit('update:visible', false)
}

watch(() => props.visible, (val) => {
  if (val) {
    resetForm()
  } else {
    formRef.value?.resetFields()
  }
})
</script>

<template>
  <AModal
    :open="visible"
    title="下载 Git 技能包"
    width="560px"
    :confirm-loading="loading"
    ok-text="开始导入"
    cancel-text="取消"
    destroyOnClose
    @ok="handleSubmit"
    @cancel="handleCancel"
  >
    <div class="import-desc">
      <GithubOutlined class="import-desc__icon" />
      <div class="import-desc__text">
        <p>从远程 Git 仓库拉取并导入技能包。系统将自动克隆仓库并扫描技能文件。</p>
      </div>
    </div>

    <AAlert
      type="info"
      :show-icon="false"
      class="repo-tip"
    >
      <template #message>
        <div class="repo-tip__content">
          <InfoCircleOutlined class="repo-tip__icon" />
          <span>
            如果仓库中存在 <code>skills/</code> 子目录，会优先从该目录加载；
            否则将使用仓库根目录作为技能来源。
          </span>
        </div>
      </template>
    </AAlert>

    <AForm
      ref="formRef"
      :model="form"
      :rules="rules"
      layout="vertical"
      class="import-form"
    >
      <AFormItem label="技能分类" name="category">
        <AInput
          v-model:value="form.category"
          placeholder="请输入技能分类标签"
          allow-clear
        />
        <template #extra>导入的技能包将被归入此分类</template>
      </AFormItem>

      <AFormItem label="仓库地址" name="repoUrl">
        <AInput
          v-model:value="form.repoUrl"
          placeholder="例如：https://github.com/user/repo.git"
          allow-clear
        >
          <template #prefix>
            <GithubOutlined class="text-secondary" />
          </template>
        </AInput>
        <template #extra>支持 HTTPS 和 SSH 协议的 Git 仓库地址</template>
      </AFormItem>

      <AFormItem label="覆盖策略" name="cover">
        <div class="cover-option">
          <ASwitch v-model:checked="form.cover" />
          <span class="cover-option__label">{{ form.cover ? '覆盖已有同名技能' : '跳过已有同名技能' }}</span>
        </div>
        <template #extra>开启后，若存在同名技能包，将以新导入的内容覆盖原有数据</template>
      </AFormItem>
    </AForm>
  </AModal>
</template>

<style scoped lang="scss">
.import-desc {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  margin-bottom: 16px;
  background: var(--color-bg-light, #f8f9fa);
  border: 1px solid var(--color-border-base, #e8e8e8);
  border-radius: 8px;

  &__icon {
    font-size: 18px;
    color: var(--color-primary);
    margin-top: 2px;
    flex-shrink: 0;
  }

  &__text {
    flex: 1;

    p {
      margin: 0;
      font-size: 13px;
      color: var(--color-text-secondary, #666);
      line-height: 1.6;
    }
  }
}

.repo-tip {
  margin-bottom: 20px;
  border-radius: 8px;

  &__content {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    font-size: 13px;
    line-height: 1.6;
    color: var(--color-text-base, #333);
  }

  &__icon {
    color: var(--color-primary);
    margin-top: 2px;
    flex-shrink: 0;
  }

  code {
    padding: 1px 6px;
    background: rgba(0, 0, 0, 0.06);
    border-radius: 4px;
    font-size: 12px;
    font-family: monospace;
  }
}

.import-form {
  :deep(.ant-form-item-extra) {
    font-size: 12px;
    margin-top: 4px;
  }
}

.cover-option {
  display: flex;
  align-items: center;
  gap: 10px;

  &__label {
    font-size: 13px;
    color: var(--color-text-base, #333);
  }
}
</style>
