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
} as any

/**
 * 布局配置
 */
const LAYOUT = {
  centerX: 600,
  centerY: 450,
  categoryDistance: 320,
  itemStartDistance: 220,
  itemSpacingY: 130,
  directNodeY: 750,
  directNodeSpacing: 300
}

/**
 * 计算分类节点位置（在上半圆区域分布）
 */
function getCategoryPosition(index: number, total: number): { x: number; y: number } {
  // 角度范围：-180度到0度（上方180度半圆，最大分散）
  const startAngle = (-180 * Math.PI) / 180
  const endAngle = (0 * Math.PI) / 180
  const angleRange = endAngle - startAngle
  const angle = startAngle + (angleRange * index) / Math.max(total - 1, 1)
  return {
    x: LAYOUT.centerX + Math.cos(angle) * LAYOUT.categoryDistance - NODE_SIZES.category.width / 2,
    y: LAYOUT.centerY + Math.sin(angle) * LAYOUT.categoryDistance - NODE_SIZES.category.height / 2
  }
}

/**
 * 计算配置项节点位置（向外辐射排列）
 */
function getItemPosition(
  categoryPos: { x: number; y: number },
  index: number,
  total: number,
  categoryIndex: number,
  totalCategories: number
): { x: number; y: number } {
  // 计算分类节点的角度
  const startAngle = (-180 * Math.PI) / 180
  const endAngle = (0 * Math.PI) / 180
  const angleRange = endAngle - startAngle
  const categoryAngle = startAngle + (angleRange * categoryIndex) / Math.max(totalCategories - 1, 1)

  // 计算配置项的扇形展开
  const itemSpreadAngle = 0.15 // 每个配置项之间的角度偏移
  const totalSpread = (total - 1) * itemSpreadAngle
  const itemAngle = categoryAngle - totalSpread / 2 + index * itemSpreadAngle

  // 计算配置项位置（从分类节点向外延伸）
  const categoryCenterX = categoryPos.x + NODE_SIZES.category.width / 2
  const categoryCenterY = categoryPos.y + NODE_SIZES.category.height / 2

  return {
    x: categoryCenterX + Math.cos(itemAngle) * LAYOUT.itemStartDistance - NODE_SIZES.item.width / 2,
    y: categoryCenterY + Math.sin(itemAngle) * LAYOUT.itemStartDistance - NODE_SIZES.item.height / 2
  }
}

/**
 * 需要显示的分类列表（仅包含多对一关系的分类）
 */
const activeCategories = computed<CategoryType[]>(() => {
  const categories: CategoryType[] = []

  if (data.tools.length > 0) categories.push('tool')
  if (data.hooks.length > 0) categories.push('hook')
  if (data.skills.length > 0) categories.push('skill')
  if (data.mcps.length > 0) categories.push('mcp')
  if (data.knowledgeBases.length > 0) categories.push('knowledge')
  if (data.subAgents.length > 0) categories.push('sub-agent')

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

  // 遭历活跃的分类
  activeCategories.value.forEach((category, categoryIndex) => {
    const config = CATEGORY_CONFIGS[category]
    const categoryPos = getCategoryPosition(categoryIndex, totalCategories)

    // 根据分类类型添加X轴偏移
    let xOffset = 0
    if (category === 'tool') xOffset = -80
    if (category === 'sub-agent') xOffset = 80
    categoryPos.x += xOffset

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
          pos.x += xOffset // 配置项也应用相同偏移
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
          pos.x += xOffset // 配置项也应用相同偏移
          result.push({
            id: `agent-${agent.id}`,
            type: 'agent-item',
            position: pos,
            data: { agent },
            draggable: true
          })
        })
        break
    }
  })

  // 直连节点（模型配置、提示词、高级配置、敏感词）放置在中心节点下方
  const directNodes: { id: string; type: string; data: Record<string, unknown>; width: number }[] = []

  // 模型配置节点
  directNodes.push({
    id: 'model-config',
    type: 'model',
    data: {
      modelConfig: data.modelConfig,
      provider: data.modelProvider,
      paramsOverride: data.agent?.modelParamsOverride || null
    },
    width: NODE_SIZES.model.width
  })

  // 提示词节点
  directNodes.push({
    id: 'prompt-config',
    type: 'prompt',
    data: {
      promptTemplate: data.promptTemplate,
      followTemplate: data.agent?.followTemplate || false,
      systemPrompt: data.agent?.systemPrompt || ''
    },
    width: NODE_SIZES.prompt.width
  })

  // 高级配置节点
  directNodes.push({
    id: 'advanced-config',
    type: 'advanced-config',
    data: {
      enablePlanning: data.agent?.enablePlanning || false,
      enableMemory: data.agent?.enableMemory || false,
      enableMemoryCompression: data.agent?.enableMemoryCompression || false,
      structuredOutputEnabled: data.agent?.structuredOutputEnabled || false,
      maxIterations: data.agent?.maxIterations || 10,
      maxSubtasks: data.agent?.maxSubtasks || 5
    },
    width: NODE_SIZES.advanced.width
  })

  // 敏感词节点（仅在启用时显示）
  if (data.agent?.sensitiveFilterEnabled && data.sensitiveConfig) {
    directNodes.push({
      id: `sensitive-${data.sensitiveConfig.id}`,
      type: 'sensitive-item',
      data: { sensitive: data.sensitiveConfig },
      width: NODE_SIZES.item.width
    })
  }

  // 计算直连节点的水平布局
  const totalDirectWidth = directNodes.reduce((sum, node) => sum + node.width, 0)
  const totalSpacing = (directNodes.length - 1) * 40 // 节点间距
  const startX = LAYOUT.centerX - (totalDirectWidth + totalSpacing) / 2

  let currentX = startX
  directNodes.forEach((node) => {
    result.push({
      id: node.id,
      type: node.type,
      position: {
        x: currentX,
        y: LAYOUT.directNodeY
      },
      data: node.data,
      draggable: true
    })
    currentX += node.width + 40
  })

  return result
})

