/**
 * 智能体定时任务表单组件
 *
 * @author huxuehao
 */
<template>
  <AModal
    :open="visible"
    :title="modalTitle"
    :confirm-loading="loading"
    :width="600"
    destroyOnClose
    @ok="handleSubmit"
    @cancel="handleCancel"
    @update:open="(val: boolean) => emit('update:visible', val)"
  >
    <AAlert
      message="说明：定时任务目前不支持集群部署，后面会进行改造升级~"
      banner
      closable
    />
    <AForm
      ref="formRef"
      :model="formData"
      :rules="formRules"
      layout="vertical"
      class="job-form"
    >
      <!-- 输入内容 -->
      <AFormItem label="输入内容" name="input">
        <ATextarea
          v-model:value="formData.input"
          placeholder="请输入定时任务触发时发送给智能体的消息内容"
          :rows="4"
          show-count
          :maxlength="500"
        />
      </AFormItem>

      <!-- Cron表达式 -->
      <AFormItem label="执行策略" name="cron" required>
        <div class="cron-section">
          <!-- Cron生成器 -->
          <div class="cron-generator">
            <div class="cron-row">
              <span class="cron-label">秒</span>
              <ASelect
                v-model:value="cronParts.second"
                style="width: 120px"
                @change="updateCronFromParts"
              >
                <ASelectOption value="0">0秒</ASelectOption>
                <ASelectOption value="*/5">每5秒</ASelectOption>
                <ASelectOption value="*/10">每10秒</ASelectOption>
                <ASelectOption value="*/30">每30秒</ASelectOption>
              </ASelect>
            </div>
            <div class="cron-row">
              <span class="cron-label">分</span>
              <ASelect
                v-model:value="cronParts.minute"
                style="width: 120px"
                @change="updateCronFromParts"
              >
                <ASelectOption value="*">每分</ASelectOption>
                <ASelectOption value="0">0分</ASelectOption>
                <ASelectOption value="*/5">每5分</ASelectOption>
                <ASelectOption value="*/10">每10分</ASelectOption>
                <ASelectOption value="*/30">每30分</ASelectOption>
              </ASelect>
            </div>
            <div class="cron-row">
              <span class="cron-label">时</span>
              <ASelect
                v-model:value="cronParts.hour"
                style="width: 120px"
                @change="updateCronFromParts"
              >
                <ASelectOption value="*">每小时</ASelectOption>
                <ASelectOption value="0">0点</ASelectOption>
                <ASelectOption value="8">8点</ASelectOption>
                <ASelectOption value="12">12点</ASelectOption>
                <ASelectOption value="18">18点</ASelectOption>
                <ASelectOption value="20">20点</ASelectOption>
              </ASelect>
            </div>
            <div class="cron-row">
              <span class="cron-label">日</span>
              <ASelect
                v-model:value="cronParts.day"
                style="width: 120px"
                @change="updateCronFromParts"
              >
                <ASelectOption value="*">每天</ASelectOption>
                <ASelectOption value="1">1日</ASelectOption>
                <ASelectOption value="15">15日</ASelectOption>
              </ASelect>
            </div>
            <div class="cron-row">
              <span class="cron-label">月</span>
              <ASelect
                v-model:value="cronParts.month"
                style="width: 120px"
                @change="updateCronFromParts"
              >
                <ASelectOption value="*">每月</ASelectOption>
                <ASelectOption value="1">1月</ASelectOption>
                <ASelectOption value="6">6月</ASelectOption>
                <ASelectOption value="12">12月</ASelectOption>
              </ASelect>
            </div>
            <div class="cron-row">
              <span class="cron-label">周</span>
              <ASelect
                v-model:value="cronParts.week"
                style="width: 120px"
                @change="updateCronFromParts"
              >
                <ASelectOption value="?">不指定</ASelectOption>
                <ASelectOption value="MON-FRI">工作日</ASelectOption>
                <ASelectOption value="SUN,SAT">周末</ASelectOption>
                <ASelectOption value="MON">周一</ASelectOption>
                <ASelectOption value="FRI">周五</ASelectOption>
              </ASelect>
            </div>
          </div>

          <!-- Cron表达式显示 -->
          <div class="cron-expression">
            <span>Quartz Cron</span>
            <AInput
              v-model:value="formData.cron"
              placeholder="0 0 * * * ?"
              @change="parseCronToParts"
            >
            </AInput>
            <div class="cron-desc">{{ cronDescription }}</div>
          </div>

          <!-- 常用表达式快捷选择 -->
          <div class="cron-presets">
            <span class="preset-label">常用：</span>
            <ASpace wrap>
              <AButton type="link" size="small" @click="applyPreset('0 0 * * * ?')">每小时</AButton>
              <AButton type="link" size="small" @click="applyPreset('0 0 0 * * ?')">每天零点</AButton>
              <AButton type="link" size="small" @click="applyPreset('0 0 8 ? * MON-FRI')">工作日早8点</AButton>
              <AButton type="link" size="small" @click="applyPreset('0 0 9,18 ? * MON-FRI')">工作日早晚</AButton>
              <AButton type="link" size="small" @click="applyPreset('0 0 0 1 * ?')">每月1号</AButton>
              <AButton type="link" size="small" @click="applyPreset('0 */5 * * * ?')">每5分钟</AButton>
            </ASpace>
          </div>
        </div>
      </AFormItem>

      <!-- 是否启用 -->
      <AFormItem label="状态" name="enabled">
        <ASwitch
          v-model:checked="formData.enabled"
          checked-children="启用"
          un-checked-children="禁用"
        />
      </AFormItem>
    </AForm>

    <!-- 解绑按钮（仅在编辑模式下显示） -->
    <template #footer>
      <div class="modal-footer">
        <AButton
          v-if="isEdit"
          danger
          :loading="unbindLoading"
          @click="handleUnbind"
        >
          解绑定时任务
        </AButton>
        <div class="footer-right">
          <AButton @click="handleCancel">取消</AButton>
          <AButton type="primary" :loading="loading" @click="handleSubmit">确定</AButton>
        </div>
      </div>
    </template>
  </AModal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { JobInfo } from '@/types'
