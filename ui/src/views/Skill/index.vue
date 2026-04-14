/**
 * 技能管理主页面
 *
 * @author huxuehao
 */
<script setup lang="ts">
/* eslint-disable vue/multi-word-component-names */
import { onMounted, ref, computed, h, watch } from 'vue'
import { Modal, Collapse } from 'ant-design-vue'
import {SearchOutlined, AppstoreOutlined, LoadingOutlined} from '@ant-design/icons-vue'
import { useSkillStore } from '@/stores'
import { storeToRefs } from 'pinia'
import * as skillApi from '@/api/skill'
import type { SkillPackageVO } from '@/types'
import SkillCard from '@/components/skill/SkillCard.vue'
import CreateCard from '@/components/skill/CreateCard.vue'
import SkillForm from '@/components/skill/SkillForm.vue'
import ImportLocalForm from '@/components/skill/ImportLocalForm.vue'
import ImportGitForm from '@/components/skill/ImportGitForm.vue'
import ImportUploadForm from '@/components/skill/ImportUploadForm.vue'
import {ApboaModalApi} from "@/components/common/ApboaModalApi.ts";
import InfiniteLoading from "v3-infinite-loading";
import "v3-infinite-loading/lib/style.css";

/**
 * 资源项接口
 */
interface ResourceItem {
  prefix: string
  name: string
  content: string
}

const store = useSkillStore()
const { list, categories, selectedCategory, keyword, loading, hasMore } = storeToRefs(store)

const formVisible = ref<boolean>(false)
const currentData = ref<SkillPackageVO | undefined>(undefined)

const importLocalVisible = ref(false)
const importGitVisible = ref(false)
const importUploadVisible = ref(false)

/**
 * 用于强制重建 InfiniteLoading 组件的 key
 */
const infiniteLoadingKey = ref(0)

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
 * 处理装载本地技能包
 */
function handleImportLocal() {
  importLocalVisible.value = true
}

/**
 * 处理导入 Git 技能包
 */
function handleImportGit() {
  importGitVisible.value = true
}

/**
 * 处理导入压缩包技能
 */
function handleImportUpload() {
  importUploadVisible.value = true
}

/**
 * 渲染折叠面板内容
 */
function renderCollapseContent(items: ResourceItem[]) {
  if (!items || items.length === 0) {
    return h('div', { class: 'text-secondary' }, '暂无数据')
  }

  return h('div', { class: 'resource-list' },
    items.map((item, index) =>
      h('div', {
        key: index,
        class: 'resource-item',
        style: {
          marginBottom: '12px',
          padding: '12px',
          backgroundColor: '#f5f5f5',
          borderRadius: '4px'
        }
      }, [
        h('div', {
          style: {
            fontWeight: 'bold',
            marginBottom: '8px',
            color: '#333'
          }
        }, `${index + 1}. ${item.name || '未命名'}`),
        h('pre', {
          style: {
            maxHeight: '200px',
            overflowY: 'auto',
            background: '#fff',
            padding: '12px',
            borderRadius: '4px',
            overflow: 'auto',
            margin: 0,
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word'
          }
        }, item.content || '')
      ])
    )
  )
}

/**
 * 处理查看
 */