/**
 * 生成连线
 */
const edges = computed<Edge[]>(() => {
  if (!data.agent) return []

  const result: Edge[] = []

  // 统一边样式
  const edgeStyle = { stroke: '#d9d9d9', strokeWidth: 1.5 }

  // 中心到分类的连线
  activeCategories.value.forEach((category) => {
    result.push({
      id: `e-center-${category}`,
      source: 'center',
      target: `category-${category}`,
      type: 'default',
      style: edgeStyle
    })

    // 分类到配置项的连线
    switch (category) {
      case 'tool':
        data.tools.forEach(tool => {
          result.push({
            id: `e-category-tool-${tool.id}`,
            source: `category-${category}`,
            target: `tool-${tool.id}`,
            type: 'default',
            style: edgeStyle
          })
        })
        break

      case 'hook':
        data.hooks.forEach(hook => {
          result.push({
            id: `e-category-hook-${hook.id}`,
            source: `category-${category}`,
            target: `hook-${hook.id}`,
            type: 'default',
            style: edgeStyle
          })
        })
        break

      case 'skill':
        data.skills.forEach(skill => {
          result.push({
            id: `e-category-skill-${skill.id}`,
            source: `category-${category}`,
            target: `skill-${skill.id}`,
            type: 'default',
            style: edgeStyle
          })
        })
        break

      case 'mcp':
        data.mcps.forEach(mcp => {
          result.push({
            id: `e-category-mcp-${mcp.id}`,
            source: `category-${category}`,
            target: `mcp-${mcp.id}`,
            type: 'default',
            style: edgeStyle
          })
        })
        break

      case 'knowledge':
        data.knowledgeBases.forEach(kb => {
          result.push({
            id: `e-category-knowledge-${kb.id}`,
            source: `category-${category}`,
            target: `knowledge-${kb.id}`,
            type: 'default',
            style: edgeStyle
          })
        })
        break

      case 'sub-agent':
        data.subAgents.forEach(agent => {
          result.push({
            id: `e-category-agent-${agent.id}`,
            source: `category-${category}`,
            target: `agent-${agent.id}`,
            type: 'default',
            style: edgeStyle
          })
        })
        break
    }
  })

  // 中心直连四个配置节点的连线
  result.push({
    id: 'e-center-model',
    source: 'center',
    target: 'model-config',
    type: 'default',
    style: edgeStyle
  })

  result.push({
    id: 'e-center-prompt',
    source: 'center',
    target: 'prompt-config',
    type: 'default',
    style: edgeStyle
  })

  result.push({
    id: 'e-center-advanced',
    source: 'center',
    target: 'advanced-config',
    type: 'default',
    style: edgeStyle
  })

  // 敏感词节点连线（仅在启用时）
  if (data.agent?.sensitiveFilterEnabled && data.sensitiveConfig) {
    result.push({
      id: 'e-center-sensitive',
      source: 'center',
      target: `sensitive-${data.sensitiveConfig.id}`,
      type: 'default',
      style: edgeStyle
    })
  }

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
    <Spin :spinning="loading" tip="正在加载架构数据..." style="background-color: transparent">
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
