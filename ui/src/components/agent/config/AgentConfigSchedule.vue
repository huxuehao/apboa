/**
 * 智能体配置-定时任务子组件
 * 内嵌展示定时任务配置（非弹窗形式）
 *
 * @component
 */
<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { JobInfo } from '@/types'
import * as jobApi from '@/api/job'

const props = defineProps<{
  agentId: string
}>()

const emit = defineEmits<{
  success: []
}>()

const formRef = ref()
const loading = ref(false)
const unbindLoading = ref(false)
const initialLoading = ref(true)
const isDirty = ref(false)
const jobInfo = ref<JobInfo | null>(null)

const isEdit = computed(() => !!jobInfo.value?.id)

const cronParts = ref({
  second: '0',
  minute: '0',
  hour: '*',
  day: '*',
  month: '*',
  week: '?'
})

const formData = ref({
  id: '',
  input: '',
  cron: '0 0 * * * ?',
  enabled: true
})

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

  const [second, minute, hour, day, month, week] = parts as [string, string, string, string, string, string]
  if (second === '0' && minute === '0' && hour === '0' && day === '*' && month === '*' && week === '?') return '每天零点执行'
  if (second === '0' && minute === '0' && hour === '*' && day === '*' && month === '*' && week === '?') return '每小时执行'
  if (second === '0' && minute === '*/5' && hour === '*' && day === '*' && month === '*' && week === '?') return '每5分钟执行'

  const desc: string[] = []
  if (week === 'MON-FRI') desc.push('工作日')
  else if (week === 'SUN,SAT') desc.push('周末')
  if (hour !== '*') desc.push(`${hour}点`)
  if (minute && minute !== '0' && minute !== '*') {
    desc.push(minute.startsWith('*/') ? `每${minute.replace('*/', '')}分钟` : `${minute}分`)
  }
  return desc.length > 0 ? desc.join('，') : '自定义执行策略'
})

function updateCronFromParts() {
  const { second, minute, hour, day, month, week } = cronParts.value
  let actualDay = day
  if (week !== '?' && day !== '?') actualDay = '?'
  formData.value.cron = `${second} ${minute} ${hour} ${actualDay} ${month} ${week}`
}

function parseCronToParts() {
  const parts = formData.value.cron?.split(' ')
  if (parts && parts.length >= 6) {
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

function applyPreset(cron: string) {
  formData.value.cron = cron
  parseCronToParts()
}

/**
 * 加载当前定时任务
 */
async function loadJobInfo() {
  initialLoading.value = true
  try {
    const response = await jobApi.getByBizId(props.agentId)
    jobInfo.value = response.data.data || null
    initFormData()
  } catch {
    jobInfo.value = null
    initFormData()
  } finally {
    initialLoading.value = false
  }
}

function initFormData() {
  if (jobInfo.value?.id) {
    formData.value.id = jobInfo.value.id
    formData.value.cron = jobInfo.value.cron || '0 0 * * * ?'
    formData.value.enabled = jobInfo.value.enabled ?? true
    try {
      const dataMap = jobInfo.value.dataMap ? JSON.parse(jobInfo.value.dataMap) : {}
      formData.value.input = dataMap.input || ''
    } catch {
      formData.value.input = ''
    }
  } else {
    formData.value.id = ''
    formData.value.input = ''
    formData.value.cron = '0 0 * * * ?'
    formData.value.enabled = true
  }
  parseCronToParts()
  isDirty.value = false
}

watch(formData, () => { isDirty.value = true }, { deep: true })

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch { return }

  loading.value = true
  try {
    const dataMap = JSON.stringify({ agentId: props.agentId, input: formData.value.input })
    const info: JobInfo = {
      id: formData.value.id,
      type: 'AGENT',
      bizId: props.agentId,
      cron: formData.value.cron,
      jobClass: 'com.hxh.apboa.job.scheduler.AgentScheduler',
      dataMap,
      enabled: formData.value.enabled
    }

    if (isEdit.value) {
      await jobApi.update(info)
      message.success('定时任务更新成功')
    } else {
      await jobApi.add(info)
      message.success('定时任务绑定成功')
    }
    isDirty.value = false
    emit('success')
    loadJobInfo()
  } catch (error) {
    console.error('保存定时任务失败:', error)
  } finally {
    loading.value = false
  }
}

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
        isDirty.value = false
        emit('success')
        loadJobInfo()
      } catch (error) {
        console.error('解绑失败:', error)
      } finally {
        unbindLoading.value = false
      }
    }
  })
}

