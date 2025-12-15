<template>
  <div class="kb-detail">
    <el-page-header @back="goBack" title="返回">
      <template #content>
        <span class="kb-name">{{ knowledgeBase.name || '知识库详情' }}</span>
      </template>
    </el-page-header>

    <el-row :gutter="20" style="margin-top: 20px" class="kb-detail-row">
      <!-- 左侧：知识库信息 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="8" :xl="8" class="left-col">
        <!-- 知识库信息 -->
        <el-card shadow="hover" class="info-card">
          <template #header>
            <div class="card-header-title">
              <el-icon class="header-icon"><Document /></el-icon>
              <span class="header-text">知识库信息</span>
            </div>
          </template>

          <el-descriptions :column="1" border size="default" class="kb-descriptions">
            <el-descriptions-item label="名称">
              <span class="kb-name-value">{{ knowledgeBase.name }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="描述">
              <span class="text-regular">{{ knowledgeBase.description || '暂无描述' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="作用域">
              <el-tag :type="getScopeTagType(knowledgeBase.scopeType)" size="default">
                {{ getScopeLabel(knowledgeBase.scopeType) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="访问级别">
              <el-tag :type="getAccessLevelTagType(knowledgeBase.accessLevel)" size="default">
                {{ getAccessLevelLabel(knowledgeBase.accessLevel) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="文档数">
              <el-tag type="primary" size="default">{{ knowledgeBase.documentCount || 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="文本块数">
              <el-tag type="success" size="default">{{ knowledgeBase.chunkCount || 0 }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="总大小">
              <span class="text-regular">{{ formatSize(knowledgeBase.totalSize) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">
              <span class="text-regular">{{ formatDateTime(knowledgeBase.createTime) }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 右侧：文档列表和检索测试 -->
      <el-col :xs="24" :sm="24" :md="16" :lg="16" :xl="16" class="right-col">
        <div class="right-cards-container">
        <el-card class="documents-card">
          <template #header>
            <div class="card-header">
              <div class="card-header-title">
                <el-icon class="header-icon"><Document /></el-icon>
                <span class="header-text">文档列表</span>
              </div>
              <el-button type="primary" @click="showUploadDialog = true" class="upload-button">
                <el-icon><Upload /></el-icon>
                上传文档
              </el-button>
            </div>
          </template>

          <!-- 文档列表 -->
          <div class="table-container">
          <el-table v-loading="loading" :data="documents" stripe>
            <el-table-column prop="name" label="标题" min-width="200" />
            <el-table-column prop="fileType" label="类型" width="80">
              <template #default="{ row }">
                <el-tag size="small">{{ row.fileType?.toUpperCase() || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fileSize" label="大小" width="100">
              <template #default="{ row }">
                {{ formatSize(row.fileSize) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="向量化状态" width="120">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)" size="small">
                  {{ getStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="chunkCount" label="文本块" width="80" />
            <el-table-column prop="createdAt" label="上传时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="viewDocument(row)">
                  查看
                </el-button>
                <el-button size="small" type="danger" link @click="confirmDeleteDoc(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          </div>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="pagination.page"
              v-model:page-size="pagination.pageSize"
              :total="pagination.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              @size-change="loadDocuments"
              @current-change="loadDocuments"
            />
          </div>
        </el-card>

        <!-- 向量检索测试 -->
        <el-card shadow="hover" class="search-card" style="margin-top: 20px">
          <template #header>
            <div class="search-card-header">
              <div class="card-header-title">
                <el-icon class="header-icon"><Search /></el-icon>
                <span class="header-text">向量检索测试</span>
              </div>
              <el-tag type="info" size="small" effect="plain" class="test-tag">测试知识库效果</el-tag>
            </div>
          </template>

          <div class="search-content">
            <el-input
              v-model="searchQuery"
              type="textarea"
              :rows="3"
              placeholder="输入查询内容，测试向量检索效果..."
              :disabled="searching"
            />
            
            <div class="search-controls">
              <div class="search-actions">
                <el-select v-model="searchTopK" size="default" class="topk-select">
                  <el-option label="返回 3 条" :value="3" />
                  <el-option label="返回 5 条" :value="5" />
                  <el-option label="返回 10 条" :value="10" />
                  <el-option label="返回 20 条" :value="20" />
                </el-select>
                
                <el-button 
                  type="primary" 
                  :loading="searching" 
                  @click="handleSearch"
                  :disabled="!searchQuery.trim()"
                  class="search-button"
                >
                  <el-icon v-if="!searching" class="button-icon"><Search /></el-icon>
                  {{ searching ? '检索中...' : '开始检索' }}
                </el-button>
              </div>
            </div>

            <!-- 检索结果 -->
            <div v-if="searchResults.length > 0" class="search-results-list">
              <el-divider>
                <el-tag type="success" class="results-tag">
                  找到 {{ searchResults.length }} 个相关结果
                </el-tag>
              </el-divider>

              <div 
                v-for="(result, index) in searchResults" 
                :key="index"
                class="result-item"
              >
                <el-card shadow="hover" class="result-card">
                  <template #header>
                    <div class="result-header">
                      <div class="result-title">
                        <el-tag size="small" type="primary" class="result-index">
                          #{{ index + 1 }}
                        </el-tag>
                        <span class="result-doc-title">
                          {{ result.documentName || '未命名文档' }}
                        </span>
                      </div>
                    </div>
                  </template>

                  <div class="result-content">
                    <el-text 
                      line-clamp="4" 
                      class="result-text"
                    >
                      {{ result.content || result.text || '' }}
                    </el-text>
                  </div>
                </el-card>
              </div>
            </div>

            <!-- 空状态提示 -->
            <el-empty 
              v-else-if="hasSearched && searchResults.length === 0"
              description="未找到相关内容"
              :image-size="80"
              class="empty-state"
            >
              <template #extra>
                <el-text type="info" size="small">
                  尝试使用不同的关键词或上传更多文档
                </el-text>
              </template>
            </el-empty>

            <!-- 初始提示 -->
            <div v-else class="search-placeholder">
              <el-icon size="40" color="#C0C4CC" class="placeholder-icon"><Search /></el-icon>
              <div class="placeholder-text">
                输入查询内容并点击"开始检索"按钮<br>
                测试知识库的向量检索效果
              </div>
            </div>
          </div>
        </el-card>
        </div>
      </el-col>
    </el-row>

    <!-- 文档详情对话框 -->
    <el-dialog
      v-model="showDocumentDialog"
      :title="`文档详情 - ${currentDocument?.name || ''}`"
      width="80%"
    >
      <div v-if="currentDocument">
        <el-descriptions :column="3" border style="margin-bottom: 20px">
          <el-descriptions-item label="文档名称">
            {{ currentDocument.name }}
          </el-descriptions-item>
          <el-descriptions-item label="文件名">
            {{ currentDocument.filename }}
          </el-descriptions-item>
          <el-descriptions-item label="文件类型">
            <el-tag size="small">{{ currentDocument.fileType?.toUpperCase() }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="文件大小">
            {{ formatSize(currentDocument.fileSize) }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentDocument.status)" size="small">
              {{ getStatusLabel(currentDocument.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="文本块数">
            {{ currentDocument.chunkCount || 0 }}
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">
            {{ formatDateTime(currentDocument.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="处理时间">
            {{ currentDocument.processedAt ? formatDateTime(currentDocument.processedAt) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="错误信息" v-if="currentDocument.errorMessage">
            <el-text type="danger">{{ currentDocument.errorMessage }}</el-text>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <template #footer>
        <el-button @click="showDocumentDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="600px">
      <el-upload
        ref="uploadRef"
        class="upload-demo"
        drag
        :auto-upload="false"
        :limit="1"
        :accept="'.txt,.md,.markdown'"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :file-list="fileList"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只支持 TXT 和 Markdown 格式（.txt, .md, .markdown），文件大小不超过 10MB
          </div>
        </template>
      </el-upload>

      <div v-if="uploadProgress > 0 && uploadProgress < 100" class="upload-progress">
        <el-progress :percentage="uploadProgress" :status="uploadStatus" />
      </div>

      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="handleUpload"
          :loading="uploading"
          :disabled="!selectedFile"
        >
          上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Upload, UploadFilled, Search } from '@element-plus/icons-vue'
import {
  getKnowledgeBase,
  getDocumentList,
  getDocument,
  uploadDocument,
  deleteDocument,
  type KnowledgeBase,
  type Document as DocumentVO,
  queryKnowledgeBase
} from '@/api/knowledgeBase'
import { formatDateTime, formatSize, getScopeTagType, getScopeLabel, getAccessLevelTagType, getAccessLevelLabel, getDocumentStatusType, getDocumentStatusLabel } from '@/utils/formatters'

const route = useRoute()
const router = useRouter()

// 数据
const loading = ref(false)
const uploading = ref(false)
const knowledgeBase = ref<KnowledgeBase>({} as KnowledgeBase)
const documents = ref<DocumentVO[]>([])
const showUploadDialog = ref(false)
const showDocumentDialog = ref(false)
const currentDocument = ref<DocumentVO | null>(null)
const selectedFile = ref<File | null>(null)
const fileList = ref<any[]>([])
const uploadRef = ref()
const uploadProgress = ref(0)
const uploadStatus = ref<'success' | 'exception' | 'warning' | ''>('')

// 轮询定时器
let pollingTimer: number | null = null

// 向量检索相关
const searchQuery = ref('')
const searchTopK = ref(5)
const searchSimilarityThreshold = ref(0.3)
const searchResults = ref<any[]>([])
const searching = ref(false)
const hasSearched = ref(false)

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0
})

// 方法
const loadKnowledgeBase = async () => {
  try {
    const uuid = route.params.uuid as string
    knowledgeBase.value = await getKnowledgeBase(uuid)
    
    // 从知识库配置中加载相似度阈值
    if (knowledgeBase.value.retrievalConfig) {
      const config = knowledgeBase.value.retrievalConfig as any
      if (config.similarity_threshold !== undefined) {
        searchSimilarityThreshold.value = parseFloat(config.similarity_threshold)
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载知识库信息失败')
  }
}

const loadDocuments = async () => {
  loading.value = true
  try {
    const uuid = route.params.uuid as string
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize
    }

    const result = await getDocumentList(uuid, params)
    documents.value = result.list || []
    pagination.total = result.total || 0

    // 检查是否有处理中的文档，如果有则启动轮询
    const hasProcessing = documents.value.some(
      (doc) => doc.status === 'uploading' || doc.status === 'processing'
    )
    if (hasProcessing && !pollingTimer) {
      startPolling()
    } else if (!hasProcessing && pollingTimer) {
      stopPolling()
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载文档列表失败')
  } finally {
    loading.value = false
  }
}

// 轮询文档状态
const startPolling = () => {
  if (pollingTimer) return
  pollingTimer = window.setInterval(() => {
    loadDocuments()
  }, 3000) // 每3秒轮询一次
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

const handleFileChange = (file: any) => {
  // 验证文件格式
  const allowedTypes = ['.txt', '.md', '.markdown']
  const fileName = file.name.toLowerCase()
  const isValidType = allowedTypes.some((type) => fileName.endsWith(type))

  if (!isValidType) {
    ElMessage.error('只支持 TXT 和 Markdown 格式（.txt, .md, .markdown）')
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    fileList.value = []
    return false
  }

  // 验证文件大小（10MB = 10 * 1024 * 1024 bytes）
  const MAX_SIZE = 10 * 1024 * 1024
  if (file.size > MAX_SIZE) {
    ElMessage.error(
      `文件大小超过限制，最大支持 10MB（当前文件：${(file.size / 1024 / 1024).toFixed(2)}MB）`
    )
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    fileList.value = []
    return false
  }

  selectedFile.value = file.raw
  fileList.value = [file]
}

const handleFileRemove = () => {
  selectedFile.value = null
  fileList.value = []
}

const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  uploading.value = true
  uploadProgress.value = 0
  uploadStatus.value = ''

  try {
    const uuid = route.params.uuid as string

    await uploadDocument(
      uuid,
      selectedFile.value,
      (progressEvent) => {
        if (progressEvent.total) {
          uploadProgress.value = Math.round(
            (progressEvent.loaded * 100) / progressEvent.total
          )
        }
      }
    )

    uploadProgress.value = 100
    uploadStatus.value = 'success'
    ElMessage.success('上传成功，文档正在处理中')

    // 关闭对话框并重置
    showUploadDialog.value = false
    selectedFile.value = null
    fileList.value = []
    uploadRef.value?.clearFiles()
    uploadProgress.value = 0
    uploadStatus.value = ''

    // 刷新列表
    await loadDocuments()
    await loadKnowledgeBase()

    // 启动轮询以监控处理状态
    startPolling()
  } catch (error: any) {
    uploadStatus.value = 'exception'
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const viewDocument = async (doc: DocumentVO) => {
  try {
    currentDocument.value = await getDocument(doc.uuid)
    showDocumentDialog.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '加载文档详情失败')
  }
}

const confirmDeleteDoc = (doc: DocumentVO) => {
  ElMessageBox.confirm(
    `确定要删除文档"${doc.name}"吗？删除后将级联删除文档块和向量数据。`,
    '确认删除',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        await deleteDocument(doc.uuid)
        ElMessage.success('删除成功')
        await loadDocuments()
        await loadKnowledgeBase()
      } catch (error: any) {
        ElMessage.error(error.message || '删除失败')
      }
    })
    .catch(() => {
      // 用户取消
    })
}

// 使用公共工具函数
const getStatusType = getDocumentStatusType
const getStatusLabel = getDocumentStatusLabel

const goBack = () => {
  router.back()
}

// 向量检索
const handleSearch = async () => {
  if (!searchQuery.value.trim()) {
    ElMessage.warning('请输入查询内容')
    return
  }

  if (knowledgeBase.value.chunkCount === 0) {
    ElMessage.warning('知识库中暂无已向量化的内容，请先上传并向量化文档')
    return
  }

  searching.value = true
  searchResults.value = []
  hasSearched.value = true

  try {
    // TODO: 实现RAG检索API调用
    const res = await queryKnowledgeBase(route.params.uuid as string,
      knowledgeBase.value.id,
      searchQuery.value,
      searchTopK.value,
      searchSimilarityThreshold.value
    )
    // ElMessage.info(`相似度阈值:${searchSimilarityThreshold.value}`)
    // 暂时提示功能未实现
    // ElMessage.info('RAG检索功能正在开发中，敬请期待')
    searchResults.value = res.results
  } catch (error: any) {
    console.error('检索失败:', error)
    ElMessage.error(error.message || '检索失败，请稍后重试')
    searchResults.value = []
  } finally {
    searching.value = false
  }
}

// 保存相似度阈值（暂时不实现，等后端支持）
// const saveSimilarityThreshold = async () => {
//   // TODO: 保存到知识库配置
// }

onMounted(() => {
  loadKnowledgeBase()
  loadDocuments()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.kb-detail {
  padding: 20px;
  min-height: calc(100vh - 60px);
  background: var(--el-bg-color-page);
}

.kb-name {
  font-size: 20px;
  font-weight: bold;
  color: var(--el-text-color-primary);
}

/* 行布局 */
.kb-detail-row {
  display: flex;
  align-items: stretch;
}

.left-col {
  display: flex;
  flex-direction: column;
}

.right-col {
  display: flex;
  flex-direction: column;
}

.right-cards-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  gap: 20px;
}

/* 卡片样式 */
.info-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.info-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.documents-card {
  flex: 0 0 40%;
  display: flex;
  flex-direction: column;
  margin-bottom: 0;
  min-height: 0;
}

.documents-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.table-container {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

.search-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-top: 0;
  margin-bottom: 0;
  min-height: 0;
}

.search-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.search-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  min-height: 0;
}

.search-results {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

/* 卡片标题 */
/* 使用公共样式类 */
.search-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.test-tag {
  margin-left: auto;
}

.upload-button {
  display: flex;
  align-items: center;
  gap: 4px;
}

.kb-descriptions {
  margin-top: 10px;
}

.kb-name-value {
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.text-regular {
  color: var(--el-text-color-regular);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.upload-progress {
  margin-top: 20px;
}

/* 检索控制区域 */
.search-controls {
  margin-top: 15px;
}

.search-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.topk-select {
  width: 130px;
}

.search-button {
  flex: 1;
}

.button-icon {
  margin-right: 4px;
}

/* 检索结果列表 */
.search-results-list {
  margin-top: 20px;
}

.results-tag {
  font-size: 13px;
}

.result-item {
  margin-bottom: 15px;
}

.result-card {
  transition: transform 0.2s, box-shadow 0.2s;
}

.result-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.result-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.result-index {
  flex-shrink: 0;
}

.result-doc-title {
  font-size: 14px;
  color: var(--el-text-color-primary);
  font-weight: 500;
}

.result-content {
  padding: 0;
}

.result-text {
  font-size: 13px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

/* 空状态和占位符 */
.empty-state {
  margin-top: 20px;
}

.search-placeholder {
  margin-top: 20px;
  text-align: center;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  padding: 20px;
}

.placeholder-icon {
  display: block;
  margin: 0 auto 8px;
}

.placeholder-text {
  margin-top: 8px;
  line-height: 1.6;
}

</style>

