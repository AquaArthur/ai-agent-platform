<template>
  <div
    class="start-node workflow-node-base"
    :style="{
      background: data.color?.bg || 'linear-gradient(135deg, #0ea5e9 0%, #0891b2 100%)',
      borderColor: data.color?.border || 'rgba(255, 255, 255, 0.3)',
      '--node-hover-shadow-color': 'rgba(14, 165, 233, 0.45)',
      '--node-selected-shadow-color': 'rgba(14, 165, 233, 0.3)',
      '--node-selected-shadow-color-strong': 'rgba(14, 165, 233, 0.4)',
      '--node-pulse-shadow-color-1': 'rgba(14, 165, 233, 0.3)',
      '--node-pulse-shadow-color-2': 'rgba(14, 165, 233, 0.4)',
      '--node-pulse-shadow-color-3': 'rgba(14, 165, 233, 0.2)',
      '--node-pulse-shadow-color-4': 'rgba(14, 165, 233, 0.5)'
    }"
  >
    <!-- 开始节点只有输出连接点（右侧） -->
    <Handle
      type="source"
      :position="Position.Right"
      class="node-handle"
    />
    <div class="node-icon">
      <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2" fill="none"/>
        <path d="M8 12l4 4 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <path d="M12 8v8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </div>
    <div class="node-label">开始</div>
    <div v-if="hasConfig" class="node-badge">✓</div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Handle, Position } from '@vue-flow/core'

interface Props {
  data: {
    label?: string;
    config?: Record<string, any>;
    color?: { bg: string; border: string };
  }
}

const props = defineProps<Props>()

const hasConfig = computed(() => {
  return props.data.config && Object.keys(props.data.config).length > 0
})
</script>

<style scoped>
/* 使用公共样式类，只保留节点特定的样式 */
.start-node {
  box-shadow: 
    0 8px 24px rgba(14, 165, 233, 0.35),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset,
    0 1px 0 rgba(255, 255, 255, 0.2) inset;
}

.start-node:hover .node-icon svg {
  transform: scale(1.1) rotate(5deg);
}
</style>
