<!-- eslint-disable vue/multi-word-component-names -->
/**
 * Markdown 扩展开发指南 - 文档视图
 *
 * 使用 MarkdownRenderer 渲染结构化 .md 文件，支持右侧目录导航
 *
 * @author huxuehao
 * @component
 */
<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import MarkdownRenderer from '@/components/markdown/MarkdownRenderer.vue'

interface TocItem {
  id: string
  title: string
  level: number
  children?: TocItem[]
}

/**
 * 文档章节配置
 *
 * 每个章节对应一个 .md 文件，按顺序加载和渲染
 */
const docSections = [
  { key: 'overview', label: '概述', file: import('../content/markdown-extension/overview.md?raw') },
  { key: 'container', label: '容器扩展自动发现', file: import('../content/markdown-extension/container-extension.md?raw') },
  { key: 'renderer', label: '渲染器处理器覆盖', file: import('../content/markdown-extension/renderer-override.md?raw') },
  { key: 'practices', label: '最佳实践', file: import('../content/markdown-extension/best-practices.md?raw') },
  { key: 'reference', label: '速查表', file: import('../content/markdown-extension/quick-reference.md?raw') },
]

/**
 * 获取实际滚动容器（doc-content）
 */
const getScrollContainer = (): HTMLElement | null => {
  return document.querySelector('.doc-content')
}

const route = useRoute()
const activeSection = ref('')
const tocItems = ref<TocItem[]>([])
const markdownContent = ref('')
const loading = ref(true)

/**
 * 加载所有 markdown 内容并合并
 */
const loadMarkdownContent = async () => {
  loading.value = true
  try {
    const modules = await Promise.all(docSections.map(s => s.file))
    markdownContent.value = modules.map(m => m.default as string).join('\n\n---\n\n')
  } catch (error) {
    console.error('加载文档内容失败:', error)
    markdownContent.value = '# 加载失败\n\n文档内容加载失败，请刷新重试。'
  } finally {
    loading.value = false
  }
}

/**
 * 生成目录
 */
const generateToc = () => {
  const container = document.querySelector('.doc-md-content')
  if (!container) return

  const headings = container.querySelectorAll('h1, h2, h3')
  const toc: TocItem[] = []
  let currentH1: TocItem | null = null
  let currentH2: TocItem | null = null

  headings.forEach((heading, index) => {
    const id = `section-${index}`
    heading.id = id

    const item: TocItem = {
      id,
      title: heading.textContent || '',
      level: parseInt(heading.tagName.charAt(1)),
      children: []
    }

    if (item.level === 1) {
      currentH1 = item
      toc.push(item)
    } else if (item.level === 2) {
      if (currentH1) {
        currentH1.children = currentH1.children || []
        currentH1.children.push(item)
      }
      currentH2 = item
    } else if (item.level === 3) {
      if (currentH2 && currentH1) {
        const h2Parent = currentH1.children?.find(h2 => h2.id === currentH2?.id)
        if (h2Parent) {
          h2Parent.children = h2Parent.children || []
          h2Parent.children.push(item)
        }
      }
    }
  })

  tocItems.value = toc
}

/**
 * 滚动到指定章节
 */
const scrollToSection = (id: string) => {
  const element = document.getElementById(id)
  const container = getScrollContainer()
  if (element && container) {
    const containerRect = container.getBoundingClientRect()
    const elementRect = element.getBoundingClientRect()
    const offset = container.scrollTop + elementRect.top - containerRect.top - 16
    container.scrollTo({ top: offset, behavior: 'smooth' })
    activeSection.value = id
  }
}

/**
 * 监听内容区滚动高亮当前章节
 */
const handleScroll = () => {
  const container = getScrollContainer()
  if (!container) return
  const headings = document.querySelectorAll('.doc-md-content h1, .doc-md-content h2, .doc-md-content h3')
  let currentId = ''
  const containerTop = container.getBoundingClientRect().top

  headings.forEach((heading) => {
    const rect = heading.getBoundingClientRect()
    if (rect.top - containerTop <= 32) {
      currentId = heading.id
    }
  })

  if (currentId) {
    activeSection.value = currentId
  }
}

onMounted(async () => {
  await loadMarkdownContent()
  nextTick(() => {
    generateToc()
    const container = getScrollContainer()
    if (container) {
      container.addEventListener('scroll', handleScroll)
    }
  })
})

