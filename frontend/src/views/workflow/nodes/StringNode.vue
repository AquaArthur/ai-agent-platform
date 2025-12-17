<template>
  <div
    class="string-node workflow-node-base"
    :style="{
      background: data?.color?.bg || 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)',
      borderColor: data?.color?.border || 'rgba(255, 255, 255, 0.3)',
      '--node-hover-shadow-color': 'rgba(139, 92, 246, 0.45)',
      '--node-selected-shadow-color': 'rgba(139, 92, 246, 0.3)',
      '--node-selected-shadow-color-strong': 'rgba(139, 92, 246, 0.4)',
      '--node-pulse-shadow-color-1': 'rgba(139, 92, 246, 0.3)',
      '--node-pulse-shadow-color-2': 'rgba(139, 92, 246, 0.4)',
      '--node-pulse-shadow-color-3': 'rgba(139, 92, 246, 0.2)',
      '--node-pulse-shadow-color-4': 'rgba(139, 92, 246, 0.5)'
    }"
  >
    <!-- 输入连接点（左侧） -->
    <Handle
      type="target"
      :position="Position.Left"
      class="node-handle"
    />
    <div class="node-icon">
      <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M4 7h16M4 12h16M4 17h10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
      </svg>
    </div>
    <div class="node-label">字符串处理</div>
    <div v-if="hasConfig" class="node-badge">✓</div>
    <!-- 输出连接点（右侧） -->
    <Handle
      type="source"
      :position="Position.Right"
      class="node-handle"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'

interface Props {
  data?: {
    label?: string;
    config?: Record<string, any>;
    color?: { bg: string; border: string };
  }
}

const props = defineProps<Props>()

const hasConfig = computed(() => {
  return props.data?.config && Object.keys(props.data.config).length > 0
})
</script>

<style scoped>
/* 使用公共样式类，只保留节点特定的样式 */
.string-node {
  box-shadow: 
    0 8px 24px rgba(139, 92, 246, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset,
    0 1px 0 rgba(255, 255, 255, 0.2) inset;
}
</style>
