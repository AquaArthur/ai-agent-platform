<template>
  <div class="knowledge-base-management">
    <div class="page-header">
      <h2>知识库管理</h2>
    </div>

    <!-- 搜索和筛选 -->
    <div class="filter-section">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-input
            v-model="searchParams.search"
            placeholder="搜索知识库名称、描述"
            clearable
            :prefix-icon="Search"
            @input="handleSearch"
          />
        </el-col>
        <el-col :span="6">
          <el-select
            v-model="searchParams.scopeType"
            placeholder="筛选作用域"
            clearable
            @change="handleSearch"
            style="width: 100%"
          >
            <el-option label="全部" value="" />
            <el-option label="系统级" value="system" />
            <el-option label="学校级" value="school" />
            <el-option label="课程级" value="course" />
            <el-option label="智能体级" value="agent" />
            <el-option label="个人级" value="personal" />
          </el-select>
        </el-col>
        <el-col :span="10" style="text-align: right;">
          <el-button @click="resetFilters">重置筛选</el-button>
          <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
            新增知识库
          </el-button>
        </el-col>
      </el-row>
    </div>

    <!-- 知识库列表 - 卡片形式 -->
    <div v-loading="loading" class="kb-grid">
      <el-empty v-if="!loading && knowledgeBaseList.length === 0" description="暂无知识库数据">
        <el-button type="primary" @click="showCreateDialog = true">创建第一个知识库</el-button>
      </el-empty>
      
      <el-card
        v-for="kb in knowledgeBaseList"
        :key="kb.uuid"
        class="kb-card"
        shadow="hover"
        :body-style="{ padding: '0' }"
      >
        <div class="card-header">
          <div class="header-top">
            <div class="kb-icon">
              <el-icon size="24"><component :is="getScopeIcon(kb.scopeType)" /></el-icon>
            </div>
            <div class="kb-info">
              <h3 class="kb-name">{{ kb.name }}</h3>
              <div class="kb-badges">
                <el-tag :type="getScopeTagType(kb.scopeType)" size="small">
                  {{ getScopeLabel(kb.scopeType) }}
                </el-tag>
                <el-tag :type="getAccessLevelTagType(kb.accessLevel)" size="small">
                  {{ getAccessLevelLabel(kb.accessLevel) }}
                </el-tag>
              </div>
            </div>
          </div>
        </div>
        
        <div class="card-body">
          <p class="kb-description">{{ kb.description || '暂无描述' }}</p>
          
          <div class="kb-stats">
            <div class="stat-item">
              <el-icon><Document /></el-icon>
              <span>{{ kb.documentCount || 0 }} 个文档</span>
            </div>
            <div class="stat-item">
              <el-icon><Grid /></el-icon>
              <span>{{ kb.chunkCount || 0 }} 个文本块</span>
            </div>
            <div class="stat-item">
              <el-icon><Clock /></el-icon>
              <span>{{ formatDate(kb.createTime) }}</span>
            </div>
          </div>
        </div>
        
        <div class="card-footer">
          <el-button type="primary" size="small" @click="handleView(kb)">
            <el-icon><View /></el-icon>
            查看
          </el-button>
          <el-button type="primary" size="small" @click="handleEdit(kb)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button
            type="danger"
            size="small"
            @click="handleDelete(kb)"
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
      @size-change="loadKnowledgeBases"
      @current-change="loadKnowledgeBases"
      style="margin-top: 20px; justify-content: center;"
    />

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="editingKb ? '编辑知识库' : '创建知识库'"
      width="600px"
    >
      <el-form :model="formData" label-width="100px" :rules="formRules" ref="formRef">
        <el-form-item label="名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入知识库名称" maxlength="100" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入知识库描述"
          />
        </el-form-item>

        <el-form-item label="作用域类型" prop="scopeType">
          <el-select v-model="formData.scopeType" placeholder="请选择" style="width: 100%">
            <el-option label="系统级" value="system" />
            <el-option label="学校级" value="school" />
            <el-option label="课程级" value="course" />
            <el-option label="智能体级" value="agent" />
            <el-option label="个人级" value="personal" />
          </el-select>
        </el-form-item>

        <el-form-item label="访问级别">
          <el-select v-model="formData.accessLevel" placeholder="请选择" style="width: 100%">
            <el-option label="公开" value="public" />
            <el-option label="受保护" value="protected" />
            <el-option label="私有" value="private" />
          </el-select>
        </el-form-item>

        <el-form-item label="分块大小">
          <el-input-number
            v-model="formData.chunkSize"
            :min="100"
            :max="2000"
            :step="50"
            style="width: 100%"
          />
          <div class="form-tip">字符数，建议 300-800</div>
        </el-form-item>

        <el-form-item label="分块重叠">
          <el-input-number
            v-model="formData.chunkOverlap"
            :min="0"
            :max="200"
            :step="10"
            style="width: 100%"
          />
          <div class="form-tip">字符数，建议 50-100</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Document, School, Grid, User, Clock, View, Edit, Delete } from '@element-plus/icons-vue'