onMounted(() => loadJobInfo())

defineExpose({ isDirty })
</script>

<template>
  <div class="config-schedule">
    <ASpin :spinning="initialLoading">
      <AForm ref="formRef" :model="formData" :rules="formRules" layout="vertical" class="config-schedule-form">
        <AFormItem label="输入内容" name="input">
          <ATextarea
            v-model:value="formData.input"
            placeholder="请输入定时任务触发时发送给智能体的消息内容"
            :rows="4"
            show-count
            :maxlength="500"
          />
        </AFormItem>

        <AFormItem label="执行策略" name="cron" required>
          <div class="cron-section">
            <div class="cron-generator">
              <div class="cron-row">
                <span class="cron-label">秒</span>
                <ASelect v-model:value="cronParts.second" style="width: 120px" @change="updateCronFromParts">
                  <ASelectOption value="0">0秒</ASelectOption>
                  <ASelectOption value="*/5">每5秒</ASelectOption>
                  <ASelectOption value="*/10">每10秒</ASelectOption>
                  <ASelectOption value="*/30">每30秒</ASelectOption>
                </ASelect>
              </div>
              <div class="cron-row">
                <span class="cron-label">分</span>
                <ASelect v-model:value="cronParts.minute" style="width: 120px" @change="updateCronFromParts">
                  <ASelectOption value="*">每分</ASelectOption>
                  <ASelectOption value="0">0分</ASelectOption>
                  <ASelectOption value="*/5">每5分</ASelectOption>
                  <ASelectOption value="*/10">每10分</ASelectOption>
                  <ASelectOption value="*/30">每30分</ASelectOption>
                </ASelect>
              </div>
              <div class="cron-row">
                <span class="cron-label">时</span>
                <ASelect v-model:value="cronParts.hour" style="width: 120px" @change="updateCronFromParts">
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
                <ASelect v-model:value="cronParts.day" style="width: 120px" @change="updateCronFromParts">
                  <ASelectOption value="*">每天</ASelectOption>
                  <ASelectOption value="1">1日</ASelectOption>
                  <ASelectOption value="15">15日</ASelectOption>
                </ASelect>
              </div>
              <div class="cron-row">
                <span class="cron-label">月</span>
                <ASelect v-model:value="cronParts.month" style="width: 120px" @change="updateCronFromParts">
                  <ASelectOption value="*">每月</ASelectOption>
                  <ASelectOption value="1">1月</ASelectOption>
                  <ASelectOption value="6">6月</ASelectOption>
                  <ASelectOption value="12">12月</ASelectOption>
                </ASelect>
              </div>
              <div class="cron-row">
                <span class="cron-label">周</span>
                <ASelect v-model:value="cronParts.week" style="width: 120px" @change="updateCronFromParts">
                  <ASelectOption value="?">不指定</ASelectOption>
                  <ASelectOption value="MON-FRI">工作日</ASelectOption>
                  <ASelectOption value="SUN,SAT">周末</ASelectOption>
                  <ASelectOption value="MON">周一</ASelectOption>
                  <ASelectOption value="FRI">周五</ASelectOption>
                </ASelect>
              </div>
            </div>

            <div class="cron-expression">
              <span>Quartz Cron</span>
              <AInput v-model:value="formData.cron" placeholder="0 0 * * * ?" @change="parseCronToParts" />
              <div class="cron-desc">{{ cronDescription }}</div>
            </div>

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

        <AFormItem label="状态" name="enabled">
          <ASwitch v-model:checked="formData.enabled" checked-children="启用" un-checked-children="禁用" />
        </AFormItem>
      </AForm>

      <div class="config-schedule-actions flex justify-between mt-lg" style="padding-top: 16px;">
        <AButton danger :disabled="!isEdit" :loading="unbindLoading" @click="handleUnbind">解绑定时任务</AButton>
        <AButton type="primary" :loading="loading" @click="handleSubmit">保存</AButton>
      </div>
    </ASpin>
  </div>
</template>

<style scoped lang="scss">
.config-schedule {
  height: 100%;
  background-color: #FFFFFF;
  padding: 12px 300px;
}
.config-schedule-form {
  height: calc(100vh - 180px);
  overflow: auto;
  padding-right: 10px
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
</style>