import * as jobApi from '@/api/job'

/**
 * Props定义
 */
const props = defineProps<{
  visible: boolean
  agentId: string
  jobInfo?: JobInfo | null
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
const unbindLoading = ref(false)

/**
 * 是否为编辑模式
 */
const isEdit = computed(() => !!props.jobInfo?.id)

/**
 * 弹窗标题
 */
const modalTitle = computed(() => isEdit.value ? '编辑定时任务' : '绑定定时任务')

/**
 * Cron表达式各部分
 */
const cronParts = ref({
  second: '0',
  minute: '0',
  hour: '*',
  day: '*',
  month: '*',
  week: '?'
})

/**
 * 表单数据
 */
const formData = ref({
  id: '',
  input: '',
  cron: '0 0 * * * ?',
  enabled: true
})

/**
 * 表单校验规则
 */
const formRules = {
  input: [{ required: true, message: '请输入输入内容', trigger: 'blur' }],
  cron: [{ required: true, message: '请输入Cron表达式', trigger: 'blur' }]
}

/**
 * Cron表达式描述
 */
const cronDescription = computed(() => {
  const cron = formData.value.cron
  if (!cron) return ''

  const parts = cron.split(' ')
  if (parts.length !== 6 && parts.length !== 7) return '格式不正确'

  // 简单的描述生成
  const desc: string[] = []
  const [second, minute, hour, day, month, week] = parts as [string, string, string, string, string, string]

  if (second === '0' && minute === '0' && hour === '0' && day === '*' && month === '*' && week === '?') {
    return '每天零点执行'
  }
  if (second === '0' && minute === '0' && hour === '*' && day === '*' && month === '*' && week === '?') {
    return '每小时执行'
  }
  if (second === '0' && minute === '*/5' && hour === '*' && day === '*' && month === '*' && week === '?') {
    return '每5分钟执行'
  }
  if (week === 'MON-FRI' || week === '2-6') {
    desc.push('工作日')
  } else if (week === 'SUN,SAT' || week === '1,7') {
    desc.push('周末')
  } else if (week === 'MON') {
    desc.push('周一')
  } else if (week === 'FRI') {
    desc.push('周五')
  }
  if (hour !== '*') {
    desc.push(`${hour}点`)
  }
  if (minute && minute !== '0' && minute !== '*') {
    if (minute.startsWith('*/')) {
      desc.push(`每${minute.replace('*/', '')}分钟`)
    } else {
      desc.push(`${minute}分`)
    }
  }

  return desc.length > 0 ? desc.join('，') : '自定义执行策略'
})

/**
 * 从Cron各部分更新表达式
 */
function updateCronFromParts() {
  const { second, minute, hour, day, month, week } = cronParts.value
  // Quartz规则：周和日不能同时指定，如果周不是'?'，则日必须是'?'
  let actualDay = day
  if (week !== '?' && day !== '?') {
    actualDay = '?'
  }
  formData.value.cron = `${second} ${minute} ${hour} ${actualDay} ${month} ${week}`
}

/**
 * 解析Cron表达式到各部分
 */
function parseCronToParts() {
  const cron = formData.value.cron
  if (!cron) return

  const parts = cron.split(' ')
  if (parts.length >= 6) {
    cronParts.value = {
      second: parts[0] || '0',
      minute: parts[1] || '0',
      hour: parts[2] || '*',
      day: parts[3] || '*',
      month: parts[4] || '*',
      week: parts[5] || '?'
    }
  }
}

/**
 * 应用预设表达式
 */
function applyPreset(cron: string) {
  formData.value.cron = cron
  parseCronToParts()
}

/**
 * 初始化表单数据
 */
function initFormData() {
  if (props.jobInfo?.id) {
    // 编辑模式
    formData.value.id = props.jobInfo.id
    formData.value.cron = props.jobInfo.cron || '0 0 * * * ?'
    formData.value.enabled = props.jobInfo.enabled ?? true

    // 解析dataMap获取input
    try {
      const dataMap = props.jobInfo.dataMap ? JSON.parse(props.jobInfo.dataMap) : {}
      formData.value.input = dataMap.input || ''
    } catch {
      formData.value.input = ''
    }
  } else {
    // 新增模式
    formData.value.id = ''
    formData.value.input = ''
    formData.value.cron = '0 0 * * * ?'
    formData.value.enabled = true
  }
  parseCronToParts()
}

/**
 * 处理提交
 */
async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true

  try {
    // 构建dataMap
    const dataMap = JSON.stringify({
      agentId: props.agentId,
      input: formData.value.input
    })

    const jobInfo: JobInfo = {
      id: formData.value.id,
      type: 'AGENT',
      bizId: props.agentId,
      cron: formData.value.cron,
      jobClass: 'com.hxh.apboa.job.scheduler.AgentScheduler',
      dataMap: dataMap,
      enabled: formData.value.enabled
    }

    if (isEdit.value) {
      await jobApi.update(jobInfo)
      message.success('定时任务更新成功')
    } else {
      await jobApi.add(jobInfo)
      message.success('定时任务绑定成功')
    }

    emit('success')
    emit('update:visible', false)
  } catch (error) {
    console.error('保存定时任务失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 处理取消
 */
function handleCancel() {
  emit('update:visible', false)
}

/**
 * 处理解绑
 */
function handleUnbind() {
  Modal.confirm({
    title: '确认解绑',
    content: '解绑后将删除该定时任务，是否继续？',
    okText: '确认解绑',
    okType: 'danger',
    onOk: async () => {
      unbindLoading.value = true
      try {
        await jobApi.deleteByBizId(props.agentId)
        message.success('解绑成功')
        emit('success')
        emit('update:visible', false)
      } catch (error) {
        console.error('解绑失败:', error)
      } finally {
        unbindLoading.value = false
      }
    }
  })
}

/**
 * 监听visible变化
 */
watch(() => props.visible, (val) => {
  if (val) {
    initFormData()
  }
})
</script>

<style scoped lang="scss">
.job-form {
  padding: 8px 0;
}

.cron-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cron-generator {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  padding: 16px;
  background-color: var(--color-bg-container);
  border-radius: var(--border-radius-base);
  border: 1px solid var(--color-border);
}

.cron-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cron-label {
  width: 32px;
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.cron-expression {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.cron-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  padding-left: 8px;
}

.cron-presets {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px;
  background-color: var(--color-bg-container);
  border-radius: var(--border-radius-base);
  border: 1px solid var(--color-border);
}

.preset-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.footer-right {
  display: flex;
  gap: 8px;
}

@media (max-width: 576px) {
  .cron-generator {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
