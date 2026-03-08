/**
 * 智能体架构图组件
 * 使用 Vue Flow 展示智能体的完整配置架构
 *
 * @author huxuehao
 */
<script setup lang="ts">
import { computed, onMounted, watch, markRaw } from 'vue'
import { VueFlow, useVueFlow, type Node, type Edge } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { MiniMap } from '@vue-flow/minimap'
import { Controls } from '@vue-flow/controls'
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/minimap/dist/style.css'
import '@vue-flow/controls/dist/style.css'
import { Spin } from 'ant-design-vue'

import { useArchitectureData } from './composables/useArchitectureData'
import { CATEGORY_CONFIGS, NODE_SIZES, type CategoryType } from './types'

import CenterAgentNode from './nodes/CenterAgentNode.vue'
import CategoryNode from './nodes/CategoryNode.vue'
import ToolItemNode from './nodes/ToolItemNode.vue'
import HookItemNode from './nodes/HookItemNode.vue'
import SkillItemNode from './nodes/SkillItemNode.vue'
import McpItemNode from './nodes/McpItemNode.vue'
import KnowledgeItemNode from './nodes/KnowledgeItemNode.vue'
import AgentItemNode from './nodes/AgentItemNode.vue'
import ModelNode from './nodes/ModelNode.vue'
import PromptNode from './nodes/PromptNode.vue'
import AdvancedConfigNode from './nodes/AdvancedConfigNode.vue'
import SensitiveItemNode from './nodes/SensitiveItemNode.vue'

/**
 * Props定义
 */
const props = defineProps<{
  agentId: string
}>()

/**
 * 数据获取
 */
const { loading, data, loadArchitectureData, resetData } = useArchitectureData()

/**
 * Vue Flow
 */
const { fitView } = useVueFlow()

/**
 * 自定义节点类型
 */
const nodeTypes = {
  'center-agent': markRaw(CenterAgentNode),
  'category': markRaw(CategoryNode),
  'tool-item': markRaw(ToolItemNode),
  'hook-item': markRaw(HookItemNode),
  'skill-item': markRaw(SkillItemNode),
  'mcp-item': markRaw(McpItemNode),
  'knowledge-item': markRaw(KnowledgeItemNode),
  'agent-item': markRaw(AgentItemNode),
  'model': markRaw(ModelNode),
  'prompt': markRaw(PromptNode),
  'advanced-config': markRaw(AdvancedConfigNode),
  'sensitive-item': markRaw(SensitiveItemNode)
}

/**
 * 布局配置
 */
const LAYOUT = {
  centerX: 400,
  centerY: 400,
  categoryDistance: 280,
  itemStartDistance: 200,
  itemSpacingY: 140
}

/**
 * 计算分类节点位置（围绕中心分布）
 */
function getCategoryPosition(index: number, total: number): { x: number; y: number } {
  const startAngle = -Math.PI / 2
  const angle = startAngle + (2 * Math.PI * index) / total
  return {
    x: LAYOUT.centerX + Math.cos(angle) * LAYOUT.categoryDistance - NODE_SIZES.category.width / 2,
    y: LAYOUT.centerY + Math.sin(angle) * LAYOUT.categoryDistance - NODE_SIZES.category.height / 2
  }
}

/**
 * 计算配置项节点位置
 */
function getItemPosition(
  categoryPos: { x: number; y: number },
  index: number,
  total: number,
  categoryIndex: number,
  totalCategories: number
): { x: number; y: number } {
  const angle = -Math.PI / 2 + (2 * Math.PI * categoryIndex) / totalCategories
  const dirX = Math.cos(angle)
  const dirY = Math.sin(angle)

  const startOffset = -((total - 1) * LAYOUT.itemSpacingY) / 2
  const offsetY = startOffset + index * LAYOUT.itemSpacingY

  const baseX = categoryPos.x + NODE_SIZES.category.width + 60
  const baseY = categoryPos.y + NODE_SIZES.category.height / 2

  if (Math.abs(dirX) > Math.abs(dirY)) {
    return {
      x: baseX + (dirX > 0 ? LAYOUT.itemStartDistance : -LAYOUT.itemStartDistance - NODE_SIZES.item.width),
      y: baseY + offsetY - NODE_SIZES.item.height / 2
    }
  } else {
    return {
      x: categoryPos.x + NODE_SIZES.category.width / 2 + offsetY - NODE_SIZES.item.width / 2,
      y: baseY + (dirY > 0 ? LAYOUT.itemStartDistance : -LAYOUT.itemStartDistance - NODE_SIZES.item.height)
    }
  }
}

