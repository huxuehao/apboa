/**
 * 钩子管理主页面
 *
 * @author huxuehao
 */
<script setup lang="ts">
/* eslint-disable vue/multi-word-component-names */
import { onMounted, ref, onUnmounted, computed, h } from 'vue'
import { Modal } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import { useHookStore } from '@/stores'
import { storeToRefs } from 'pinia'
import * as hookApi from '@/api/hook'
import type { HookConfigVO } from '@/types'
import { HookType } from '@/types'
import HookCard from '@/components/hook/HookCard.vue'
import HookCreateCard from '@/components/hook/HookCreateCard.vue'
import HookForm from '@/components/hook/HookForm.vue'

const store = useHookStore()
const {
  list,
  selectedHookType,
  keyword,
  loading,
  hasMore
} = storeToRefs(store)

const formVisible = ref<boolean>(false)
const currentData = ref<HookConfigVO | undefined>(undefined)
const scrollContainer = ref<HTMLElement>()
const loadMoreObserver = ref<IntersectionObserver>()

/** 全部类型的占位值（用于 Segmented，避免 null 绑定问题） */
const HOOK_TYPE_ALL = 'ALL'

/**
 * 钩子类型选项（全部、内置、自定义）
 */
const hookTypeOptions = [
  { label: '全部', value: HOOK_TYPE_ALL },
  { label: '内置', value: HookType.BUILTIN },
  { label: '自定义', value: HookType.CUSTOM }
]

/**
 * 当前选中的钩子类型（用于 Segmented 显示，ALL 表示全部）
 */
const selectedHookTypeDisplay = computed({
  get: () => (selectedHookType.value === null ? HOOK_TYPE_ALL : selectedHookType.value),
  set: (val: string | HookType) => {
    store.setHookType(val === HOOK_TYPE_ALL ? null : (val as HookType))
  }
})

/**
 * 是否显示新增卡片（全部或自定义时显示）
 */
const showCreateCard = computed(() => {
  return selectedHookType.value !== HookType.BUILTIN
})

/**
 * 处理新增
 */
function handleCreate() {
  currentData.value = undefined
  formVisible.value = true
}

/**
 * 处理编辑
 */
async function handleEdit(id: string) {
  const response = await hookApi.detail(id)
  currentData.value = response.data.data
  formVisible.value = true
}

/**
 * 处理删除
 */
async function handleDelete(id: string) {
  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0) {
    Modal.confirm({
      title: '二次确认',
      content: `该钩子正在被 [ ${used.join('、')} ] 智能体引用，删除后可能会影响上述智能体的正常使用！`,
      okText: '确认并继续删除',
      onOk: async () => {
        await store.deleteConfig(id)
      }
    })
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复，是否继续？',
    onOk: async () => {
      await store.deleteConfig(id)
    }
  })
}

/**
 * 处理表单提交成功
 */
function handleFormSuccess() {
  store.resetAndFetch()
}

/**
 * 处理查看详情
 */
async function handleView(id: string) {
  const response = await hookApi.detail(id)
  const data = response.data.data

  Modal.info({
    title: '钩子详情',
    closable: true,
    icon: null,
    footer: null,
    width: 640,
    content: h('div', { style: { maxHeight: '600px', overflowY: 'auto' } }, [
      h('p', {}, [h('strong', '关联智能体: '), data.used?.length ? data.used.join('、') : '无']),
      h('p', {}, [h('strong', '类型: '), data.hookType === 'BUILTIN' ? '内置' : '自定义']),
      h('p', {}, [h('strong', '名称: '), data.name]),
      h('p', {}, [h('strong', '描述: '), data.description || '暂无']),
      h('p', {}, [h('strong', '类路径: '), data.classPath || '暂无']),
      h('p', {}, [h('strong', '优先级: '), String(data.priority)]),
      h('p', {}, [h('strong', '状态: '), data.enabled ? '启用' : '禁用']),
      ...(data.code
        ? [
            h('p', {}, h('strong', '钩子内容:')),
            h('pre', {
              style: {
                background: '#f5f5f5',
                padding: '12px',
                borderRadius: '4px',
                maxHeight: '200px',
                overflowY: 'auto',
                fontSize: '12px'
              }
            }, data.code)
          ]
        : [])
    ])
  })
}

