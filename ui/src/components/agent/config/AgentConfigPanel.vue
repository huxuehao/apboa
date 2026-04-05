/**
 * 智能体配置面板主组件
 * 聚合编辑、定时、架构图、历史对话、访问API、统计分析
 *
 * @component
 */
<script setup lang="ts">
import { ref, computed } from 'vue'
import { Modal } from 'ant-design-vue'
import {
  SettingOutlined,
  ClockCircleOutlined,
  ApartmentOutlined,
  HistoryOutlined,
  ApiOutlined,
  BarChartOutlined,
  RobotOutlined
} from '@ant-design/icons-vue'
import type { AgentDefinitionVO } from '@/types'
import AgentConfigEdit from './AgentConfigEdit.vue'
import AgentConfigSchedule from './AgentConfigSchedule.vue'
import AgentConfigArchitecture from './AgentConfigArchitecture.vue'
import AgentConfigHistory from './AgentConfigHistory.vue'
import AgentConfigApiDoc from './AgentConfigApiDoc.vue'
import AgentConfigStatistics from './AgentConfigStatistics.vue'

const props = defineProps<{
  visible: boolean
  agentData?: AgentDefinitionVO
  tags: string[]
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  success: []
}>()

/**
 * 导航配置
 */
const navItems = computed(() => {
  const items = [
    { key: 'edit', label: 'Agent配置', icon: SettingOutlined },
    { key: 'schedule', label: '定时任务', icon: ClockCircleOutlined }
  ]

  // 架构图仅对自定义智能体显示
  if (props.agentData?.agentType === 'CUSTOM') {
    items.push({ key: 'architecture', label: 'Agent架构', icon: ApartmentOutlined })
  }

  items.push(
    { key: 'history', label: '对话日志', icon: HistoryOutlined },
    { key: 'api', label: '访问 API', icon: ApiOutlined },
    { key: 'statistics', label: '统计监测', icon: BarChartOutlined }
  )

  return items
})

const activeKey = ref('edit')
const editRef = ref<InstanceType<typeof AgentConfigEdit>>()
const scheduleRef = ref<InstanceType<typeof AgentConfigSchedule>>()

/**
 * 检查是否有未保存的数据
 */
function checkUnsaved(): boolean {
  if (activeKey.value === 'edit' && editRef.value?.isDirty) return true
  if (activeKey.value === 'schedule' && scheduleRef.value?.isDirty) return true
  return false
}

/**
 * 切换导航项
 */
function handleNavClick(key: string) {
  if (key === activeKey.value) return

  if (checkUnsaved()) {
    Modal.confirm({
      title: '未保存的更改',
      content: '当前页面有未保存的更改，切换后将丢失这些更改。是否继续？',
      okText: '继续切换',
      cancelText: '留在当前',
      onOk: () => {
        activeKey.value = key
      }
    })
    return
  }

  activeKey.value = key
}

/**
 * 关闭面板
 */
function handleClose() {
  if (checkUnsaved()) {
    Modal.confirm({
      title: '未保存的更改',
      content: '当前页面有未保存的更改，关闭后将丢失这些更改。是否继续？',
      okText: '关闭',
      cancelText: '取消',
      onOk: () => {
        doClose()
      }
    })
    return
  }
  doClose()
}

function doClose() {
  activeKey.value = 'edit'
  emit('update:visible', false)
}

function handleSuccess() {
  emit('success')
}

/**
 * 智能体ID
 */
const agentId = computed(() => props.agentData?.id ? String(props.agentData.id) : '')
const agentCode = computed(() => props.agentData?.agentCode || '')
</script>

<template>
  <ApboaModal
    :open="visible"
    :title-icon="RobotOutlined"
    :title="agentData?.name ? `${agentData.name} - 设计` : '智能体设计'"
    :footer="null"
    defaultWidth="100%"
    destroyOnClose
    @cancel="handleClose"
  >
    <div class="agent-config-panel">
      <!-- 左侧导航 -->
      <nav class="config-nav">
        <ul class="config-nav-list">
          <li
            v-for="item in navItems"
            :key="item.key"
            class="config-nav-item"
            :class="{ active: activeKey === item.key }"
            @click="handleNavClick(item.key)"
          >
            <div class="nav-icon">
              <component :is="item.icon" />
            </div>
            <span class="nav-label">{{ item.label }}</span>
          </li>
        </ul>
      </nav>

      <!-- 右侧内容 -->
      <div class="config-content">
        <AgentConfigEdit
          v-if="activeKey === 'edit' && agentData"
          ref="editRef"
          :agent-data="agentData"
          :tags="tags"
          @success="handleSuccess"
        />

        <AgentConfigSchedule
          v-if="activeKey === 'schedule' && agentId"
          ref="scheduleRef"
          :agent-id="agentId"
          @success="handleSuccess"
        />

        <AgentConfigArchitecture
          v-if="activeKey === 'architecture' && agentId"
          :agent-id="agentId"
        />

        <AgentConfigHistory
          v-if="activeKey === 'history' && agentId"
          :agent-id="agentId"
        />

        <AgentConfigApiDoc
          v-if="activeKey === 'api' && agentCode"
          :agent-code="agentCode"
        />

        <AgentConfigStatistics
          v-if="activeKey === 'statistics' && agentId"
          :agent-id="agentId"
        />
      </div>
    </div>
  </ApboaModal>
</template>

<style scoped lang="scss">
@use '@/styles/agent/config-panel.scss' as *;
</style>
