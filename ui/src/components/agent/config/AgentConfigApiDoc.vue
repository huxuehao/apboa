/**
 * 智能体配置-访问 API 文档子组件
 *
 * @component
 */
<script setup lang="ts">
import { ref, computed } from 'vue'
import { CopyOutlined, CheckOutlined, DownOutlined, RightOutlined, KeyOutlined, LinkOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const props = defineProps<{
  agentCode: string
}>()

/**
 * 访问路径
 */
const accessUrl = computed(() => {
  const loc = window.location
  return `${loc.protocol}//${loc.host}/api/apboa/agui/${props.agentCode}`
})

/**
 * 复制到剪贴板
 */
const copiedKey = ref('')
async function copyToClipboard(text: string, key: string) {
  try {
    await navigator.clipboard.writeText(text)
    copiedKey.value = key
    message.success('已复制')
    setTimeout(() => { copiedKey.value = '' }, 2000)
  } catch {
    message.error('复制失败')
  }
}

/**
 * 展开/收起的接口
 */
const expandedEndpoints = ref<Set<string>>(new Set())
function toggleEndpoint(id: string) {
  if (expandedEndpoints.value.has(id)) {
    expandedEndpoints.value.delete(id)
  } else {
    expandedEndpoints.value.add(id)
  }
}

/**
 * 智能体对话接口请求体示例
 */
const aguiBodyExample = `{
  "threadId": "2038965802636013570",
  "runId": "run_1775396544170_0c84jdg37",
  "messages": [
    {
      "id": "undefined",
      "role": "user",
      "content": "你好"
    }
  ],
  "state": {},
  "tools": [],
  "context": [],
  "forwardedProps": {
    "agentId": "2022979139770359809",
    "agentCode": "test_agent",
    "fileIds": [],
    "memoryActive": false,
    "planActive": false
  }
}`

/**
 * 接口定义
 */
const endpoints = [
  {
    id: 'create-session',
    method: 'POST',
    path: '/api/agent/chat/session',
    desc: '创建新会话',
    note: '创建一个新的对话会话，系统会自动插入根消息并设置 current_message_id。',
    params: [
      { name: 'agentId', type: 'string', required: true, desc: '智能体ID' },
      { name: 'title', type: 'string', required: false, desc: '会话标题，默认"新对话"' }
    ],
    bodyExample: '{\n  "agentId": "123456",\n  "title": "测试对话"\n}',
    responseExample: '{\n  "code": 200,\n  "success": true,\n  "data": {\n    "id": "789",\n    "userId": "1",\n    "agentId": "123456",\n    "title": "测试对话",\n    "isPinned": false\n  }\n}'
  },
  {
    id: 'append-message',
    method: 'POST',
    path: '/api/agent/chat/session/{sessionId}/message',
    desc: '追加消息',
    note: '在当前对话的 current_message_id 后追加新消息，并更新游标。',
    params: [
      { name: 'sessionId', type: 'Long (路径参数)', required: true, desc: '会话ID' },
      { name: 'role', type: 'string', required: true, desc: '消息角色：user / assistant' },
      { name: 'content', type: 'string', required: true, desc: '消息内容' }
    ],
    bodyExample: '{\n  "role": "user",\n  "content": "你好，请帮我分析一下数据"\n}',
    responseExample: '{\n  "code": 200,\n  "success": true,\n  "data": {\n    "id": "10",\n    "sessionId": "789",\n    "role": "user",\n    "content": "你好，请帮我分析一下数据",\n    "depth": 1\n  }\n}'
  },
  {
    id: 'regenerate',
    method: 'POST',
    path: '/api/agent/chat/session/{sessionId}/regenerate',
    desc: '重新生成（新分支）',
    note: '以当前消息为父节点创建新分支消息，适用于重新生成回复的场景。',
    params: [
      { name: 'sessionId', type: 'Long (路径参数)', required: true, desc: '会话ID' },
      { name: 'role', type: 'string', required: true, desc: '消息角色' },
      { name: 'content', type: 'string', required: true, desc: '重新生成的内容' }
    ],
    bodyExample: '{\n  "role": "assistant",\n  "content": "这是重新生成的回复"\n}',
    responseExample: null
  },
  {
    id: 'switch-branch',
    method: 'PUT',
    path: '/api/agent/chat/session/{sessionId}/current?messageId=xxx',
    desc: '切换历史分支',
    note: '仅更新 current_message_id 指针，切换到历史对话分支。不会创建新消息。',
    params: [
      { name: 'sessionId', type: 'Long (路径参数)', required: true, desc: '会话ID' },
      { name: 'messageId', type: 'Integer (查询参数)', required: true, desc: '目标消息ID' }
    ],
    bodyExample: null,
    responseExample: null
  },
  {
    id: 'current-messages',
    method: 'GET',
    path: '/api/agent/chat/session/{sessionId}/messages/current',
    desc: '获取当前完整对话',
    note: '根据 current_message_id 回溯路径，返回完整的消息链，按深度升序排列。',
    params: [
      { name: 'sessionId', type: 'Long (路径参数)', required: true, desc: '会话ID' }
    ],
    bodyExample: null,
    responseExample: '{\n  "code": 200,\n  "success": true,\n  "data": [\n    { "id": "1", "role": "system", "content": "", "depth": 0 },\n    { "id": "2", "role": "user", "content": "你好", "depth": 1 },\n    { "id": "3", "role": "assistant", "content": "你好！", "depth": 2 }\n  ]\n}'
  },
  {
    id: 'list-sessions',
    method: 'GET',
    path: '/api/agent/chat/session/list',
    desc: '会话列表',
    note: '获取当前用户的会话列表，可按 agentId 筛选，按置顶和更新时间倒序排列。',
    params: [
      { name: 'agentId', type: 'Long (查询参数)', required: false, desc: '按智能体ID筛选' }
    ],
    bodyExample: null,
    responseExample: null
  },
  {
    id: 'page-sessions',
    method: 'GET',
    path: '/api/agent/chat/session/page',
    desc: '分页查询会话',
    note: '支持分页查询，可按 isPinned 筛选置顶会话。',
    params: [
      { name: 'agentId', type: 'Long (查询参数)', required: false, desc: '按智能体ID筛选' },
      { name: 'isPinned', type: 'Boolean (查询参数)', required: false, desc: '按置顶状态筛选' },
      { name: 'current', type: 'Integer (查询参数)', required: false, desc: '页码' },
      { name: 'size', type: 'Integer (查询参数)', required: false, desc: '每页条数' }
    ],
    bodyExample: null,
    responseExample: null
  },
  {
    id: 'session-detail',
    method: 'GET',
    path: '/api/agent/chat/session/{id}',
    desc: '会话详情',
    note: '获取指定会话的详细信息。',
    params: [
      { name: 'id', type: 'Long (路径参数)', required: true, desc: '会话ID' }
    ],
    bodyExample: null,
    responseExample: null
  },
  {
    id: 'pin-session',
    method: 'PUT',
    path: '/api/agent/chat/session/{id}/pin',
    desc: '置顶会话',
    note: '将指定会话设为置顶状态。',
    params: [
      { name: 'id', type: 'Long (路径参数)', required: true, desc: '会话ID' }
    ],
    bodyExample: null,
    responseExample: null
  },
  {
    id: 'unpin-session',
    method: 'PUT',
    path: '/api/agent/chat/session/{id}/unpin',
    desc: '取消置顶会话',
    note: '取消指定会话的置顶状态。',
    params: [
      { name: 'id', type: 'Long (路径参数)', required: true, desc: '会话ID' }
    ],
    bodyExample: null,
    responseExample: null
  },
  {
    id: 'update-title',
    method: 'PUT',
    path: '/api/agent/chat/session/{id}/title?title=xxx',
    desc: '更新会话标题',
    note: '修改指定会话的标题。',
    params: [
      { name: 'id', type: 'Long (路径参数)', required: true, desc: '会话ID' },
      { name: 'title', type: 'String (查询参数)', required: true, desc: '新标题' }
    ],
    bodyExample: null,
    responseExample: null
  },
  {
    id: 'delete-session',
    method: 'DELETE',
    path: '/api/agent/chat/session/{id}',
    desc: '删除会话',
    note: '物理删除会话及其所有消息，操作不可逆。',
    params: [
      { name: 'id', type: 'Long (路径参数)', required: true, desc: '会话ID' }
    ],
    bodyExample: null,
    responseExample: null
  }
]
</script>

<template>
  <div class="api-doc">
    <!-- 访问入口 -->
    <div class="api-doc-section">
      <div class="api-doc-title">
        <LinkOutlined style="margin-right: 8px;" />访问入口
      </div>

      <div class="api-info-box info">
        <div style="margin-bottom: 8px; font-weight: 600;">智能体对话接口</div>
        <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 4px;">
          <span class="method-badge post">POST</span>
          <code style="font-size: 13px; word-break: break-all;">{{ accessUrl }}</code>
          <AButton
            type="text"
            size="small"
            @click="copyToClipboard(accessUrl, 'url')"
          >
            <template #icon>
              <CheckOutlined v-if="copiedKey === 'url'" style="color: #52c41a;" />
              <CopyOutlined v-else />
            </template>
          </AButton>
        </div>
        <div style="font-size: 12px; color: #546e7a; margin-top: 4px;">
          该接口为智能体的主要对话入口，支持流式和非流式响应。
        </div>

        <div class="endpoint-detail-title" style="margin-top: 12px;">Request Body</div>
        <table class="param-table">
          <thead>
            <tr>
              <th>参数名</th>
              <th>类型</th>
              <th>必填</th>
              <th>说明</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="param-name">threadId</td>
              <td class="param-type">string</td>
              <td><span class="param-required">Required</span></td>
              <td>会话线程ID，用于标识一次完整的对话</td>
            </tr>
            <tr>
              <td class="param-name">runId</td>
              <td class="param-type">string</td>
              <td><span class="param-required">Required</span></td>
              <td>本次运行ID，系统自动生成的唯一标识</td>
            </tr>
            <tr>
              <td class="param-name">messages</td>
              <td class="param-type">array</td>
              <td><span class="param-required">Required</span></td>
              <td>消息列表，包含 id、role（user/assistant）、content 字段</td>
            </tr>
            <tr>
              <td class="param-name">state</td>
              <td class="param-type">object</td>
              <td><span style="color: #bfbfbf;">Optional</span></td>
              <td>对话状态对象，可用于传递上下文状态</td>
            </tr>
            <tr>
              <td class="param-name">tools</td>
              <td class="param-type">array</td>
              <td><span style="color: #bfbfbf;">Optional</span></td>
              <td>可用工具列表</td>
            </tr>
            <tr>
              <td class="param-name">context</td>
              <td class="param-type">array</td>
              <td><span style="color: #bfbfbf;">Optional</span></td>
              <td>上下文信息列表</td>
            </tr>
            <tr>
              <td class="param-name">forwardedProps</td>
              <td class="param-type">object</td>
              <td><span class="param-required">Required</span></td>
              <td>转发属性，包含 agentId、agentCode、fileIds、memoryActive、planActive 等扩展参数</td>
            </tr>
          </tbody>
        </table>

        <div class="endpoint-detail-title" style="margin-top: 12px;">Request Example</div>
        <div class="code-block" style="margin: 0;">{{ aguiBodyExample }}<span
          class="code-copy-btn"
          style="position: absolute; top: 8px; right: 8px; cursor: pointer;"
          @click="copyToClipboard(aguiBodyExample, 'agui-body')"
        >
          <CheckOutlined v-if="copiedKey === 'agui-body'" style="color: #a6e3a1;" />
          <CopyOutlined v-else style="color: #a6adc8;" />
        </span></div>
      </div>
    </div>

    <!-- 鉴权说明 -->
    <div class="api-doc-section">
      <div class="api-doc-title">
        <KeyOutlined style="margin-right: 8px;" />鉴权方式
      </div>

      <div class="api-info-box warning">
        <div style="margin-bottom: 8px; font-weight: 600;">API Key 鉴权</div>
        <div style="margin-bottom: 8px;">所有接口请求需要在请求头中携带 API Key 进行身份验证：</div>
        <div class="code-block" style="margin: 0;">Authorization: {API_KEY}<span
          class="code-copy-btn"
          style="position: absolute; top: 8px; right: 8px; cursor: pointer;"
          @click="copyToClipboard('Authorization: {API_KEY}', 'auth')"
        >
          <CheckOutlined v-if="copiedKey === 'auth'" style="color: #a6e3a1;" />
          <CopyOutlined v-else style="color: #a6adc8;" />
        </span></div>
        <div style="font-size: 12px; color: #795548; margin-top: 8px;">
          API Key 可在系统设置 > API Keys 中创建和管理。请妥善保管您的 API Key，不要在客户端代码中暴露。
        </div>
      </div>
    </div>

    <!-- 接口列表 -->
    <div class="api-doc-section">
      <div class="api-doc-title">
        <ThunderboltOutlined style="margin-right: 8px;" />会话管理接口
      </div>

      <div
        v-for="ep in endpoints"
        :key="ep.id"
        class="api-endpoint-card"
      >
        <div class="endpoint-header" @click="toggleEndpoint(ep.id)">
          <component
            :is="expandedEndpoints.has(ep.id) ? DownOutlined : RightOutlined"
            style="font-size: 10px; color: #bfbfbf;"
          />
          <span class="method-badge" :class="ep.method.toLowerCase()">{{ ep.method }}</span>
          <span class="endpoint-path">{{ ep.path }}</span>
          <span class="endpoint-desc">{{ ep.desc }}</span>
        </div>

        <div v-if="expandedEndpoints.has(ep.id)" class="endpoint-body">
          <div v-if="ep.note" style="font-size: 13px; color: var(--color-text-secondary); margin-top: 12px; margin-bottom: 8px;">
            {{ ep.note }}
          </div>

          <!-- 参数表 -->
          <div v-if="ep.params.length > 0">
            <div class="endpoint-detail-title">Parameters</div>
            <table class="param-table">
              <thead>
                <tr>
                  <th>参数名</th>
                  <th>类型</th>
                  <th>必填</th>
                  <th>说明</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="p in ep.params" :key="p.name">
                  <td class="param-name">{{ p.name }}</td>
                  <td class="param-type">{{ p.type }}</td>
                  <td><span v-if="p.required" class="param-required">Required</span><span v-else style="color: #bfbfbf;">Optional</span></td>
                  <td>{{ p.desc }}</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 请求体示例 -->
          <template v-if="ep.bodyExample">
            <div class="endpoint-detail-title">Request Body</div>
            <div class="code-block">{{ ep.bodyExample }}<span
              class="code-copy-btn"
              style="position: absolute; top: 8px; right: 8px; cursor: pointer;"
              @click="copyToClipboard(ep.bodyExample!, `body-${ep.id}`)"
            >
              <CheckOutlined v-if="copiedKey === `body-${ep.id}`" style="color: #a6e3a1;" />
              <CopyOutlined v-else style="color: #a6adc8;" />
            </span></div>
          </template>

          <!-- 响应示例 -->
          <template v-if="ep.responseExample">
            <div class="endpoint-detail-title">Response</div>
            <div class="code-block">{{ ep.responseExample }}<span
              class="code-copy-btn"
              style="position: absolute; top: 8px; right: 8px; cursor: pointer;"
              @click="copyToClipboard(ep.responseExample!, `resp-${ep.id}`)"
            >
              <CheckOutlined v-if="copiedKey === `resp-${ep.id}`" style="color: #a6e3a1;" />
              <CopyOutlined v-else style="color: #a6adc8;" />
            </span></div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/agent/config-panel.scss' as *;
</style>
