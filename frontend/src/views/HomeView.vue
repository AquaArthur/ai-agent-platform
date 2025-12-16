<template>
  <div class="home-container">
    <el-card class="test-card">
      <template #header>
        <div class="card-header">
          <h2>前后端连通性测试</h2>
        </div>
      </template>
      <div class="test-content">
        <div class="status-indicator" :class="{ success: message }">
          <el-icon v-if="!loading && message" size="48"><CircleCheck /></el-icon>
          <el-icon v-else-if="loading" class="is-loading" size="48"><Loading /></el-icon>
          <el-icon v-else size="48"><Warning /></el-icon>
        </div>
        <h3>{{ message || (loading ? '正在测试...' : '未连接') }}</h3>
        <el-button type="primary" @click="fetchHello" :loading="loading">
          重新测试
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { CircleCheck, Loading, Warning } from '@element-plus/icons-vue'
import { getHello } from '@/api'
import { showSuccess, handleError } from '@/utils/message'

const message = ref<string>('')
const loading = ref(false)

const fetchHello = async () => {
  loading.value = true
  message.value = ''
  try {
    const result = await getHello()
    message.value = result || '连接成功'
    showSuccess('前后端连接正常')
  } catch (error: any) {
    handleError(error, '前后端连接失败', true)
    message.value = error?.message || '连接失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchHello()
})
</script>

<style scoped>
.home-container {
  padding: 20px;
}

.test-card {
  max-width: 600px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

/* 使用公共样式类 */
.test-card .card-header {
  display: flex;
  justify-content: center;
  align-items: center;
}

.test-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  padding: 40px 20px;
  text-align: center;
}

.status-indicator {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  transition: all 0.3s ease;
}

.status-indicator.success {
  background: var(--gradient-bg-success);
  box-shadow: 0 8px 24px rgba(103, 194, 58, 0.3);
}

.status-indicator.success .el-icon {
  color: #ffffff;
}

.status-indicator .el-icon {
  color: #909399;
}

.test-content h3 {
  margin: 0;
  font-size: 1.25rem;
  font-weight: 500;
  color: var(--text-primary);
  min-height: 32px;
}
</style>