/**
 * 处理切换状态
 */
async function handleEnable(id: string) {
  const response = await hookApi.detail(id)
  const { enabled } = response.data.data

  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0 && enabled) {
    Modal.confirm({
      title: '二次确认',
      content: `该钩子正在被 [ ${used.join('、')} ] 智能体引用，禁用后可能会影响上述智能体的正常使用！`,
      okText: '确认并继续',
      onOk: async () => {
        await store.toggleEnabled(id, !enabled)
      }
    })
    return
  }

  await store.toggleEnabled(id, !enabled)
}

/**
 * 处理搜索
 */
function handleSearch() {
  store.setKeyword(keyword.value)
}

/**
 * 初始化无限滚动观察器
 */
function initIntersectionObserver() {
  if (!scrollContainer.value) return

  const sentinel = document.createElement('div')
  sentinel.className = 'scroll-sentinel'
  scrollContainer.value.appendChild(sentinel)

  loadMoreObserver.value = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry && entry.isIntersecting && hasMore.value && !loading.value) {
        store.loadMore()
      }
    },
    { threshold: 0.1 }
  )

  loadMoreObserver.value.observe(sentinel)
}

onMounted(() => {
  store.fetchPage(1)
  setTimeout(() => {
    initIntersectionObserver()
  }, 100)
})

onUnmounted(() => {
  loadMoreObserver.value?.disconnect()
})
</script>

<template>
  <div class="hook-page">
    <section class="intro-section">
      <h3 class="intro-title">钩子管理</h3>
      <p class="intro-desc text-secondary">
        钩子（Hook）是智能体执行流程中的切面扩展点，用于在关键节点插入自定义逻辑。行业实践通常在「调用前后、推理前后、工具调用前后、发生错误时」等时机挂载钩子，实现日志、鉴权、限流、审计或业务定制，而不侵入主流程代码。本模块展示已配置的钩子，便于查看与启用状态管理。
      </p>
    </section>

    <section class="filter-section flex justify-between items-center">
      <div class="filter-left">
        <ASegmented
          v-model:value="selectedHookTypeDisplay"
          :options="hookTypeOptions"
        />
      </div>

      <div class="filter-right flex items-center gap-md">
        <AInput
          v-model:value="keyword"
          placeholder="搜索钩子名称"
          style="width: 300px; border: rgba(14,14,14,0.1) solid 1px !important;"
          @pressEnter="handleSearch"
        >
          <template #suffix>
            <AButton type="text" size="small" @click="handleSearch">
              <SearchOutlined />
            </AButton>
          </template>
        </AInput>
      </div>
    </section>

    <section ref="scrollContainer" class="card-section">
      <div class="card-grid">
        <HookCreateCard v-if="showCreateCard" @click="handleCreate" v-permission="['EDIT','ADMIN']" />

        <HookCard
          v-for="item in list"
          :key="item.id"
          :data="item"
          @view="handleView"
          @edit="handleEdit"
          @enable="handleEnable"
          @delete="handleDelete"
        />
      </div>

      <div v-if="loading" class="load-indicator mt-md">
        <span class="ml-sm text-secondary">加载中...</span>
      </div>

      <div v-if="!hasMore && list.length > 0" class="no-more-indicator text-secondary mt-md">
        没有更多数据了
      </div>

      <div v-if="!loading && list.length === 0" class="empty-indicator mt-lg">
        <AEmpty description="暂无数据" />
      </div>
    </section>

    <HookForm
      v-model:visible="formVisible"
      :data="currentData"
      @success="handleFormSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/hook/index.scss' as *;
</style>
