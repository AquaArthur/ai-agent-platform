<template>
  <div
    class="end-node workflow-node-base"
    :style="{
      background: props.data?.color?.bg || 'linear-gradient(135deg, #ef4444 0%, #dc2626 100%)',
      borderColor: props.data?.color?.border || 'rgba(255, 255, 255, 0.3)',
      '--node-hover-shadow-color': 'rgba(239, 68, 68, 0.45)',
      '--node-selected-shadow-color': 'rgba(239, 68, 68, 0.3)',
      '--node-selected-shadow-color-strong': 'rgba(239, 68, 68, 0.4)',
      '--node-pulse-shadow-color-1': 'rgba(239, 68, 68, 0.3)',
      '--node-pulse-shadow-color-2': 'rgba(239, 68, 68, 0.4)',
      '--node-pulse-shadow-color-3': 'rgba(239, 68, 68, 0.2)',
      '--node-pulse-shadow-color-4': 'rgba(239, 68, 68, 0.5)'
    }"
  >
    <!-- 结束节点只有输入连接点（左侧） -->
    <Handle
      type="target"
      :position="Position.Left"
      class="node-handle"
    />
    <div class="node-icon">
      <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" fill="none"/>
        <path d="M8 12l4 4 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M12 8v8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="node-label">结束</div>
  </div>
</template>

<script setup lang="ts">
import { Handle, Position } from '@vue-flow/core'

interface Props {
  data?: {
    label?: string;
    config?: Record<string, any>;
    color?: { bg: string; border: string };
  }
}

const props = defineProps<Props>()
</script>

<style scoped>
/* 使用公共样式类，只保留节点特定的样式 */
.end-node {
  box-shadow: 
    0 8px 24px rgba(239, 68, 68, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset,
    0 1px 0 rgba(255, 255, 255, 0.2) inset;
}

.end-node .node-icon {
  transform: rotate(180deg);
}

.end-node:hover .node-icon svg {
  transform: rotate(180deg) scale(1.1) rotate(-5deg);
}
</style>
