/**
 * 智能体配置-统计分析子组件
 * 使用 vue-echarts 展示4个维度的趋势图表
 *
 * @component
 */
<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent
} from 'echarts/components'
import * as statisticsApi from '@/api/agentStatistics'
import type { TrendItem } from '@/types'

use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, GridComponent, LegendComponent])

const props = defineProps<{
  agentId: string
}>()

const loading = ref(false)
const selectedDays = ref(7)

const dayOptions = [
  { label: '3天', value: 3 },
  { label: '7天', value: 7 },
  { label: '15天', value: 15 },
  { label: '30天', value: 30 },
  { label: '90天', value: 90 }
]

const sessionTrend = ref<TrendItem[]>([])
const activeUserTrend = ref<TrendItem[]>([])
const messageTrend = ref<TrendItem[]>([])
const avgRoundsTrend = ref<TrendItem[]>([])

/**
 * 加载统计数据
 */
async function loadData() {
  loading.value = true
  try {
    const res = await statisticsApi.getAgentTrends(props.agentId, selectedDays.value)
    const data = res.data.data
    sessionTrend.value = data.sessionTrend || []
    activeUserTrend.value = data.activeUserTrend || []
    messageTrend.value = data.messageTrend || []
    avgRoundsTrend.value = data.avgRoundsTrend || []
  } catch (e) {
    console.error('加载统计数据失败:', e)
  } finally {
    loading.value = false
  }
}

/**
 * 计算汇总值
 */
function sumValues(items: TrendItem[]) {
  return items.reduce((sum, i) => sum + (i.value || 0), 0)
}

function avgValues(items: TrendItem[]) {
  if (items.length === 0) return 0
  return (sumValues(items) / items.length).toFixed(1)
}

/**
 * 构建图表配置
 */
function buildChartOption(data: TrendItem[], color: string, areaColor: string) {
  return computed(() => ({
    grid: { top: 10, right: 16, bottom: 24, left: 48 },
    xAxis: {
      type: 'category' as const,
      data: data.map(d => d.date.slice(5)),
      axisLine: { lineStyle: { color: '#e8e8e8' } },
      axisLabel: { color: '#999', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value' as const,
      splitLine: { lineStyle: { color: '#f5f5f5' } },
      axisLabel: { color: '#999', fontSize: 11 },
      minInterval: 1
    },
    tooltip: {
      trigger: 'axis' as const,
      backgroundColor: '#fff',
      borderColor: '#e8e8e8',
      textStyle: { color: '#333', fontSize: 12 }
    },
    series: [{
      type: 'line' as const,
      data: data.map(d => d.value),
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color, width: 2 },
      itemStyle: { color },
      areaStyle: {
        color: {
          type: 'linear' as const,
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: areaColor },
            { offset: 1, color: 'rgba(255,255,255,0)' }
          ]
        }
      }
    }]
  }))
}

/** 响应式图表选项 */
const sessionChartOpt = computed(() => buildChartOption(sessionTrend.value, '#4449d0', 'rgba(68,73,208,0.15)').value)
const activeUserChartOpt = computed(() => buildChartOption(activeUserTrend.value, '#52c41a', 'rgba(82,196,26,0.15)').value)
const messageChartOpt = computed(() => buildChartOption(messageTrend.value, '#1890ff', 'rgba(24,144,255,0.15)').value)
const avgRoundsChartOpt = computed(() => buildChartOption(avgRoundsTrend.value, '#eb2f96', 'rgba(235,47,150,0.15)').value)

function handleDaysChange() {
  loadData()
}

watch(() => props.agentId, () => loadData())
onMounted(() => loadData())
</script>

<template>
  <div class="statistics-panel">
    <div class="statistics-toolbar">
      <span style="font-size: 16px; font-weight: 600; color: var(--color-text-primary);">数据概览</span>
      <ASegmented v-model:value="selectedDays" :options="dayOptions" @change="handleDaysChange" />
    </div>

    <ASpin :spinning="loading">
      <div class="statistics-grid">
        <!-- 会话数趋势 -->
        <div class="statistics-card">
          <div class="statistics-card-header">
            <span class="statistics-card-title">会话数</span>
            <span class="statistics-card-value">{{ sumValues(sessionTrend) }}</span>
          </div>
          <div class="statistics-chart">
            <VChart :option="sessionChartOpt" autoresize />
          </div>
        </div>

        <!-- 活跃用户趋势 -->
        <div class="statistics-card">
          <div class="statistics-card-header">
            <span class="statistics-card-title">活跃用户</span>
            <span class="statistics-card-value" style="color: #52c41a;">{{ sumValues(activeUserTrend) }}</span>
          </div>
          <div class="statistics-chart">
            <VChart :option="activeUserChartOpt" autoresize />
          </div>
        </div>

        <!-- 消息数趋势 -->
        <div class="statistics-card">
          <div class="statistics-card-header">
            <span class="statistics-card-title">消息数</span>
            <span class="statistics-card-value" style="color: #1890ff;">{{ sumValues(messageTrend) }}</span>
          </div>
          <div class="statistics-chart">
            <VChart :option="messageChartOpt" autoresize />
          </div>
        </div>

        <!-- 平均对话轮次趋势 -->
        <div class="statistics-card">
          <div class="statistics-card-header">
            <span class="statistics-card-title">平均对话轮次</span>
            <span class="statistics-card-value" style="color: #eb2f96;">{{ avgValues(avgRoundsTrend) }}</span>
          </div>
          <div class="statistics-chart">
            <VChart :option="avgRoundsChartOpt" autoresize />
          </div>
        </div>
      </div>
    </ASpin>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/agent/config-panel.scss' as *;
</style>
