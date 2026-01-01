<template>
  <div class="plugin-list-container">
    <div class="page-header">
      <h2>插件管理</h2>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-section">
      <el-row :gutter="16">
        <el-col :span="6">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索插件名称、标识符或描述"
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
            <el-option label="已启用" value="enabled" />
            <el-option label="已禁用" value="disabled" />
          </el-select>
        </el-col>
        <el-col :span="12" class="button-group-col">
          <div class="button-group">
            <el-button @click="resetFilters">重置筛选</el-button>
            <el-button type="primary" :icon="Plus" @click="handleCreate">
              新增插件
            </el-button>
            <el-button type="success" :icon="Upload" @click="importFromFile">
              从文件导入
            </el-button>
          </div>
        </el-col>
      </el-row>
    </div>

    <!-- 插件列表 - 卡片形式 -->
    <div v-loading="loading || pluginStore.loading" class="plugins-grid">
      <el-empty v-if="!pluginStore.loading && paginatedPluginList.length === 0" description="暂无插件数据">
        <el-button type="primary" @click="handleCreate">创建第一个插件</el-button>
      </el-empty>
      
      <el-card
        v-for="plugin in paginatedPluginList"
        :key="plugin.id"
        class="plugin-card"
        shadow="hover"
        :body-style="{ padding: '0' }"
      >
        <div class="card-header">
          <div class="header-top">
            <div class="plugin-icon">
              <el-icon size="24"><Connection /></el-icon>
            </div>
            <div class="plugin-info">
              <h3 class="plugin-name">{{ plugin.name }}</h3>
              <div class="plugin-badges">
                <el-tag :type="isPluginEnabled(plugin) ? 'success' : 'info'" size="small">
                  {{ isPluginEnabled(plugin) ? '已启用' : '已禁用' }}
                </el-tag>
                <el-tag v-if="!plugin.userId" type="warning" size="small">系统插件</el-tag>
              </div>
            </div>
          </div>
        </div>
        
        <div class="card-body">
          <p class="plugin-description">{{ plugin.description || '暂无描述' }}</p>
          
          <div class="plugin-stats">
            <div class="stat-item">
              <el-icon><Document /></el-icon>
              <span>{{ plugin.identifier || 'N/A' }}</span>
            </div>
            <div class="stat-item" v-if="plugin.openapiSpec">
              <el-icon><Link /></el-icon>
              <span>{{ Object.keys(plugin.openapiSpec?.paths || {}).length }} 个API</span>
            </div>
            <div class="stat-item">
              <el-icon><Clock /></el-icon>
              <span>{{ formatDateTime(getCreateTime(plugin)) }}</span>
            </div>
          </div>
        </div>
        
        <div class="card-footer">
          <el-button size="small" @click="handleView(plugin)">
            <el-icon><View /></el-icon>
            查看
          </el-button>
          <el-button type="info" size="small" @click="handleTest(plugin)">
            <el-icon><VideoPlay /></el-icon>
            测试
          </el-button>
          <el-button type="primary" size="small" @click="handleEdit(plugin)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button
            type="danger"
            size="small"
            @click="handleDelete(plugin)"
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

    <!-- 插件编辑/创建弹窗 -->
    <PluginDialog
      v-model="dialogVisible"
      :plugin="currentPlugin"
      @success="handleDialogSuccess"
    />

    <!-- 插件测试弹窗 -->
    <el-dialog
      v-model="testDialogVisible"
      title="插件测试"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="testPlugin">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="插件名称">{{ testPlugin.name }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="isPluginEnabled(testPlugin) ? 'success' : 'info'">
              {{ isPluginEnabled(testPlugin) ? '已启用' : '已禁用' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <el-divider>选择操作</el-divider>
        
        <el-form label-width="100px">
          <el-form-item label="操作">
            <el-select v-model="selectedOperation" placeholder="选择要测试的操作" style="width: 100%">
              <el-option
                v-for="op in testOperations"
                :key="op.operationId"
                :label="`${op.name} (${op.method} ${op.path})`"
                :value="op.operationId"
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="参数 (JSON)">
            <el-input
              v-model="testParams"
              type="textarea"
              :rows="4"
              placeholder='{"sensor": "DHT11_temperature", "uuid": "your-device-uuid"}'
            />
          </el-form-item>
        </el-form>

        <el-divider>测试结果</el-divider>
        
        <div v-if="testLoading" class="test-loading">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>调用中...</span>
        </div>
        
        <div v-else-if="testResult" class="test-result">
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="状态">
              <el-tag :type="testResult.status === 'success' ? 'success' : 'danger'">
                {{ testResult.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="HTTP状态码">{{ testResult.httpStatusCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="耗时">{{ testResult.duration }}ms</el-descriptions-item>
            <el-descriptions-item label="错误信息">{{ testResult.errorMessage || '-' }}</el-descriptions-item>
          </el-descriptions>
          
          <div class="result-body" v-if="testResult.parsedData">
            <h4>响应数据：</h4>
            <pre>{{ JSON.stringify(testResult.parsedData, null, 2) }}</pre>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testLoading" @click="executeTest">
          执行测试
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看详情弹窗 -->
    <el-dialog
      v-model="viewDialogVisible"
      title="查看插件详情"
      width="900px"
    >
      <div v-if="viewPluginData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="插件名称">{{ viewPluginData.name }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="isPluginEnabled(viewPluginData) ? 'success' : 'info'">
              {{ isPluginEnabled(viewPluginData) ? '已启用' : '已禁用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标识符">{{ viewPluginData.identifier || '-' }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ viewPluginData.type || 'http' }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ viewPluginData.description || '无' }}</el-descriptions-item>
          <el-descriptions-item label="基础URL" :span="2">{{ viewPluginData.baseUrl || '-' }}</el-descriptions-item>
          <el-descriptions-item label="鉴权类型">{{ viewPluginData.authType || 'none' }}</el-descriptions-item>
          <el-descriptions-item label="API数量">
            {{ viewPluginData.operations?.length || Object.keys(viewPluginData.openapiSpec?.paths || {}).length || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(getCreateTime(viewPluginData)) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 操作列表 -->
        <el-divider v-if="viewPluginData.operations?.length">插件操作列表</el-divider>
        <el-table v-if="viewPluginData.operations?.length" :data="viewPluginData.operations" border size="small">
          <el-table-column prop="operationId" label="操作ID" width="150" />
          <el-table-column prop="name" label="名称" width="150" />
          <el-table-column prop="method" label="方法" width="80">
            <template #default="{ row }">
              <el-tag size="small" :type="getMethodTagType(row.method)">{{ row.method }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="path" label="路径" />
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
        </el-table>

        <!-- OpenAPI 规范 -->
        <el-divider v-if="viewPluginData.openapiSpec">OpenAPI 规范</el-divider>
        <pre v-if="viewPluginData.openapiSpec" class="openapi-spec">{{ JSON.stringify(viewPluginData.openapiSpec, null, 2) }}</pre>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
        <el-button 
          :type="isPluginEnabled(viewPluginData) ? 'warning' : 'success'" 
          @click="handleToggleStatusFromView"
        >
          {{ isPluginEnabled(viewPluginData) ? '禁用插件' : '启用插件' }}
        </el-button>
        <el-button type="primary" @click="handleEditFromView">编辑</el-button>
      </template>
    </el-dialog>

    <!-- 文件上传（隐藏） -->
    <input
      ref="fileInputRef"
      type="file"
      accept=".json"
      style="display: none"
      @change="handleFileImport"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Edit, Delete, Connection, Document, Link, Clock, VideoPlay, Loading, Upload, View } from '@element-plus/icons-vue'
import { usePluginStore } from '@/stores/usePluginStore'
import type { Plugin } from '@/types/entity'
import PluginDialog from './PluginDialog.vue'
import { formatDateTime } from '@/utils/formatters'
import { importFromOpenApi } from '@/api/plugin'

const pluginStore = usePluginStore()

// 搜索关键词
const searchKeyword = ref('')
// 状态筛选
const filterStatus = ref<string>('')
// 弹窗显示状态
const dialogVisible = ref(false)
// 当前编辑的插件
const currentPlugin = ref<Plugin | null>(null)

// 分页配置
const pagination = reactive({
  page: 1,
  pageSize: 12,
  total: 0
})

// 测试弹窗相关
const testDialogVisible = ref(false)
const testPlugin = ref<Plugin | null>(null)
const testOperations = ref<any[]>([])
const selectedOperation = ref('')
const testParams = ref('')
const testLoading = ref(false)
const testResult = ref<any>(null)

// 查看详情弹窗相关
const viewDialogVisible = ref(false)
const viewPluginData = ref<Plugin | null>(null)

// 文件导入相关
const fileInputRef = ref<HTMLInputElement | null>(null)

// 插件列表数据
const pluginList = ref<Plugin[]>([])
const loading = ref(false)

// 加载插件列表（从后端获取所有数据，前端做过滤和分页）
const loadPlugins = async () => {
  loading.value = true
  try {
    // 使用较大的pageSize获取所有插件，后续可以考虑支持后端搜索和过滤
    const result = await pluginStore.fetchPluginList({ page: 1, pageSize: 1000 })
    pluginList.value = result.list
    // 触发过滤计算以更新总数
    handleSearch()
  } catch (error: any) {
    ElMessage.error(error.message || '加载插件列表失败')
  } finally {
    loading.value = false
  }
}

// 过滤后的插件列表（未分页）
const filteredPluginList = computed(() => {
  let list = pluginList.value

  // 关键词搜索
  if (searchKeyword.value) {
    const keyword = searchKeyword.value.toLowerCase()
    list = list.filter(
      (plugin) =>
        plugin.name?.toLowerCase().includes(keyword) ||
        plugin.identifier?.toLowerCase().includes(keyword) ||
        plugin.description?.toLowerCase().includes(keyword)
    )
  }

  // 状态筛选
  if (filterStatus.value) {
    list = list.filter((plugin) => {
      if (filterStatus.value === 'enabled') {
        return isPluginEnabled(plugin)
      } else {
        return !isPluginEnabled(plugin)
      }
    })
  }

  // 更新总数
  pagination.total = list.length

  return list
})

// 分页后的插件列表
const paginatedPluginList = computed(() => {
  const start = (pagination.page - 1) * pagination.pageSize
  const end = start + pagination.pageSize
  return filteredPluginList.value.slice(start, end)
})

// 辅助函数：判断插件是否启用（兼容 camelCase 和 snake_case）
const isPluginEnabled = (plugin: Plugin | null): boolean => {
  if (!plugin) return false
  return plugin.isEnabled ?? (plugin as any).is_enabled ?? (plugin.status === 'enabled')
}

// 辅助函数：获取创建时间（兼容 camelCase 和 snake_case，以及数字/字符串格式）
const getCreateTime = (plugin: Plugin | null): string | null => {
  if (!plugin) return null
  const time = plugin.createTime ?? (plugin as any).create_time
  // 如果是数字（时间戳），转换为字符串
  if (typeof time === 'number') {
    return new Date(time).toISOString()
  }
  return time ?? null
}

// 搜索处理
const handleSearch = () => {
  pagination.page = 1
}

// 重置筛选
const resetFilters = () => {
  searchKeyword.value = ''
  filterStatus.value = ''
  pagination.page = 1
}

// 分页变化处理
const handlePageChange = () => {
  // 分页逻辑已在 computed 中处理
}

// 创建插件
const handleCreate = () => {
  currentPlugin.value = null
  dialogVisible.value = true
}

// 编辑插件
const handleEdit = (plugin: Plugin) => {
  currentPlugin.value = { ...plugin }
  dialogVisible.value = true
}

// 删除插件
const handleDelete = async (plugin: Plugin) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除插件 "${plugin.name}" 吗？此操作不可恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await pluginStore.removePlugin(plugin.id!)
    ElMessage.success('删除成功')
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除插件失败:', error)
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 弹窗成功回调
const handleDialogSuccess = () => {
  dialogVisible.value = false
  currentPlugin.value = null
  loadPlugins() // 重新加载插件列表，确保数据显示最新
}

// 测试插件
const handleTest = async (plugin: Plugin) => {
  testPlugin.value = plugin
  testResult.value = null
  selectedOperation.value = ''
  testParams.value = ''
  
  try {
    // 获取插件详情（包含 operations）
    const detail = await pluginStore.fetchPluginById(plugin.id!)
    testOperations.value = (detail as any)?.operations || []
    
    // 如果只有一个操作，自动选中
    if (testOperations.value.length === 1) {
      selectedOperation.value = testOperations.value[0].operationId
    }
    
    testDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取插件详情失败')
  }
}

// 执行测试
const executeTest = async () => {
  if (!testPlugin.value || !selectedOperation.value) {
    ElMessage.warning('请选择要测试的操作')
    return
  }

  let params = {}
  if (testParams.value.trim()) {
    try {
      params = JSON.parse(testParams.value)
    } catch (e) {
      ElMessage.error('参数格式错误，请输入有效的 JSON')
      return
    }
  }

  testLoading.value = true
  testResult.value = null

  try {
    const result = await pluginStore.invokeOperation(
      testPlugin.value.id!,
      selectedOperation.value,
      params
    )
    testResult.value = result
    
    if (result.status === 'success') {
      ElMessage.success('调用成功')
    } else {
      ElMessage.warning('调用返回错误')
    }
  } catch (error: any) {
    testResult.value = {
      status: 'error',
      errorMessage: error.message || '调用失败',
      duration: 0
    }
    ElMessage.error(error.message || '调用失败')
  } finally {
    testLoading.value = false
  }
}

// 查看插件详情
const handleView = async (plugin: Plugin) => {
  try {
    // 获取插件详情（包含 operations）
    const detail = await pluginStore.fetchPluginById(plugin.id!)
    viewPluginData.value = detail as Plugin
    viewDialogVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '获取插件详情失败')
  }
}

// 从查看弹窗切换到编辑
const handleEditFromView = () => {
  if (viewPluginData.value) {
    currentPlugin.value = { ...viewPluginData.value }
    viewDialogVisible.value = false
    dialogVisible.value = true
  }
}

// 从查看弹窗切换状态
const handleToggleStatusFromView = async () => {
  if (!viewPluginData.value) return
  
  const newStatus = !isPluginEnabled(viewPluginData.value)
  try {
    await pluginStore.togglePluginStatus(viewPluginData.value.id!, newStatus)
    ElMessage.success(newStatus ? '插件已启用' : '插件已禁用')
    // 刷新详情
    const detail = await pluginStore.fetchPluginById(viewPluginData.value.id!)
    viewPluginData.value = detail as Plugin
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

// 从文件导入
const importFromFile = () => {
  fileInputRef.value?.click()
}

// 处理文件导入
const handleFileImport = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      const content = e.target?.result as string
      const json = JSON.parse(content)
      
      // 验证 OpenAPI 格式
      if (!json.openapi && !json.info && !json.paths) {
        // 可能是简化格式，检查是否有 operations
        if (!json.operations && !json.name) {
          ElMessage.error('文件格式不正确，必须是有效的 OpenAPI 规范或包含 operations 的 JSON')
          return
        }
      }
      
      // 构建导入请求
      const importRequest: Plugin = {
        name: json.info?.title || json.name || file.name.replace('.json', ''),
        description: json.info?.description || json.description || '',
        openapiSpec: json.openapi ? json : undefined,
        authType: json.authType || 'none',
        authConfig: json.authConfig || {}
      }
      
      // 如果是标准 OpenAPI 格式，提取 baseUrl
      if (json.servers?.[0]?.url) {
        importRequest.baseUrl = json.servers[0].url
      }

      // 调用导入 API
      await importFromOpenApi(importRequest)
      ElMessage.success('导入成功')
      loadPlugins()
      
    } catch (error: any) {
      if (error instanceof SyntaxError) {
        ElMessage.error('文件解析失败：不是有效的 JSON 格式')
      } else {
        ElMessage.error(error.message || '导入失败')
      }
    }
  }
  
  reader.readAsText(file)
  
  // 重置文件输入
  target.value = ''
}

// 获取 HTTP 方法的标签类型
const getMethodTagType = (method: string): string => {
  const types: Record<string, string> = {
    'GET': 'success',
    'POST': 'primary',
    'PUT': 'warning',
    'DELETE': 'danger',
    'PATCH': 'info'
  }
  return types[method?.toUpperCase()] || 'info'
}

// 初始化加载数据
onMounted(() => {
  loadPlugins()
})
</script>

<style scoped>
.plugin-list-container {
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: #f8fafc;
}

/* 按钮组样式 */
.button-group-col {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.button-group {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
}

.button-group .el-button {
  white-space: nowrap;
}

/* 使用公共样式类 */

/* 卡片网格布局 */
.plugins-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

/* 插件卡片 */
.plugin-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid var(--border-light);
  background: #ffffff;
}

.plugin-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* 卡片头部 - 使用公共样式 */
.plugin-card .card-header {
  padding: 20px;
  background: var(--gradient-bg-card-header);
}

.header-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.plugin-icon {
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

.plugin-info {
  flex: 1;
  min-width: 0;
}

.plugin-name {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plugin-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 卡片主体 - 使用公共样式 */
.plugin-card .card-body {
  padding: 20px;
  background: #ffffff;
}

.plugin-description {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: var(--text-regular);
  line-height: 1.6;
  min-height: 44px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.plugin-stats {
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
  color: var(--text-secondary);
}

.stat-item .el-icon {
  font-size: 16px;
}

/* 卡片底部 - 使用 flex 一行布局（学习 CodeHubot 样式） */
.plugin-card .card-footer {
  padding: 12px 20px;
  background: #f5f7fa;
  display: flex;
  gap: 8px;
  border-top: 1px solid var(--border-light, #e4e7ed);
}

.plugin-card .card-footer :deep(.el-button) {
  flex: 1;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .plugins-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .plugin-list-container {
    padding: 16px;
  }
  
  .plugins-grid {
    grid-template-columns: 1fr;
  }
  
  .button-group-col {
    margin-top: 12px;
    justify-content: flex-start;
  }
  
  .button-group {
    flex-wrap: wrap;
    width: 100%;
  }
  
  .button-group .el-button {
    flex: 1;
    min-width: calc(33.333% - 6px);
  }
}

/* 测试弹窗样式 */
.test-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #409eff;
}

.test-result {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
}

.result-body {
  margin-top: 16px;
}

.result-body h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}

.result-body pre {
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  margin: 0;
  font-size: 13px;
  overflow-x: auto;
  max-height: 200px;
}

/* 查看详情弹窗样式 */
.openapi-spec {
  background: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 400px;
  overflow: auto;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  line-height: 1.5;
  margin: 0;
}
</style>