async function handleView(id: string) {
  const response = await skillApi.detail(id)
  const data = response.data.data

  ApboaModalApi.open({
    title: '技能包详情',
    titleIcon: AppstoreOutlined,
    footer: null,
    content: h('div', {}, [
      h('p', {}, [h('strong', '关联智能体: '), data.used?.length ? data.used.join('、') : '无']),
      h('p', {}, [h('strong', '分类: '), data.category]),
      h('p', {}, [h('strong', '名称: '), data.name]),
      h('p', {}, [h('strong', '描述: '), data.description]),
      h('p', {}, [h('strong', '是否关联工具: '), data.tools?.length ? '是' : '否']),
      h('div', { style: { marginTop: '16px' } }, [
        h(Collapse, {
          defaultActiveKey: []
        }, {
          default: () => [
            h(Collapse.Panel, {
              key: '1',
              header: '技能内容'
            }, {
              default: () => h('pre', {
                style: {
                  background: '#f5f5f5',
                  maxHeight: '300px',
                  overflowY: 'auto',
                  padding: '12px',
                  borderRadius: '4px',
                  whiteSpace: 'pre-wrap',
                  wordBreak: 'break-word'
                }
              }, data.skillContent || '')
            }),
            h(Collapse.Panel, {
              key: '2',
              header: `参考资料 (${(data.references || []).length} 项)`
            }, {
              default: () => renderCollapseContent((data.references as ResourceItem[]) || [])
            }),
            h(Collapse.Panel, {
              key: '3',
              header: `示例代码 (${(data.examples || []).length} 项)`
            }, {
              default: () => renderCollapseContent((data.examples as ResourceItem[]) || [])
            }),
            h(Collapse.Panel, {
              key: '4',
              header: `执行脚本 (${(data.scripts || []).length} 项)`
            }, {
              default: () => renderCollapseContent((data.scripts as ResourceItem[]) || [])
            })
          ]
        })
      ])
    ])
  })
}

/**
 * 处理编辑
 */
async function handleEdit(id: string) {
  const response = await skillApi.detail(id)
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
      content: `该技能包正在被 [ ${used.join('、')} ] 智能体引用，删除后可能会影响上述智能体的正常使用！`,
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
 * 处理导入成功
 */
function handleImportSuccess() {
  store.resetAndFetch()
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
  const response = await skillApi.detail(id)
  const { enabled } = response.data.data

  const used = await store.checkUsedWithAgent(id)
  if (used.length > 0 && enabled) {
    Modal.confirm({
      title: '二次确认',
      content: `该技能包正在被 [ ${used.join('、')} ] 智能体引用，禁用后可能会影响上述智能体的正常使用！`,
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
  // 首次加载使用 page=1，后续使用 loadMore
  if (isFirstLoad.value) {
    isFirstLoad.value = false;
    if (list.value.length > 0) {
      // 如果已有数据（如从缓存恢复），直接完成
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

  // 非首次加载，使用 loadMore 加载下一页
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
 * 是否首次加载
 */
const isFirstLoad = ref(true);

/**
 * 监听筛选条件变化，重置状态并重建 InfiniteLoading
 */
watch([selectedCategory, keyword], () => {
  // 重置列表和分页状态
  list.value = [];
  store.resetPagination();
  // 重置首次加载标志
  isFirstLoad.value = true;
  // 强制重建 InfiniteLoading 组件
  infiniteLoadingKey.value++;
});

onMounted(() => {
  store.fetchCategories()
})
</script>

<template>
  <div class="skill-page">
    <section class="intro-section">
      <h3 class="intro-title">技能包管理</h3>
      <p class="intro-desc text-secondary">
        技能管理是智能体的“专业能力装配中心”，通过模块化、可插拔的技能包体系，让智能体能够根据不同场景需求动态加载专业知识库与处理逻辑，实现从通用助手到领域专家的能力跃迁。
      </p>
    </section>

    <section class="filter-section flex justify-between items-center">
      <ASegmented
        v-model:value="selectedCategory"
        :options="categoryOptions"
      />

      <AInput
        v-model:value="keyword"
        placeholder="搜索技能包名称"
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
        <CreateCard
          @click="handleCreate"
          @importLocal="handleImportLocal"
          @importGit="handleImportGit"
          @importUpload="handleImportUpload"
          v-permission="['EDIT','ADMIN']"
        />

        <SkillCard
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

    <SkillForm
      v-model:visible="formVisible"
      :data="currentData"
      :categories="categories"
      @success="handleFormSuccess"
    />

    <ImportLocalForm
      v-model:visible="importLocalVisible"
      @success="handleImportSuccess"
    />

    <ImportGitForm
      v-model:visible="importGitVisible"
      @success="handleImportSuccess"
    />

    <ImportUploadForm
      v-model:visible="importUploadVisible"
      @success="handleImportSuccess"
    />
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/skill/index.scss' as *;
</style>