// 路由变化时重新加载
watch(() => route.path, async () => {
  await loadMarkdownContent()
  nextTick(() => {
    generateToc()
  })
})
</script>

<template>
  <div class="doc-md-page">
    <!-- 加载状态 -->
    <div v-if="loading" class="doc-md-loading">
      <ASpin size="large" tip="加载文档中..." />
    </div>

    <!-- 主内容区 -->
    <article v-else class="doc-md-content chat-md-content">
      <MarkdownRenderer :content="markdownContent" />
    </article>

    <!-- 右侧目录导航 -->
    <aside v-if="tocItems.length > 0" class="doc-md-toc">
      <div class="doc-md-toc-title">目录</div>
      <nav class="doc-md-toc-nav">
        <template v-for="h1 in tocItems" :key="h1.id">
          <div
            class="doc-md-toc-item doc-md-toc-h1"
            :class="{ active: activeSection === h1.id }"
            :style="activeSection === h1.id ? { color: '#0F74FF', backgroundColor: '#0F74FF14' } : {}"
            @click="scrollToSection(h1.id)"
          >
            {{ h1.title }}
          </div>
          <template v-for="h2 in h1.children" :key="h2.id">
            <div
              v-if="h1.children && h1.children.length > 0"
              class="doc-md-toc-item doc-md-toc-h2"
              :class="{ active: activeSection === h2.id }"
              :style="activeSection === h2.id ? { color: '#0F74FF', backgroundColor: '#0F74FF14' } : {}"
              @click="scrollToSection(h2.id)"
            >
              {{ h2.title }}
            </div>
            <template v-for="h3 in h2.children" :key="h3.id">
              <div
                v-if="h2.children && h2.children.length > 0"
                class="doc-md-toc-item doc-md-toc-h3"
                :class="{ active: activeSection === h3.id }"
                :style="activeSection === h3.id ? { color: '#0F74FF', backgroundColor: '#0F74FF14' } : {}"
                @click="scrollToSection(h3.id)"
              >
                {{ h3.title }}
              </div>
            </template>
          </template>
        </template>
      </nav>
    </aside>
  </div>
</template>

<style scoped lang="scss">
$doc-toc-width: 240px;

.doc-md-page {
  display: flex;
  min-height: 100%;
  position: relative;
}

/* 加载状态 */
.doc-md-loading {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

/* 主内容区 - 居中、左右留白等宽 */
.doc-md-content {
  flex: 1;
  min-width: 0;
  padding: 40px 128px 64px;
  margin-right: $doc-toc-width;

  /* 文档页特有样式：标题间距增大 */
  :deep(.md-h1) {
    font-size: 1.8em;
    margin-top: 0;
    margin-bottom: 0.8em;
  }

  :deep(.md-h2) {
    font-size: 1.4em;
    margin-top: 2em;
    padding-top: 1.5em;
    border-top: 1px solid #F3F4F6;
  }

  :deep(.md-h3) {
    font-size: 1.15em;
    margin-top: 1.5em;
  }

  /* 文档页特有样式：分隔线间距 */
  :deep(.md-hr) {
    margin: 2em 0;
  }
}

/* 右侧目录导航 - 固定不滚动，宽度与左侧菜单一致 */
.doc-md-toc {
  width: $doc-toc-width;
  flex-shrink: 0;
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  padding: 24px 12px;
  overflow-y: auto;
  border-left: 1px solid #f0f0f0;
  background-color: #fff;
}

.doc-md-toc-title {
  font-size: 13px;
  font-weight: 600;
  color: #1F2937;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #E5E7EB;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.doc-md-toc-nav {
  display: flex;
  flex-direction: column;
}

.doc-md-toc-item {
  padding: 5px 8px;
  cursor: pointer;
  color: #6B7280;
  font-size: 13px;
  transition: all 0.15s ease;
  border-radius: 4px;
  line-height: 1.4;

  &:hover {
    background-color: #f5f7fa;
  }

  &.active {
    font-weight: 500;
  }
}

.doc-md-toc-h1 {
  font-weight: 500;
  color: #374151;
}

.doc-md-toc-h2 {
  padding-left: 14px;
  font-size: 12px;
}

.doc-md-toc-h3 {
  padding-left: 24px;
  font-size: 12px;
  color: #9CA3AF;
}
</style>
