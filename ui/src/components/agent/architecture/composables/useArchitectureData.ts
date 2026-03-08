/**
 * 智能体架构图数据获取Composable
 *
 * @author huxuehao
 */

import { ref, reactive } from 'vue'
import type {
  ArchitectureData,
  ArchitectureLoadingState
} from '../types'
import type {
  AgentDefinitionVO,
  ToolVO,
  HookConfigVO,
  SkillPackageVO,
  McpServerVO,
  KnowledgeBaseConfigVO,
  ModelConfigVO,
  ModelProviderVO,
  SystemPromptTemplateVO,
  SensitiveWordConfigVO
} from '@/types'
import * as agentApi from '@/api/agent'
import * as toolApi from '@/api/tool'
import * as hookApi from '@/api/hook'
import * as skillApi from '@/api/skill'
import * as mcpApi from '@/api/mcp'
import * as knowledgeApi from '@/api/knowledge'
import * as modelApi from '@/api/model'
import * as promptApi from '@/api/prompt'
import * as sensitiveApi from '@/api/sensitive'

/**
 * 架构图数据获取Hook
 */
export function useArchitectureData() {
  const loading = ref(false)
  const loadingState = reactive<ArchitectureLoadingState>({
    agent: false,
    tools: false,
    hooks: false,
    skills: false,
    mcps: false,
    knowledgeBases: false,
    subAgents: false,
    model: false,
    prompt: false,
    sensitive: false
  })

  const data = reactive<ArchitectureData>({
    agent: null,
    tools: [],
    hooks: [],
    skills: [],
    mcps: [],
    knowledgeBases: [],
    subAgents: [],
    modelConfig: null,
    modelProvider: null,
    promptTemplate: null,
    sensitiveConfig: null
  })

  /**
   * 批量获取工具详情
   */
  async function fetchTools(ids: string[]): Promise<ToolVO[]> {
    if (!ids || ids.length === 0) return []
    loadingState.tools = true
    try {
      const results = await Promise.allSettled(
        ids.map(id => toolApi.detail(id))
      )
      const items: ToolVO[] = []
      for (const result of results) {
        if (result.status === 'fulfilled' && result.value?.data?.data) {
          items.push(result.value.data.data)
        }
      }
      return items
    } catch (error) {
      console.error('获取工具详情失败:', error)
      return []
    } finally {
      loadingState.tools = false
    }
  }

  /**
   * 批量获取钩子详情
   */
  async function fetchHooks(ids: string[]): Promise<HookConfigVO[]> {
    if (!ids || ids.length === 0) return []
    loadingState.hooks = true
    try {
      const results = await Promise.allSettled(
        ids.map(id => hookApi.detail(id))
      )
      const items: HookConfigVO[] = []
      for (const result of results) {
        if (result.status === 'fulfilled' && result.value?.data?.data) {
          items.push(result.value.data.data)
        }
      }
      return items
    } catch (error) {
      console.error('获取钩子详情失败:', error)
      return []
    } finally {
      loadingState.hooks = false
    }
  }

  /**
   * 批量获取技能详情
   */
  async function fetchSkills(ids: string[]): Promise<SkillPackageVO[]> {
    if (!ids || ids.length === 0) return []
    loadingState.skills = true
    try {
      const results = await Promise.allSettled(
        ids.map(id => skillApi.detail(id))
      )
      const items: SkillPackageVO[] = []
      for (const result of results) {
        if (result.status === 'fulfilled' && result.value?.data?.data) {
          items.push(result.value.data.data)
        }
      }
      return items
    } catch (error) {
      console.error('获取技能详情失败:', error)
      return []
    } finally {
      loadingState.skills = false
    }
  }

  /**
   * 批量获取MCP详情
   */
  async function fetchMcps(ids: string[]): Promise<McpServerVO[]> {
    if (!ids || ids.length === 0) return []
    loadingState.mcps = true
    try {
      const results = await Promise.allSettled(
        ids.map(id => mcpApi.detail(id))
      )
      const items: McpServerVO[] = []
      for (const result of results) {
        if (result.status === 'fulfilled' && result.value?.data?.data) {
          items.push(result.value.data.data)
        }
      }
      return items
    } catch (error) {
      console.error('获取MCP详情失败:', error)
      return []
    } finally {
      loadingState.mcps = false
    }
  }

  /**
   * 批量获取知识库详情
   */
  async function fetchKnowledgeBases(ids: string[]): Promise<KnowledgeBaseConfigVO[]> {
    if (!ids || ids.length === 0) return []
    loadingState.knowledgeBases = true
    try {
      const results = await Promise.allSettled(
        ids.map(id => knowledgeApi.detail(id))
      )
      const items: KnowledgeBaseConfigVO[] = []
      for (const result of results) {
        if (result.status === 'fulfilled' && result.value?.data?.data) {
          items.push(result.value.data.data)
        }
      }
      return items
    } catch (error) {
      console.error('获取知识库详情失败:', error)
      return []
    } finally {
      loadingState.knowledgeBases = false
    }
  }

  /**
   * 批量获取子智能体详情
   */
  async function fetchSubAgents(ids: string[]): Promise<AgentDefinitionVO[]> {
    if (!ids || ids.length === 0) return []
    loadingState.subAgents = true
    try {
      const results = await Promise.allSettled(
        ids.map(id => agentApi.detail(id))
      )
      const items: AgentDefinitionVO[] = []
      for (const result of results) {
        if (result.status === 'fulfilled' && result.value?.data?.data) {
          items.push(result.value.data.data)
        }
      }
      return items
    } catch (error) {
      console.error('获取子智能体详情失败:', error)
      return []
    } finally {
      loadingState.subAgents = false
    }
  }

  /**
   * 获取模型配置详情
   */
  async function fetchModelConfig(id: string): Promise<{ config: ModelConfigVO | null; provider: ModelProviderVO | null }> {
    if (!id) return { config: null, provider: null }
    loadingState.model = true
    try {
      const configRes = await modelApi.configDetail(id)
      const config = configRes.data.data
      let provider: ModelProviderVO | null = null
      if (config?.providerId) {
        const providerRes = await modelApi.providerDetail(config.providerId)
        provider = providerRes.data.data
      }
      return { config, provider }
    } catch (error) {
      console.error('获取模型配置详情失败:', error)
      return { config: null, provider: null }
    } finally {
      loadingState.model = false
    }
  }

  /**
   * 获取提示词模板详情
   */
  async function fetchPromptTemplate(id: string): Promise<SystemPromptTemplateVO | null> {
    if (!id) return null
    loadingState.prompt = true
    try {
      const res = await promptApi.detail(id)
      return res.data.data
    } catch (error) {
      console.error('获取提示词模板详情失败:', error)
      return null
    } finally {
      loadingState.prompt = false
    }
  }

  /**
   * 获取敏感词配置详情
   */
  async function fetchSensitiveConfig(id: string): Promise<SensitiveWordConfigVO | null> {
    if (!id) return null
    loadingState.sensitive = true
    try {
      const res = await sensitiveApi.detail(id)
      return res.data.data
    } catch (error) {
      console.error('获取敏感词配置详情失败:', error)
      return null
    } finally {
      loadingState.sensitive = false
    }
  }

  /**
   * 加载所有架构数据
   */
  async function loadArchitectureData(agentId: string): Promise<void> {
    loading.value = true
    loadingState.agent = true

    try {
      // 首先获取智能体基本信息
      const agentRes = await agentApi.detail(agentId)
      const agent = agentRes.data.data
      data.agent = agent
      loadingState.agent = false

      if (!agent) {
        console.error('智能体数据为空')
        return
      }

      // 并行获取所有关联配置
      const [
        tools,
        hooks,
        skills,
        mcps,
        knowledgeBases,
        subAgents,
        modelData,
        promptTemplate,
        sensitiveConfig
      ] = await Promise.all([
        fetchTools(agent.tool || []),
        fetchHooks(agent.hook || []),
        fetchSkills(agent.skill || []),
        fetchMcps(agent.mcp || []),
        fetchKnowledgeBases(agent.knowledgeBase || []),
        fetchSubAgents(agent.subAgent || []),
        fetchModelConfig(agent.modelConfigId),
        fetchPromptTemplate(agent.systemPromptTemplateId),
        agent.sensitiveFilterEnabled && agent.sensitiveWordConfigId
          ? fetchSensitiveConfig(agent.sensitiveWordConfigId)
          : Promise.resolve(null)
      ])

      // 更新数据
      data.tools = tools
      data.hooks = hooks
      data.skills = skills
      data.mcps = mcps
      data.knowledgeBases = knowledgeBases
      data.subAgents = subAgents
      data.modelConfig = modelData.config
      data.modelProvider = modelData.provider
      data.promptTemplate = promptTemplate
      data.sensitiveConfig = sensitiveConfig
    } catch (error) {
      console.error('加载架构数据失败:', error)
    } finally {
      loading.value = false
    }
  }

  /**
   * 重置数据
   */
  function resetData(): void {
    data.agent = null
    data.tools = []
    data.hooks = []
    data.skills = []
    data.mcps = []
    data.knowledgeBases = []
    data.subAgents = []
    data.modelConfig = null
    data.modelProvider = null
    data.promptTemplate = null
    data.sensitiveConfig = null
  }

  return {
    loading,
    loadingState,
    data,
    loadArchitectureData,
    resetData
  }
}
