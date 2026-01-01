<template>
  <div class="model-list-container">
    <div class="page-header">
      <h2>模型管理</h2>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-section">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索模型名称、提供商或描述"
            clearable
            :prefix-icon="Search"
            @input="handleSearch"
          />
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="filterStatus"
            placeholder="筛选状态"
            clearable
            @change="handleSearch"
            style="width: 100%"
          >
            <el-option label="已激活" :value="true" />
            <el-option label="未激活" :value="false" />
          </el-select>
        </el-col>
        <el-col :span="10" style="text-align: right;">
          <el-button @click="resetFilters">重置筛选</el-button>
          <el-button 
            v-if="isAdmin" 
            type="primary" 
            :icon="Plus" 
            @click="handleCreate"
          >
            新增模型
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 模型列表 - 卡片形式 -->
    <div v-loading="loading" class="models-grid">
      <el-empty v-if="!loading && paginatedModelList.length === 0" description="暂无模型数据">
        <el-button v-if="isAdmin" type="primary" @click="handleCreate">创建第一个模型</el-button>
      </el-empty>
      
      <el-card
        v-for="model in paginatedModelList"
        :key="model.id"
        class="model-card"
        shadow="hover"
        :body-style="{ padding: '0' }"
      >
        <div class="card-header">
          <div class="header-top">
            <div class="model-icon">
              <el-icon size="24"><Cpu /></el-icon>
            </div>
            <div class="model-info">
              <h3 class="model-name">{{ model.displayName || model.name }}</h3>
              <div class="model-badges">
                <el-tag :type="model.isActive ? 'success' : 'info'" size="small">
                  {{ model.isActive ? '已激活' : '未激活' }}
                </el-tag>
                <el-tag v-if="model.isDefault" type="warning" size="small">默认</el-tag>
                <el-tag v-if="model.isSystem" type="info" size="small">系统</el-tag>
              </div>
            </div>
          </div>
        </div>
        
        <div class="card-body">
          <el-tooltip
            :content="model.description || '暂无描述'"
            placement="top"
            effect="dark"
            :disabled="!model.description"
            :show-after="200"
          >
            <div class="model-description">{{ model.description || '暂无描述' }}</div>
          </el-tooltip>
          
          <div class="model-stats">
            <div class="stat-item">
              <el-icon><Connection /></el-icon>
              <span>{{ model.provider || 'N/A' }}</span>
            </div>
            <div class="stat-item">
              <el-icon><Setting /></el-icon>
              <span>{{ model.modelType || 'N/A' }}</span>
            </div>
            <div class="stat-item">
              <el-icon><Clock /></el-icon>
              <span>{{ formatDateTime(model.createdAt) }}</span>
            </div>
          </div>
        </div>
        
        <div class="card-footer">
          <el-button 
            v-if="isAdmin"
            :type="model.isActive ? 'warning' : 'success'" 
            size="small" 
            @click="handleToggleStatus(model)"
          >
            <el-icon><Switch /></el-icon>
            {{ model.isActive ? '禁用' : '启用' }}
          </el-button>
          <el-button 
            v-if="isAdmin"
            type="primary" 
            size="small" 
            @click="handleEdit(model)"
          >
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button
            v-if="isAdmin && !model.isSystem"
            type="danger"
            size="small"
            @click="handleDelete(model)"
          >
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <el-pagination
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.pageSize"
      :total="pagination.total"
      :page-sizes="[12, 24, 48]"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handlePageChange"
      @current-change="handlePageChange"
      style="margin-top: 20px; justify-content: center;"
    />
  </div>
    <!-- 模型编辑/创建弹窗 -->
    <ModelDialog
      v-model="dialogVisible"
      :model="currentModel"
      @success="handleDialogSuccess"
    />
  </template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  Edit,
  Delete,
  Switch,
  Cpu,
  Connection,
  Setting,
  Clock
} from '@element-plus/icons-vue'
import { getLlmModelList, deleteLlmModel, updateLlmModel } from '@/api/llm'
import type { LlmModel } from '@/types/entity'
import { formatDateTime } from '@/utils/formatters'
import { useUserStore } from '@/stores/useUserStore'
import ModelDialog from './ModelDialog.vue' // 导入 ModelDialog 组件

// 用户权限
const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

