/**
 * 智能体配置-历史对话子组件
 * 分页会话列表 + 对话详情展示
 *
 * @component
 */
<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { PushpinOutlined } from '@ant-design/icons-vue'
import * as chatSessionApi from '@/api/chatSession'
import MessageList from '@/components/chatHistory/MessageList.vue'
import type {ChatSessionVO, ChatMessageVO, DisplayMessage} from '@/types'
import dayjs from 'dayjs'

const props = defineProps<{
  agentId: string
}>()

const sessions = ref<ChatSessionVO[]>([])
const totalSessions = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const listLoading = ref(false)

const selectedSessionId = ref<string | null>(null)
const messages = ref<ChatMessageVO[]>([])
const detailLoading = ref(false)
const showDetail = ref(false)

const selectedSession = computed(() =>
  sessions.value.find(s => String(s.id) === selectedSessionId.value)
)

/**
 * 加载会话列表
 */
async function loadSessions() {
  listLoading.value = true
  try {
    const res = await chatSessionApi.pageSessions({
      agentId: props.agentId,
      current: currentPage.value,
      size: pageSize.value
    })
    const page = res.data.data
    sessions.value = page.records || []
    totalSessions.value = page.total || 0
  } catch (e) {
    console.error('加载会话列表失败:', e)
  } finally {
    listLoading.value = false
  }
}

/**
 * 选中会话，加载对话详情
 */
async function handleSelectSession(session: ChatSessionVO) {
  selectedSessionId.value = String(session.id)
  detailLoading.value = true
  showDetail.value = true
  try {
    const res = await chatSessionApi.getCurrentMessages(String(session.id))
    messages.value = res.data.data || []
  } catch (e) {
    console.error('加载对话内容失败:', e)
    messages.value = []
  } finally {
    detailLoading.value = false
  }
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadSessions()
}

function formatTime(time: string) {
  if (!time) return ''
  return dayjs(time).format('MM-DD HH:mm')
}

/**
 * 过滤掉 system 根消息
 */
const visibleMessages = computed(() =>
  messages.value.filter(m => !(m.role === 'system' && m.depth === 0 && !m.content))
)

onMounted(() => loadSessions())
</script>

<template>
  <div class="history-container">
    <!-- 会话列表 -->
    <div class="history-list-panel">
      <div class="history-list-inner">
        <div style="padding: 12px 0;">
          <div style="font-size: 15px; font-weight: 600; color: var(--color-text-primary); margin-bottom: 12px; text-align: center;">
            会话列表
            <ATag color="processing"  style="margin-left: 8px;">{{ totalSessions }}</ATag>
          </div>
        </div>

        <ASpin :spinning="listLoading">
          <div v-if="sessions.length === 0 && !listLoading" style="padding: 40px 0; text-align: center;">
            <AEmpty description="暂无会话记录" />
          </div>

          <div v-else class="history-sessions">
            <div
              v-for="session in sessions"
              :key="session.id"
              class="history-session-item"
              :class="{ active: String(session.id) === selectedSessionId }"
              @click="handleSelectSession(session)"
            >
              <div>
                <PushpinOutlined v-if="session.isPinned" style="margin-right: 6px; color: #4449d0;" />
                {{ session.title || '未命名会话' }}
              </div>
              <div class="session-meta">
                <span>创建于 {{ formatTime(session.createdAt) }}</span>
                <span style="margin-left: 12px;">更新于 {{ formatTime(session.updatedAt) }}</span>
              </div>
            </div>
          </div>
        </ASpin>

        <div v-if="totalSessions > pageSize" style="padding: 16px 0; display: flex; justify-content: center;">
          <APagination
            :current="currentPage"
            :total="totalSessions"
            :page-size="pageSize"
            size="small"
            @change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 对话详情 -->
    <div class="history-detail-panel">
      <ASpin :spinning="detailLoading">
        <div v-if="visibleMessages.length === 0 && !detailLoading" style="text-align: center;margin-top: 250px">
          <AEmpty description="暂无对话内容" />
        </div>
        <MessageList :messages="visibleMessages as DisplayMessage[]"/>
      </ASpin>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/agent/config-panel.scss' as *;
</style>
