/**
 * RAG检索测试组件
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  SearchOutlined,
  SettingOutlined,
  ClearOutlined,
  ClockCircleOutlined,
  LoadingOutlined,
  FileTextOutlined,
  NumberOutlined,
  ExperimentOutlined
} from '@ant-design/icons-vue'
import * as ragApi from '@/api/rag'

const props = defineProps<{
  knowledgeBaseConfigId: string
}>()

const searchQuery = ref('')
const searching = ref(false)
const searchResults = ref<Record<string, unknown>[]>([])
const hasSearched = ref(false)

/** 高级参数 */
const topK = ref(5)
const scoreThreshold = ref(0.3)
const showAdvanced = ref(false)

/** 检索历史 */
const searchHistory = ref<string[]>(
  loadSearchHistory()
)

/**
 * 是否有检索结果
 */
const resultCount = computed(() => searchResults.value.length)

/**
 * 获取相似度分数颜色
 */
function getScoreColor(score: number): string {
  if (score >= 0.7) return '#52c41a'
  if (score >= 0.5) return '#1677ff'
  if (score >= 0.3) return '#faad14'
  return '#ff4d4f'
}

/**
 * 获取相似度分数百分比宽度
 */
function getScoreWidth(score: number): string {
  return Math.round((score || 0) * 100) + '%'
}

/**
 * 执行检索
 */
async function handleSearch() {
  if (!searchQuery.value.trim()) {
    message.warning('请输入检索内容')
    return
  }

  searching.value = true
  hasSearched.value = true

  if (!props.knowledgeBaseConfigId) {
    message.warning('知识库配置ID不存在')
    searching.value = false
    return
  }

  try {
    const response = await ragApi.search({
      knowledgeBaseConfigId: props.knowledgeBaseConfigId,
      query: searchQuery.value,
      limit: topK.value,
      scoreThreshold: scoreThreshold.value
    })
    searchResults.value = response.data.data || []
    addToHistory(searchQuery.value)
  } finally {
    searching.value = false
  }
}

/**
 * 加载检索历史
 */
function loadSearchHistory(): string[] {
  try {
    const stored = localStorage.getItem('rag_search_history')
    return stored ? JSON.parse(stored) : []
  } catch {
    return []
  }
}

/**
 * 保存检索历史
 */
function saveSearchHistory(history: string[]) {
  localStorage.setItem('rag_search_history', JSON.stringify(history))
}

/**
 * 添加到检索历史
 */
function addToHistory(query: string) {
  const trimmed = query.trim()
  if (!trimmed || searchHistory.value[0] === trimmed) return

  const newHistory = [trimmed, ...searchHistory.value.filter(h => h !== trimmed)].slice(0, 5)
  searchHistory.value = newHistory
  saveSearchHistory(newHistory)
}

/**
 * 使用历史记录检索
 */
function searchFromHistory(query: string) {
  searchQuery.value = query
  handleSearch()
}

/**
 * 清除检索历史
 */
function clearHistory() {
  searchHistory.value = []
  saveSearchHistory([])
}

/**
 * 清除检索结果
 */
function clearResults() {
  searchResults.value = []
  hasSearched.value = false
  searchQuery.value = ''
}
</script>