// 搜索关键词
const searchKeyword = ref('')
// 状态筛选
const filterStatus = ref<boolean | null>(null)
// 加载状态
const loading = ref(false)
// 模型列表
const models = ref<LlmModel[]>([])
// 弹窗可见性
const dialogVisible = ref(false)
// 当前编辑的模型
const currentModel = ref<LlmModel | null>(null)

// 分页配置
const pagination = reactive({
  page: 1,
  pageSize: 12,
  total: 0
})

// 过滤后的模型列表（未分页）
const filteredModelList = computed(() => {
  let list = models.value

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(
      (model) =>
        model.name?.toLowerCase().includes(keyword) ||
        model.displayName?.toLowerCase().includes(keyword) ||
        model.provider?.toLowerCase().includes(keyword) ||
        model.description?.toLowerCase().includes(keyword)
    )
  }

  // 状态筛选
  if (filterStatus.value !== null) {
    list = list.filter((model) => model.isActive === filterStatus.value)
  }

  // 更新总数
  pagination.total = list.length

  return list
})

// 分页后的模型列表
const paginatedModelList = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filteredModelList.value.slice(start, end)
})

// 搜索处理
const handleSearch = () => {
  pagination.page = 1
}

// 重置筛选
const resetFilters = () => {
  searchKeyword.value = ''
  filterStatus.value = null
  pagination.page = 1
}

// 加载模型列表
const loadModels = async () => {
  loading.value = true
  try {
    models.value = await getLlmModelList()
  } catch (error: any) {
    ElMessage.error('加载模型列表失败: ' + (error.message || '未知错误'))
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 分页变化处理
const handlePageChange = () => {
  // 分页逻辑已由计算属性处理
}

// 创建模型
const handleCreate = () => {
  if (!isAdmin.value) {
    ElMessage.warning('只有管理员才能创建模型')
    return
  }
  currentModel.value = null
  dialogVisible.value = true
}

// 编辑模型
const handleEdit = (model: LlmModel) => {
  if (!isAdmin.value) {
    ElMessage.warning('只有管理员才能编辑模型')
    return
  }
  currentModel.value = { ...model }
  dialogVisible.value = true
}

// 删除模型
const handleDelete = async (model: LlmModel) => {
  if (!model.id) return

  if (!isAdmin.value) {
    ElMessage.warning('只有管理员才能删除模型')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除模型 "${model.displayName || model.name}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteLlmModel(model.id)
    ElMessage.success('删除成功')
    await loadModels()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
    }
  }
}

// 切换状态
const handleToggleStatus = async (model: LlmModel) => {
  if (!model.id) return
  
  if (!isAdmin.value) {
    ElMessage.warning('只有管理员才能修改模型状态')
    return
  }

  try {
    const newStatus = !model.isActive
    await updateLlmModel(model.id, {
      ...model,
      isActive: newStatus
    })
    ElMessage.success(newStatus ? '已启用' : '已禁用')
    await loadModels()
  } catch (error: any) {
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

// 初始化
onMounted(() => {
  loadModels()
})

// 弹窗成功回调
const handleDialogSuccess = () => {
  dialogVisible.value = false
  currentModel.value = null
  loadModels() // 重新加载模型列表，确保数据显示最新
}
</script>

<style scoped>
.model-list-container {
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: #f8fafc;
}

/* 使用公共样式类 */

/* 卡片网格布局 */
.models-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

/* 模型卡片 */
.model-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid var(--border-light, #e4e7ed);
  background: #ffffff;
}

.model-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* 卡片头部 - 使用公共样式 */
.model-card .card-header {
  padding: 20px;
  background: var(--gradient-bg-card-header);
}

.header-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.model-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  flex-shrink: 0;
}

.model-info {
  flex: 1;
  min-width: 0;
}

.model-name {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 卡片主体 - 使用公共样式 */
.model-card .card-body {
  padding: 20px;
  background: #ffffff;
}

.model-description {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: var(--text-regular, #606266);
  line-height: 1.6;
  min-height: 44px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.model-stats {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-secondary, #909399);
}

.stat-item .el-icon {
  font-size: 16px;
}

/* 卡片底部 - 使用公共样式 */
.model-card .card-footer {
  padding: 12px 20px;
  background: #f5f7fa;
  display: flex;
  gap: 8px;
  border-top: 1px solid var(--border-light, #e4e7ed);
}

.model-card .card-footer .el-button {
  flex: 1;
  min-width: 80px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .models-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .model-list-container {
    padding: 16px;
  }
  
  .models-grid {
    grid-template-columns: 1fr;
  }
}
</style>

