<template>
  <div class="test-container">
    <div class="test-card">
      <div class="test-header">
        <div class="icon-wrapper">
          <el-icon :size="48"><Connection /></el-icon>
        </div>
        <h1>AI Agent Platform</h1>
        <p class="subtitle">前后端连通性测试</p>
      </div>

      <div class="test-content">
        <div v-if="!tested" class="initial-state">
          <p class="description">
            点击下方按钮测试前后端 API 连接状态
          </p>
        </div>

        <div v-else class="result-state">
          <div v-if="isConnected" class="success-result">
            <div class="result-icon">
              <el-icon :size="64" color="#67C23A"><SuccessFilled /></el-icon>
            </div>
            <h2>连接成功 ✓</h2>
            <div class="response-box">
              <div class="response-label">API 响应内容：</div>
              <div class="response-content">{{ responseData }}</div>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">请求地址：</span>
                <span class="info-value">{{ apiUrl }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">响应时间：</span>
                <span class="info-value">{{ responseTime }}ms</span>
              </div>
            </div>
          </div>

          <div v-else class="error-result">
            <div class="result-icon">
              <el-icon :size="64" color="#F56C6C"><CircleCloseFilled /></el-icon>
            </div>
            <h2>连接不通 ✗</h2>
            <div class="error-box">
              <div class="error-label">错误信息：</div>
              <div class="error-content">{{ errorMessage }}</div>
            </div>
            <div class="error-tips">
              <p><strong>可能的原因：</strong></p>
              <ul>
                <li>后端服务未启动</li>
                <li>网络连接异常</li>
                <li>API 地址配置错误</li>
                <li>CORS 跨域问题</li>
              </ul>
            </div>
          </div>
        </div>

        <div class="action-area">
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :icon="Refresh"
            @click="handleTest"
          >
            {{ tested ? '重新测试' : '前后端连通性测试' }}
          </el-button>
          
          <el-button
            v-if="tested && isConnected"
            type="success"
            size="large"
            :icon="Right"
            @click="goToLogin"
          >
            前往登录
          </el-button>
        </div>
      </div>

      <div class="test-footer">
        <p>© 2025 AI Agent Platform - System Test</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Connection, SuccessFilled, CircleCloseFilled, Refresh, Right } from '@element-plus/icons-vue'
import { getHello } from '@/api'

const router = useRouter()

const loading = ref(false)
const tested = ref(false)
const isConnected = ref(false)
const responseData = ref('')
const errorMessage = ref('')
const responseTime = ref(0)
const apiUrl = ref('')

const handleTest = async () => {
  loading.value = true
  tested.value = false
  isConnected.value = false
  responseData.value = ''
  errorMessage.value = ''
  responseTime.value = 0

  const startTime = Date.now()
  
  try {
    // 获取 API 基础地址
    const baseApi = import.meta.env.VITE_BASE_API || '/api'
    apiUrl.value = `${baseApi}/v1/hello`
    
    const result = await getHello()
    responseTime.value = Date.now() - startTime
    
    responseData.value = result || 'Hello'
    isConnected.value = true
  } catch (error: any) {
    responseTime.value = Date.now() - startTime
    errorMessage.value = error?.message || '未知错误'
    isConnected.value = false
  } finally {
    tested.value = true
    loading.value = false
  }
}

const goToLogin = () => {
  router.push({ name: 'login' })
}
</script>

<style scoped>
.test-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0ea5e9 0%, #0891b2 100%);
  padding: 20px;
}

.test-card {
  width: 100%;
  max-width: 600px;
  background: #ffffff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.test-header {
  padding: 40px 30px 30px;
  text-align: center;
  background: linear-gradient(135deg, #0ea5e9 0%, #0891b2 100%);
  color: #ffffff;
}

.icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  margin-bottom: 20px;
  backdrop-filter: blur(10px);
}

.test-header h1 {
  margin: 0 0 10px 0;
  font-size: 28px;
  font-weight: 700;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.subtitle {
  margin: 0;
  font-size: 16px;
  opacity: 0.95;
  font-weight: 400;
}

.test-content {
  padding: 40px 30px;
}

.initial-state {
  text-align: center;
  padding: 20px 0;
}

.description {
  font-size: 16px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

.result-state {
  text-align: center;
}

.result-icon {
  margin-bottom: 20px;
  animation: scaleIn 0.5s ease-out;
}

@keyframes scaleIn {
  from {
    transform: scale(0);
  }
  to {
    transform: scale(1);
  }
}

.success-result h2,
.error-result h2 {
  margin: 0 0 24px 0;
  font-size: 24px;
  font-weight: 600;
}

.success-result h2 {
  color: #67C23A;
}

.error-result h2 {
  color: #F56C6C;
}

.response-box,
.error-box {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  text-align: left;
}

.response-label,
.error-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
  font-weight: 500;
}

.response-content {
  font-size: 18px;
  color: #303133;
  font-weight: 600;
  font-family: 'Monaco', 'Courier New', monospace;
}

.error-content {
  font-size: 14px;
  color: #F56C6C;
  font-family: 'Monaco', 'Courier New', monospace;
  word-break: break-word;
}

.info-grid {
  display: grid;
  gap: 12px;
  text-align: left;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
}

.info-label {
  font-size: 13px;
  color: #909399;
  flex-shrink: 0;
}

.info-value {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  word-break: break-all;
}

.error-tips {
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 8px;
  padding: 16px;
  text-align: left;
  margin-top: 16px;
}

.error-tips p {
  margin: 0 0 8px 0;
  color: #F56C6C;
  font-size: 14px;
}

.error-tips ul {
  margin: 0;
  padding-left: 20px;
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
}

.action-area {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 32px;
}

.action-area .el-button {
  min-width: 140px;
}

.test-footer {
  padding: 20px 30px;
  background: #f5f7fa;
  text-align: center;
  border-top: 1px solid #e4e7ed;
}

.test-footer p {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

/* 响应式设计 */
@media (max-width: 640px) {
  .test-card {
    border-radius: 0;
  }

  .test-header {
    padding: 30px 20px 20px;
  }

  .test-header h1 {
    font-size: 24px;
  }

  .test-content {
    padding: 30px 20px;
  }

  .action-area {
    flex-direction: column;
  }

  .action-area .el-button {
    width: 100%;
  }
}
</style>
