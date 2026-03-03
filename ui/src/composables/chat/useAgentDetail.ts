import { ref, watch } from 'vue'
import * as agentApi from '@/api/agent'
import type { AgentDefinitionVO } from '@/types'
import { setPageTitle } from '@/router/guards.ts'

export function useAgentDetail(agentId: import('vue').Ref<string>) {
  const agentDetail = ref<AgentDefinitionVO | null>(null)
  const allowFileType = ref<string[]>([])

  const loadAgentDetail = async () => {
    if (!agentId.value) return
    try {
      const res = await agentApi.detail(agentId.value)
      agentDetail.value = res.data?.data ?? null
      setPageTitle(agentDetail.value.name)
    } catch {
      agentDetail.value = null
    }
  }

  const loadAllowFileType = async () => {
    if (!agentId.value) return
    try {
      const res = await agentApi.allowFileType(agentId.value)
      allowFileType.value = res.data?.data ?? []
    } catch {
      allowFileType.value = []
    }
  }

  watch(agentId, () => {
    loadAgentDetail().then(() => {})
    loadAllowFileType().then(() => {})
  }, { immediate: true })

  return {
    agentDetail,
    allowFileType,
    loadAgentDetail,
    loadAllowFileType
  }
}