/**
 * 需要显示的分类列表
 */
const activeCategories = computed<CategoryType[]>(() => {
  const categories: CategoryType[] = []

  if (data.tools.length > 0) categories.push('tool')
  if (data.hooks.length > 0) categories.push('hook')
  if (data.skills.length > 0) categories.push('skill')
  if (data.mcps.length > 0) categories.push('mcp')
  if (data.knowledgeBases.length > 0) categories.push('knowledge')
  if (data.subAgents.length > 0) categories.push('sub-agent')

  // 模型、提示词、高级配置始终显示
  categories.push('model')
  categories.push('prompt')
  categories.push('advanced')

  // 敏感词只在启用时显示
  if (data.agent?.sensitiveFilterEnabled && data.sensitiveConfig) {
    categories.push('sensitive')
  }

  return categories
})

/**
 * 生成节点
 */
const nodes = computed<Node[]>(() => {
  if (!data.agent) return []

  const result: Node[] = []
  const totalCategories = activeCategories.value.length

  // 中心节点
  result.push({
    id: 'center',
    type: 'center-agent',
    position: {
      x: LAYOUT.centerX - NODE_SIZES.center.width / 2,
      y: LAYOUT.centerY - NODE_SIZES.center.height / 2
    },
    data: { agent: data.agent },
    draggable: true
  })

  // 遍历活跃的分类
  activeCategories.value.forEach((category, categoryIndex) => {
    const config = CATEGORY_CONFIGS[category]
    const categoryPos = getCategoryPosition(categoryIndex, totalCategories)

    // 分类节点
    let count = 0
    switch (category) {
      case 'tool': count = data.tools.length; break
      case 'hook': count = data.hooks.length; break
      case 'skill': count = data.skills.length; break
      case 'mcp': count = data.mcps.length; break
      case 'knowledge': count = data.knowledgeBases.length; break
      case 'sub-agent': count = data.subAgents.length; break
      default: count = 0
    }

    result.push({
      id: `category-${category}`,
      type: 'category',
      position: categoryPos,
      data: {
        category,
        label: config.label,
        count,
        icon: config.icon,
        color: config.color,
        bgColor: config.bgColor,
        borderColor: config.borderColor
      },
      draggable: true
    })

    // 配置项节点
    switch (category) {
      case 'tool':
        data.tools.forEach((tool, i) => {
          const pos = getItemPosition(categoryPos, i, data.tools.length, categoryIndex, totalCategories)
          result.push({
            id: `tool-${tool.id}`,
            type: 'tool-item',
            position: pos,
            data: { tool },
            draggable: true
          })
        })
        break

      case 'hook':
        data.hooks.forEach((hook, i) => {
          const pos = getItemPosition(categoryPos, i, data.hooks.length, categoryIndex, totalCategories)
          result.push({
            id: `hook-${hook.id}`,
            type: 'hook-item',
            position: pos,
            data: { hook },
            draggable: true
          })
        })
        break

      case 'skill':
        data.skills.forEach((skill, i) => {
          const pos = getItemPosition(categoryPos, i, data.skills.length, categoryIndex, totalCategories)
          result.push({
            id: `skill-${skill.id}`,
            type: 'skill-item',
            position: pos,
            data: { skill },
            draggable: true
          })
        })
        break

      case 'mcp':
        data.mcps.forEach((mcp, i) => {
          const pos = getItemPosition(categoryPos, i, data.mcps.length, categoryIndex, totalCategories)
          result.push({
            id: `mcp-${mcp.id}`,
            type: 'mcp-item',
            position: pos,
            data: { mcp },
            draggable: true
          })
        })
        break

      case 'knowledge':
        data.knowledgeBases.forEach((knowledge, i) => {
          const pos = getItemPosition(categoryPos, i, data.knowledgeBases.length, categoryIndex, totalCategories)
          result.push({
            id: `knowledge-${knowledge.id}`,
            type: 'knowledge-item',
            position: pos,
            data: { knowledge },
            draggable: true
          })
        })
        break

      case 'sub-agent':
        data.subAgents.forEach((agent, i) => {
          const pos = getItemPosition(categoryPos, i, data.subAgents.length, categoryIndex, totalCategories)
          result.push({
            id: `agent-${agent.id}`,
            type: 'agent-item',
            position: pos,
            data: { agent },
            draggable: true
          })
        })
        break

      case 'model':
        result.push({
          id: 'model-config',
          type: 'model',
          position: {
            x: categoryPos.x + NODE_SIZES.category.width + 60,
            y: categoryPos.y - NODE_SIZES.model.height / 2 + NODE_SIZES.category.height / 2
          },
          data: {
            modelConfig: data.modelConfig,
            provider: data.modelProvider,
            paramsOverride: data.agent?.modelParamsOverride || null
          },
          draggable: true
        })
        break

      case 'prompt':
        result.push({
          id: 'prompt-config',
          type: 'prompt',
          position: {
            x: categoryPos.x + NODE_SIZES.category.width + 60,
            y: categoryPos.y - NODE_SIZES.prompt.height / 2 + NODE_SIZES.category.height / 2
          },
          data: {
            promptTemplate: data.promptTemplate,
            followTemplate: data.agent?.followTemplate || false,
            systemPrompt: data.agent?.systemPrompt || ''
          },
          draggable: true
        })
        break

      case 'advanced':
        result.push({
          id: 'advanced-config',
          type: 'advanced-config',
          position: {
            x: categoryPos.x + NODE_SIZES.category.width + 60,
            y: categoryPos.y - NODE_SIZES.advanced.height / 2 + NODE_SIZES.category.height / 2
          },
          data: {
            enablePlanning: data.agent?.enablePlanning || false,
            enableMemory: data.agent?.enableMemory || false,
            enableMemoryCompression: data.agent?.enableMemoryCompression || false,
            structuredOutputEnabled: data.agent?.structuredOutputEnabled || false,
            maxIterations: data.agent?.maxIterations || 10,
            maxSubtasks: data.agent?.maxSubtasks || 5
          },
          draggable: true
        })
        break

      case 'sensitive':
        if (data.sensitiveConfig) {
          result.push({
            id: `sensitive-${data.sensitiveConfig.id}`,
            type: 'sensitive-item',
            position: {
              x: categoryPos.x + NODE_SIZES.category.width + 60,
              y: categoryPos.y - NODE_SIZES.item.height / 2 + NODE_SIZES.category.height / 2
            },
            data: { sensitive: data.sensitiveConfig },
            draggable: true
          })
        }
        break
    }
  })

  return result
})

