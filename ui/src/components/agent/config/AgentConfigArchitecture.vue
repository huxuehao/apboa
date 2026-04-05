/**
 * 智能体配置-架构图子组件
 *
 * @component
 */
<script setup lang="ts">
import AgentArchitectureDiagram from '@/components/agent/architecture/AgentArchitectureDiagram.vue'

defineProps<{
  agentId: string
}>()
</script>

<template>
  <div class="config-architecture">
    <AgentArchitectureDiagram v-if="agentId" :agent-id="agentId" />
  </div>
</template>

<style scoped lang="scss">
.config-architecture {
  height: calc(100vh - 120px);
  min-height: 500px;
}
</style>
