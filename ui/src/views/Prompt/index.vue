/**
 * 系统提示词模板管理主页面
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { onMounted, ref, computed, h, watch } from 'vue'
import { Modal } from 'ant-design-vue'
import {SearchOutlined, FileTextOutlined, LoadingOutlined} from '@ant-design/icons-vue'
import { usePromptStore } from '@/stores'
import { storeToRefs } from 'pinia'
import * as promptApi from '@/api/prompt'
import type { SystemPromptTemplateVO } from '@/types'
import PromptCard from '@/components/prompt/PromptCard.vue'
import CreateCard from '@/components/prompt/CreateCard.vue'
import PromptForm from '@/components/prompt/PromptForm.vue'
import {ApboaModalApi} from "@/components/common/ApboaModalApi.ts";
import InfiniteLoading from "v3-infinite-loading";
import "v3-infinite-loading/lib/style.css";

const store = usePromptStore()
const { list, categories, selectedCategory, keyword, loading, hasMore } = storeToRefs(store)

const formVisible = ref<boolean>(false)
const currentData = ref<SystemPromptTemplateVO | undefined>(undefined)
/** 用于强制重建 InfiniteLoading 组件的 key */
const infiniteLoadingKey = ref(0)
/** 是否首次加载 */
const isFirstLoad = ref(true)

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
  const response = await promptApi.detail(id)
  const data = response.data.data

  ApboaModalApi.open({
    title: '提示词模板详情',
    titleIcon: FileTextOutlined,
    footer: null,
    content: h('div', {}, [
      h('p', {}, [h('strong', '关联智能体: '), data.used?.length ? data.used.join('、') : '无']),
      h('p', {}, [h('strong', '名称: '), data.name]),
      h('p', {}, [h('strong', '分类: '), data.category]),
      h('p', {}, [h('strong', '描述: '), data.description || '暂无描述']),
      h('p', {}, h('strong', '提示词内容:')),
      h('pre', {
        style: {
          backgroundColor: '#f5f5f5',
          padding: '12px',
          borderRadius: '4px',
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
          marginTop: '8px'
        }
      }, data.content)
    ])
  })
}

/**
 * 处理编辑
 */
async function handleEdit(id: string) {
  const response = await promptApi.detail(id)
  currentData.value = response.data.data
  formVisible.value = true
}

/**
 * 重置列表状态并重建 InfiniteLoading 组件
 */
function resetListAndRebuild() {
  list.value = [];
  store.resetPagination();
  isFirstLoad.value = true;
  infiniteLoadingKey.value++;
}

/**
 * 处理删除
 */
async function handleDelete(id: string) {
  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0) {
    Modal.confirm({
      title: '二次确认',
      content: `该提示词模板正在被 [ ${used.join('、')} ] 智能体引用，删除后可能会影响上述智能体的正常使用！`,
      okText: '确认并继续删除',
      onOk: async () => {
        await store.deleteTemplate(id)
        resetListAndRebuild()
      }
    })
    return
  }

  Modal.confirm({
    title: '确认删除',
    content: '删除后无法恢复,是否继续?',
    onOk: async () => {
      await store.deleteTemplate(id)
      resetListAndRebuild()
    }
  })
}

/**
 * 处理表单提交成功
 */
function handleFormSuccess() {
  resetListAndRebuild()
  store.fetchCategories()
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
  const response = await promptApi.detail(id)
  const { enabled } = response.data.data

  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0 && enabled) {
    Modal.confirm({
      title: '二次确认',
      content: `该提示词模板正在被 [ ${used.join('、')} ] 智能体引用，禁用后可能会影响上述智能体的正常使用！`,
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
 * 处理无限加载
 *
 * @param $state 加载状态对象
 */
async function handleInfiniteLoading($state: {
  loaded: () => void;
  complete: () => void;
  error: () => void;
}) {
  if (isFirstLoad.value) {
    isFirstLoad.value = false;
    if (list.value.length > 0) {
      $state.loaded();
      return;
    }
    try {
      await store.fetchPage(1);
      if (hasMore.value) {
        $state.loaded();
      } else {
        $state.complete();
      }
    } catch {
      $state.error();
    }
    return;
  }

  if (!hasMore.value || loading.value) {
    $state.complete();
    return;
  }

  try {
    await store.loadMore();
    if (hasMore.value) {
      $state.loaded();
    } else {
      $state.complete();
    }
  } catch {
    $state.error();
  }
}

/**
 * 监听筛选条件变化，重置状态并重建 InfiniteLoading
 */
watch([selectedCategory, keyword], () => {
  list.value = [];
  store.resetPagination();
  isFirstLoad.value = true;
  infiniteLoadingKey.value++;
});

onMounted(() => {
  store.fetchCategories()
})
</script>

<template>
  <div class="prompt-page">
    <section class="intro-section">
      <h3 class="intro-title">系统提示词模板管理</h3>
      <p class="intro-desc text-secondary">
        系统提示词是智能体的“角色定义与行为准则”，通过结构化指令为大模型设定明确的角色定位、能力边界和交互范式。精心设计的提示词模板能够显著提升智能体在特定领域的表现一致性、专业性及安全性。
      </p>
    </section>

    <section class="filter-section flex justify-between items-center">
      <ASegmented
        v-model:value="selectedCategory"
        :options="categoryOptions"
      />

      <AInput
        v-model:value="keyword"
        placeholder="搜索提示词模板名称"
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

    <section class="card-section">
      <div class="card-grid">
        <CreateCard @click="handleCreate" v-permission="['EDIT','ADMIN']" />

        <PromptCard
          v-for="item in list"
          :key="item.id"
          :data="item"
          @view="handleView"
          @edit="handleEdit"
          @enable="handleEnable"
          @delete="handleDelete"
        />
      </div>

      <InfiniteLoading
        :key="infiniteLoadingKey"
        @infinite="handleInfiniteLoading"
      >
        <template #spinner>
          <div class="load-indicator mt-md">
            <span class="ml-sm text-secondary"><LoadingOutlined style="margin-right: 6px" />加载中</span>
          </div>
        </template>
        <template #complete>
          <div class="no-more-indicator text-secondary mt-md">
            没有更多数据了
          </div>
        </template>
        <template #empty>
          <div class="empty-indicator mt-lg">
            <AEmpty description="暂无数据" />
          </div>
        </template>
      </InfiniteLoading>
    </section>

    <PromptForm
      v-model:visible="formVisible"
      :data="currentData"
      :categories="categories"
      @success="handleFormSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/prompt/index.scss' as *;
</style>