import {
  getKnowledgeBaseList,
  createKnowledgeBase,
  updateKnowledgeBase,
  deleteKnowledgeBase,
  type KnowledgeBase,
  type KnowledgeBaseCreateDTO,
  type KnowledgeBasePatchDTO
} from '@/api/knowledgeBase'
import { formatDate, getScopeTagType, getScopeLabel, getAccessLevelTagType, getAccessLevelLabel } from '@/utils/formatters'

const router = useRouter()

// 数据
const loading = ref(false)
const submitting = ref(false)
const knowledgeBaseList = ref<KnowledgeBase[]>([])
const showCreateDialog = ref(false)
const editingKb = ref<KnowledgeBase | null>(null)
const formRef = ref()

const searchParams = reactive({
  search: '',
  scopeType: '',
  accessLevel: ''
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  total: 0
})

const formData = reactive<KnowledgeBaseCreateDTO>({
  name: '',
  description: '',
  scopeType: 'personal',
  accessLevel: 'private',
  chunkSize: 800,
  chunkOverlap: 50
})

const formRules = {
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
  scopeType: [{ required: true, message: '请选择作用域类型', trigger: 'change' }]
}

// 方法
const loadKnowledgeBases = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...(searchParams.search && { search: searchParams.search }),
      ...(searchParams.scopeType && { scopeType: searchParams.scopeType }),
      ...(searchParams.accessLevel && { accessLevel: searchParams.accessLevel })
    }

    const result = await getKnowledgeBaseList(params)
    knowledgeBaseList.value = result.list || []
    pagination.total = result.total || 0
  } catch (error: any) {
    ElMessage.error(error.message || '加载知识库列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  loadKnowledgeBases()
}

const resetFilters = () => {
  searchParams.search = ''
  searchParams.scopeType = ''
  searchParams.accessLevel = ''
  handleSearch()
}

const handleView = (kb: KnowledgeBase) => {
  router.push(`/main/knowledge-bases/${kb.uuid}`)
}

const handleEdit = (kb: KnowledgeBase) => {
  editingKb.value = kb
  Object.assign(formData, {
    name: kb.name,
    description: kb.description || '',
    scopeType: kb.scopeType,
    accessLevel: kb.accessLevel,
    chunkSize: kb.chunkSize || 800,
    chunkOverlap: kb.chunkOverlap || 50
  })
  showCreateDialog.value = true
}

const handleDelete = (kb: KnowledgeBase) => {
  ElMessageBox.confirm(
    `确定要删除知识库"${kb.name}"吗？删除后将级联删除关联的文档和向量数据。`,
    '确认删除',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
    .then(async () => {
      try {
        await deleteKnowledgeBase(kb.uuid)
        ElMessage.success('删除成功')
        loadKnowledgeBases()
      } catch (error: any) {
        ElMessage.error(error.message || '删除失败')
      }
    })
    .catch(() => {
      // 用户取消
    })
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (!valid) return

    submitting.value = true
    try {
      if (editingKb.value) {
        const patchData: KnowledgeBasePatchDTO = {
          name: formData.name,
          description: formData.description,
          accessLevel: formData.accessLevel,
          chunkSize: formData.chunkSize,
          chunkOverlap: formData.chunkOverlap
        }
        await updateKnowledgeBase(editingKb.value.uuid, patchData)
        ElMessage.success('更新成功')
      } else {
        const createData: KnowledgeBaseCreateDTO = { ...formData }
        await createKnowledgeBase(createData)
        ElMessage.success('创建成功')
      }

      showCreateDialog.value = false
      editingKb.value = null
      resetForm()
      loadKnowledgeBases()
    } catch (error: any) {
      ElMessage.error(error.message || (editingKb.value ? '更新失败' : '创建失败'))
    } finally {
      submitting.value = false
    }
  })
}

const resetForm = () => {
  Object.assign(formData, {
    name: '',
    description: '',
    scopeType: 'personal',
    accessLevel: 'private',
    chunkSize: 800,
    chunkOverlap: 50
  })
  formRef.value?.clearValidate()
}

// 使用公共工具函数

const getScopeIcon = (scopeType: string) => {
  const iconMap: Record<string, any> = {
    system: Grid,
    school: School,
    course: Document,
    agent: User,
    personal: User
  }
  return iconMap[scopeType] || Document
}

onMounted(() => {
  loadKnowledgeBases()
})
</script>

<style scoped>
.knowledge-base-management {
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: #f8fafc;
}

/* 使用公共样式类 */

/* 卡片网格布局 */
.kb-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

/* 知识库卡片 */
.kb-card {
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  border: 1px solid var(--border-light);
  background: #ffffff;
}

.kb-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* 卡片头部 - 使用公共样式 */
.kb-card .card-header {
  padding: 20px;
  background: var(--gradient-bg-card-header);
}

.header-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.kb-icon {
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

.kb-info {
  flex: 1;
  min-width: 0;
}

.kb-name {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 卡片主体 - 使用公共样式 */
.kb-card .card-body {
  padding: 20px;
  background: #ffffff;
}

.kb-description {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: var(--text-regular);
  line-height: 1.6;
  min-height: 44px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.kb-stats {
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

/* 卡片底部 - 使用公共样式 */
.kb-card .card-footer {
  padding: 12px 20px;
  background: #f5f7fa;
  display: flex;
  gap: 8px;
  border-top: 1px solid var(--border-light);
}

/* 知识库卡片网格 */
.kb-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}
</style>

