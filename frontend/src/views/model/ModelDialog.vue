<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑模型' : '新增模型'"
    width="600px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      v-loading="loading"
    >
      <el-form-item label="模型名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入模型名称"></el-input>
      </el-form-item>
      <el-form-item label="显示名称" prop="displayName">
        <el-input v-model="formData.displayName" placeholder="请输入模型显示名称（可选）"></el-input>
      </el-form-item>
      <el-form-item label="提供商" prop="provider">
        <el-input v-model="formData.provider" placeholder="请输入模型提供商"></el-input>
      </el-form-item>
      <el-form-item label="模型类型" prop="modelType">
        <el-input v-model="formData.modelType" placeholder="请输入模型类型（如：Chat）"></el-input>
      </el-form-item>
      <el-form-item label="API Base" prop="apiBase">
        <el-input v-model="formData.apiBase" placeholder="请输入模型API基础地址"></el-input>
      </el-form-item>
      <el-form-item label="API Key" prop="apiKey">
        <el-input 
          v-model="formData.apiKey" 
          :placeholder="isEdit ? '留空则保持原有API Key不变，或输入新API Key' : '请输入模型API密钥'" 
          show-password
          :disabled="!isAdmin"
        ></el-input>
        <div v-if="!isAdmin" class="form-item-hint">
          <el-text type="info" size="small">仅管理员可修改 API Key</el-text>
        </div>
        <div v-else-if="isEdit" class="form-item-hint">
          <el-text type="info" size="small">编辑时留空则保持原有API Key不变</el-text>
        </div>
      </el-form-item>
      <el-form-item label="API Version" prop="apiVersion">
        <el-input v-model="formData.apiVersion" placeholder="请输入模型API版本（可选）"></el-input>
      </el-form-item>
      <el-form-item label="最大Token数" prop="maxTokens">
        <el-input-number v-model="formData.maxTokens" :min="1" :max="999999" style="width: 100%"></el-input-number>
      </el-form-item>
      <el-form-item label="温度" prop="temperature">
        <el-slider v-model="formData.temperature" :min="0" :max="2" :step="0.01" show-input></el-slider>
      </el-form-item>
      <el-form-item label="Top P" prop="topP">
        <el-slider v-model="formData.topP" :min="0" :max="1" :step="0.01" show-input></el-slider>
      </el-form-item>
      <el-form-item label="启用深度思考" prop="enableDeepThinking">
        <el-switch v-model="formData.enableDeepThinking"></el-switch>
      </el-form-item>
      <el-form-item label="频率惩罚" prop="frequencyPenalty">
        <el-slider v-model="formData.frequencyPenalty" :min="-2" :max="2" :step="0.01" show-input></el-slider>
      </el-form-item>
      <el-form-item label="存在惩罚" prop="presencePenalty">
        <el-slider v-model="formData.presencePenalty" :min="-2" :max="2" :step="0.01" show-input></el-slider>
      </el-form-item>
      <el-form-item label="配置 (JSON)" prop="config">
        <el-input
          v-model="formData.config"
          type="textarea"
          :rows="4"
          placeholder="请输入JSON格式的额外配置（可选）"
        ></el-input>
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="请输入模型描述（可选）"></el-input>
      </el-form-item>
      <el-form-item label="是否激活" prop="isActive">
        <el-switch v-model="formData.isActive"></el-switch>
      </el-form-item>
      <el-form-item label="是否默认" prop="isDefault">
        <el-switch v-model="formData.isDefault"></el-switch>
      </el-form-item>
      <el-form-item label="排序" prop="sortOrder">
        <el-input-number v-model="formData.sortOrder" :min="0" style="width: 100%"></el-input-number>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed, reactive } from 'vue'
import { ElMessage, ElForm } from 'element-plus'
import type { FormRules } from 'element-plus'
import { createLlmModel, updateLlmModel } from '@/api/llm'
import type { LlmModel } from '@/types/entity'
import { useUserStore } from '@/stores/useUserStore'

