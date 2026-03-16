/**
 * 敏感词管理主页面
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { onMounted, ref, onUnmounted, computed, h } from 'vue'
import { Modal } from 'ant-design-vue'
import { SearchOutlined, SafetyCertificateOutlined } from '@ant-design/icons-vue'
import { useSensitiveStore } from '@/stores'
import { storeToRefs } from 'pinia'
import * as sensitiveApi from '@/api/sensitive'
import type { SensitiveWordConfigVO } from '@/types'
import SensitiveCard from '@/components/sensitive/SensitiveCard.vue'
import CreateCard from '@/components/sensitive/CreateCard.vue'
import SensitiveForm from '@/components/sensitive/SensitiveForm.vue'
import { ApboaModalApi } from "@/components/common/ApboaModalApi.ts";

const store = useSensitiveStore()
const { list, categories, selectedCategory, keyword, loading, hasMore } = storeToRefs(store)

const formVisible = ref<boolean>(false)
const currentData = ref<SensitiveWordConfigVO | undefined>(undefined)
const scrollContainer = ref<HTMLElement>()
const loadMoreObserver = ref<IntersectionObserver>()

/**
 * 分类选项列表
 */
const categoryOptions = computed(() => {
  const options = categories.value.map(cat => ({
    label: cat,
    value: cat
  }))
  return [
    { label: '全部', value: null },
    ...options
  ]
})

/**
 * 处理新增
 */
function handleCreate() {
  currentData.value = undefined
  formVisible.value = true
}

/**
 * 处理查看
 */
async function handleView(id: string) {
  const response = await sensitiveApi.detail(id)
  const data = response.data.data
  const wordsList = data.words || []

  ApboaModalApi.open({
    title: '敏感词配置详情',
    titleIcon: SafetyCertificateOutlined,
    footer: null,
    content: h('div', {}, [
      h('p', {}, [h('strong', '关联智能体: '), data.used?.length ? data.used.join('、') : '无']),
      h('p', {}, [h('strong', '名称: '), data.name]),
      h('p', {}, [h('strong', '分类: '), data.category]),
      h('p', {}, [h('strong', '描述: '), data.description]),
      h('p', {}, [h('strong', '处理动作: '), data.action]),
      ...(data.action === 'REPLACE' ? [h('p', {}, [h('strong', '替换文本: '), data.replacement])] : []),
      h('p', {}, h('strong', '敏感词列表:')),
      h('div', { style: { display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '8px', maxHeight: '300px', overflowY: 'auto' } }, [
        ...(Array.isArray(wordsList) && wordsList.length > 0
          ? wordsList.map(w => h('a-tag', {}, w))
          : [h('span', {}, '暂无敏感词')])
      ])
    ])
  })
}

/**
 * 处理编辑
 */
async function handleEdit(id: string) {
  const response = await sensitiveApi.detail(id)
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
      content: `该敏感词正在被 [ ${used.join('、')} ] 智能体引用，删除后可能会影响上述智能体的正常使用！`,
      okText: '确认并继续删除',
      onOk: async () => {
        await store.deleteConfig(id)
      }
    })
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复,是否继续?',
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
  store.fetchCategories()
}

/**
 * 处理分类切换
 */
function handleCategoryChange(value: string | null) {
  store.setCategory(value)
}

/**
 * 处理搜索
 */
function handleSearch() {
  store.setKeyword(keyword.value)
}

/**
 * 处理状态
 */
async function handleEnable(id: string) {
  const response = await sensitiveApi.detail(id)
  const { enabled } = response.data.data

  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0 && enabled) {
    Modal.confirm({
      title: '二次确认',
      content: `该敏感词正在被 [ ${used.join('、')} ] 智能体引用，禁用后可能会影响上述智能体的正常使用！`,
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
  store.fetchCategories()
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
  <div class="sensitive-page">
    <section class="intro-section">
      <h3 class="intro-title">敏感词管理</h3>
      <p class="intro-desc text-secondary">
        敏感词管理模块是企业级智能体应用的核心安全组件，通过系统化的关键词规则配置与实时内容扫描，在智能体交互全流程中构建多层次的内容安全防线。该机制既保障用户交互体验的流畅性，又确保所有输入输出内容符合法律法规、平台政策及企业价值观要求
      </p>
    </section>

    <section class="filter-section flex justify-between items-center">
      <ASegmented
        v-model:value="selectedCategory"
        :options="categoryOptions"
        @change="handleCategoryChange"
      />

      <AInput
        v-model:value="keyword"
        placeholder="搜索敏感词配置名称"
        style="width: 300px; border: rgba(14,14,14,0.1) solid 1px !important;"
        @pressEnter="handleSearch"
      >
        <template #suffix>
          <AButton type="text" size="small" @click="handleSearch">
            <SearchOutlined />
          </AButton>
        </template>
      </AInput>
    </section>

    <section ref="scrollContainer" class="card-section">
      <div class="card-grid">
        <CreateCard @click="handleCreate" v-permission="['EDIT','ADMIN']" />

        <SensitiveCard
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

    <SensitiveForm
      v-model:visible="formVisible"
      :data="currentData"
      :categories="categories"
      @success="handleFormSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/sensitive/index.scss' as *;
</style>