/**
 * 生成连线
 */
const edges = computed<Edge[]>(() => {
  if (!data.agent) return []

  const result: Edge[] = []

  // 中心到分类的连线
  activeCategories.value.forEach((category) => {
    const config = CATEGORY_CONFIGS[category]
    result.push({
      id: `e-center-${category}`,
      source: 'center',
      target: `category-${category}`,
      type: 'smoothstep',
      animated: true,
      style: { stroke: config.color, strokeWidth: 2 }
    })

    // 分类到配置项的连线
    switch (category) {
      case 'tool':
        data.tools.forEach(tool => {
          result.push({
            id: `e-category-tool-${tool.id}`,
            source: `category-${category}`,
            sourceHandle: 'right',
            target: `tool-${tool.id}`,
            targetHandle: 'left',
            type: 'smoothstep',
            style: { stroke: config.borderColor, strokeWidth: 1.5 }
          })
        })
        break

      case 'hook':
        data.hooks.forEach(hook => {
          result.push({
            id: `e-category-hook-${hook.id}`,
            source: `category-${category}`,
            sourceHandle: 'right',
            target: `hook-${hook.id}`,
            targetHandle: 'left',
            type: 'smoothstep',
            style: { stroke: config.borderColor, strokeWidth: 1.5 }
          })
        })
        break

      case 'skill':
        data.skills.forEach(skill => {
          result.push({
            id: `e-category-skill-${skill.id}`,
            source: `category-${category}`,
            sourceHandle: 'right',
            target: `skill-${skill.id}`,
            targetHandle: 'left',
            type: 'smoothstep',
            style: { stroke: config.borderColor, strokeWidth: 1.5 }
          })
        })
        break

      case 'mcp':
        data.mcps.forEach(mcp => {
          result.push({
            id: `e-category-mcp-${mcp.id}`,
            source: `category-${category}`,
            sourceHandle: 'right',
            target: `mcp-${mcp.id}`,
            targetHandle: 'left',
            type: 'smoothstep',
            style: { stroke: config.borderColor, strokeWidth: 1.5 }
          })
        })
        break

      case 'knowledge':
        data.knowledgeBases.forEach(kb => {
          result.push({
            id: `e-category-knowledge-${kb.id}`,
            source: `category-${category}`,
            sourceHandle: 'right',
            target: `knowledge-${kb.id}`,
            targetHandle: 'left',
            type: 'smoothstep',
            style: { stroke: config.borderColor, strokeWidth: 1.5 }
          })
        })
        break

      case 'sub-agent':
        data.subAgents.forEach(agent => {
          result.push({
            id: `e-category-agent-${agent.id}`,
            source: `category-${category}`,
            sourceHandle: 'right',
            target: `agent-${agent.id}`,
            targetHandle: 'left',
            type: 'smoothstep',
            style: { stroke: config.borderColor, strokeWidth: 1.5 }
          })
        })
        break

      case 'model':
        result.push({
          id: `e-category-model`,
          source: `category-${category}`,
          sourceHandle: 'right',
          target: 'model-config',
          targetHandle: 'left',
          type: 'smoothstep',
          style: { stroke: config.borderColor, strokeWidth: 1.5 }
        })
        break

      case 'prompt':
        result.push({
          id: `e-category-prompt`,
          source: `category-${category}`,
          sourceHandle: 'right',
          target: 'prompt-config',
          targetHandle: 'left',
          type: 'smoothstep',
          style: { stroke: config.borderColor, strokeWidth: 1.5 }
        })
        break

      case 'advanced':
        result.push({
          id: `e-category-advanced`,
          source: `category-${category}`,
          sourceHandle: 'right',
          target: 'advanced-config',
          targetHandle: 'left',
          type: 'smoothstep',
          style: { stroke: config.borderColor, strokeWidth: 1.5 }
        })
        break

      case 'sensitive':
        if (data.sensitiveConfig) {
          result.push({
            id: `e-category-sensitive`,
            source: `category-${category}`,
            sourceHandle: 'right',
            target: `sensitive-${data.sensitiveConfig.id}`,
            targetHandle: 'left',
            type: 'smoothstep',
            style: { stroke: config.borderColor, strokeWidth: 1.5 }
          })
        }
        break
    }
  })

  return result
})

/**
 * 加载数据
 */
async function loadData() {
  await loadArchitectureData(props.agentId)
  setTimeout(() => {
    fitView({ padding: 0.15, duration: 800 })
  }, 100)
}

/**
 * 监听agentId变化
 */
watch(() => props.agentId, () => {
  resetData()
  loadData()
})

/**
 * 初始化
 */
onMounted(() => {
  loadData()
})
</script>

<template>
  <div class="agent-architecture-diagram">
    <Spin :spinning="loading" tip="正在加载架构数据...">
      <div class="diagram-container">
        <VueFlow
          :nodes="nodes"
          :edges="edges"
          :node-types="nodeTypes"
          :fit-view-on-init="false"
          :zoom-on-scroll="true"
          :pan-on-drag="true"
          :nodes-connectable="false"
          :elements-selectable="true"
          :default-viewport="{ x: 0, y: 0, zoom: 0.8 }"
        >
          <Background pattern-color="#e8e8e8" :gap="24" variant="dots" />
          <MiniMap position="bottom-right" :pannable="true" :zoomable="true" />
          <Controls position="bottom-left" />
        </VueFlow>
      </div>
    </Spin>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/agent/architecture-diagram.scss' as *;
</style>