<template>
  <div class="search-test-container">
    <!-- 检索输入区 -->
    <div class="search-test-input-area">
      <div class="search-test-input-row">
        <AInput
          v-model:value="searchQuery"
          placeholder="输入检索内容测试RAG效果，例如：什么是向量数据库？"
          size="middle"
          @pressEnter="handleSearch"
          allow-clear
        >
          <template #prefix>
            <SearchOutlined class="text-placeholder" />
          </template>
        </AInput>
        <AButton type="primary" :loading="searching" @click="handleSearch">
          <SearchOutlined /> 检索
        </AButton>
        <AButton @click="showAdvanced = !showAdvanced">
          <SettingOutlined />
        </AButton>
      </div>

      <!-- 高级参数 -->
      <div v-if="showAdvanced" class="search-test-params">
        <div class="search-test-params-row">
          <div class="search-test-param-item">
            <span class="search-test-param-label">Top K</span>
            <ASlider
              v-model:value="topK"
              :min="1"
              :max="20"
              :step="1"
              style="flex: 1"
            />
            <span class="text-xs" style="min-width: 30px; text-align: right;">{{ topK }}</span>
          </div>
          <div class="search-test-param-item">
            <span class="search-test-param-label">相似度阈值</span>
            <ASlider
              v-model:value="scoreThreshold"
              :min="0"
              :max="1"
              :step="0.05"
              style="flex: 1"
            />
            <span class="text-xs" style="min-width: 30px; text-align: right;">{{ scoreThreshold }}</span>
          </div>
        </div>
      </div>

      <!-- 检索历史 -->
      <div v-if="searchHistory.length > 0" class="search-test-history">
        <span class="search-test-history-label">
          <ClockCircleOutlined /> 最近搜索
        </span>
        <ATag
          v-for="(item, index) in searchHistory"
          :key="index"
          :bordered="false"
          color="default"
          style="cursor: pointer"
          @click="searchFromHistory(item)"
        >
          {{ item }}
        </ATag>
        <AButton type="text" size="small" @click="clearHistory">
          <ClearOutlined />
        </AButton>
      </div>
    </div>

    <!-- 检索结果区 -->
    <div class="search-test-results">
      <!-- 加载中 -->
      <div v-if="searching" class="search-test-empty">
        <div class="search-test-empty-text" style="margin-top: 16px;"><LoadingOutlined /> 正在检索中...</div>
      </div>

      <!-- 有结果 -->
      <template v-else-if="searchResults.length > 0">
        <div class="search-test-result-stats">
          <ExperimentOutlined />
          共检索到 <span class="search-test-result-highlight">{{ resultCount }}</span> 条结果
          <span class="text-placeholder">
            （Top K: {{ topK }}，阈值: {{ scoreThreshold }}）
          </span>
          <AButton type="text" size="small" @click="clearResults" style="margin-left: auto;">
            <ClearOutlined /> 清除
          </AButton>
        </div>

        <div class="search-result-cards" :style="{maxHeight: `calc(100vh - ${showAdvanced ? 310 : 230}px)`}">
          <div
            v-for="(result, index) in searchResults"
            :key="index"
            class="search-result-card"
          >
            <div class="search-result-card-header">
              <div class="search-result-card-header-left">
                <ATag color="blue" :bordered="false"># {{ result.chunkIndex }}</ATag>
                <div class="search-result-score">
                  <span>相关度</span>
                  <div class="search-result-score-bar">
                    <div
                      class="search-result-score-fill"
                      :style="{
                        width: getScoreWidth(result.score as number),
                        backgroundColor: getScoreColor(result.score as number)
                      }"
                    />
                  </div>
                  <span :style="{ color: getScoreColor(result.score as number) }">
                    {{ ((result.score as number) * 100).toFixed(0) }}%
                  </span>
                </div>
              </div>
              <span class="text-placeholder text-xs">
                文档ID: {{ (result.documentId as string)?.substring(0, 12) }}...
              </span>
            </div>

            <div class="search-result-content">{{ result.content }}</div>

            <div class="search-result-footer">
              <span v-if="result.tokenCount">
                <NumberOutlined /> ~{{ result.tokenCount }} tokens
              </span>
              <span>
                <FileTextOutlined /> 分块索引 {{ result.chunkIndex }}
              </span>
            </div>
          </div>
        </div>
      </template>

      <!-- 搜索后无结果 -->
      <div v-else-if="hasSearched && !searching" class="search-test-empty">
        <AEmpty  description="未找到匹配的检索结果"/>
        <div class="search-test-empty-hint">尝试调整检索内容或降低相似度阈值</div>
      </div>

      <!-- 初始状态 -->
      <div v-else class="search-test-empty">
        <SearchOutlined class="search-test-empty-icon" />
        <div class="search-test-empty-text">输入检索内容进行RAG效果测试</div>
        <div class="search-test-empty-hint">支持对已上传文档的向量化检索</div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/rag/_doc-manager.scss' as *;
</style>