const props = defineProps({
  modelValue: { // 控制弹窗显示隐藏
    type: Boolean,
    default: false
  },
  model: { // 传入的模型数据，用于编辑
    type: Object as () => LlmModel | null,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

const formRef = ref<typeof ElForm | null>(null)
const loading = ref(false)

// 用户权限
const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

// 弹窗可见性
const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 是否为编辑模式
const isEdit = computed(() => !!props.model?.id)

// 表单数据
const initialFormData: LlmModel = {
  name: '',
  displayName: '',
  provider: '',
  modelType: '',
  apiBase: '',
  apiKey: '',
  apiVersion: '',
  maxTokens: 4096,
  temperature: 0.7,
  topP: 1,
  enableDeepThinking: false,
  frequencyPenalty: 0,
  presencePenalty: 0,
  config: '',
  description: '',
  isActive: true,
  isDefault: false,
  isSystem: false, // 不允许前端设置，默认为false
  sortOrder: 0,
}

const formData = reactive<LlmModel>({ ...initialFormData })

// 表单校验规则
const formRules = reactive<FormRules<LlmModel>>({
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请输入模型提供商', trigger: 'blur' }],
  modelType: [{ required: true, message: '请输入模型类型', trigger: 'blur' }],
  apiBase: [{ required: true, message: '请输入API基础地址', trigger: 'blur' }],
  apiKey: [
    { 
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        // 只有管理员才能修改，且仅在创建时必填，编辑时可以为空（保持原值）
        if (isAdmin.value) {
          if (!isEdit.value && !value) {
            callback(new Error('创建模型时请输入API Key'))
          } else {
            callback()
          }
        } else {
          // 非管理员不能修改，但不会走到这里（因为只有管理员才能打开对话框）
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  maxTokens: [{ required: true, message: '请输入最大Token数', trigger: 'blur' }],
  temperature: [{ required: true, message: '请输入温度', trigger: 'blur' }],
  topP: [{ required: true, message: '请输入Top P', trigger: 'blur' }],
  config: [
    {
      validator: (_rule: any, value: string, callback: (error?: Error) => void) => {
        if (value) {
          try {
            JSON.parse(value)
            callback()
          } catch (e) {
            callback(new Error('请输入有效的JSON格式'))
          }
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
})

// 监听model prop变化，用于编辑模式初始化表单
watch(
  () => props.model,
  (newVal) => {
    if (newVal) {
      Object.assign(formData, newVal)
      // 特殊处理 config 字段，确保是字符串
      if (typeof newVal.config !== 'string' && newVal.config !== null && newVal.config !== undefined) {
        formData.config = JSON.stringify(newVal.config, null, 2)
      }
      // 特殊处理 API Key：如果后端返回的是隐藏值 "************"，则清空让管理员重新输入
      if (newVal.apiKey === '************') {
        formData.apiKey = ''
      }
    } else {
      Object.assign(formData, initialFormData)
    }
  },
  { immediate: true, deep: true }
)

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        const dataToSubmit = { ...formData } as any
        
        // 编辑模式下，如果 API Key 为空，则不提交该字段（保持原有值）
        // 这是因为后端返回的是 "************"，我们清空了，如果用户没填新的，就不应该更新
        if (isEdit.value && props.model?.id) {
          if (!dataToSubmit.apiKey || dataToSubmit.apiKey.trim() === '') {
            // 编辑时如果 API Key 为空，删除该字段，让后端保持原值
            delete dataToSubmit.apiKey
          }
        }
        
        // 处理 config 字段：空字符串转为 null，非空则解析为对象
        if (dataToSubmit.config && typeof dataToSubmit.config === 'string' && dataToSubmit.config.trim() !== '') {
          try {
            dataToSubmit.config = JSON.parse(dataToSubmit.config)
          } catch (e) {
            // 如果解析失败，说明不是有效JSON，但因为已在规则中校验，这里通常不会发生
            console.warn('Config is not valid JSON, submitting as string.', e)
          }
        } else {
          // 空字符串或空白字符串转为 null，避免 MySQL JSON 字段报错
          dataToSubmit.config = null
        }

        if (isEdit.value && props.model?.id) {
          await updateLlmModel(props.model.id, dataToSubmit)
          ElMessage.success('模型更新成功')
        } else {
          await createLlmModel(dataToSubmit)
          ElMessage.success('模型创建成功')
        }
        emit('success')
        visible.value = false
      } catch (error: any) {
        ElMessage.error('操作失败: ' + (error.message || '未知错误'))
        console.error('模型操作失败:', error)
      } finally {
        loading.value = false
      }
    }
  })
}

// 弹窗关闭时重置表单
const handleClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, initialFormData) // 确保完全重置数据
  emit('update:modelValue', false)
}

</script>

<style scoped>
.form-item-hint {
  margin-top: 4px;
}
</style>

