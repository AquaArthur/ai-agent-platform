<template>
  <el-drawer
    v-model="visible"
    :title="dialogTitle"
    size="500px"
    direction="rtl"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="config-content">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
      >
      <!-- 开始节点：无配置项 -->
      <template v-if="nodeType === 'start'">
        <el-alert
          type="info"
          :closable="false"
          show-icon
        >
          <template #title>
            <span>开始节点无需配置，它将接收工作流的输入参数。</span>
          </template>
        </el-alert>
      </template>

      <!-- 结束节点：无配置项 -->
      <template v-else-if="nodeType === 'end'">
        <el-alert
          type="info"
          :closable="false"
          show-icon
        >
          <template #title>
            <span>结束节点无需配置，它将输出工作流的执行结果。</span>
          </template>
        </el-alert>
      </template>

      <!-- LLM节点配置 -->
      <template v-else-if="nodeType === 'llm'">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          style="margin-bottom: 20px;"
        >
          <template #title>
            <span style="font-size: 13px;">LLM节点用于调用大语言模型生成文本，可以处理文本生成、对话、摘要、翻译等任务。</span>
          </template>
        </el-alert>

        <el-divider content-position="left">
          <span style="display: flex; align-items: center; gap: 6px;">
            <svg viewBox="0 0 24 24" fill="currentColor" style="width: 16px; height: 16px;">
              <path d="M12 2L2 7l10 5 10-5-10-5z"/>
              <path d="M2 17l10 5 10-5M2 12l10 5 10-5"/>
            </svg>
            模型选择
          </span>
        </el-divider>
        
        <el-form-item label="LLM模型" prop="llmModelId">
          <el-select
            v-model="formData.llmModelId"
            placeholder="请选择要使用的大语言模型"
            filterable
            clearable
            style="width: 100%"
            :loading="loadingModels"
            @visible-change="loadModelsIfNeeded"
          >
            <template #prefix>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" style="width: 14px; height: 14px;">
                <circle cx="12" cy="12" r="3"/>
                <path d="M12 1v6m0 6v6M5.64 5.64l4.24 4.24m4.24 4.24l4.24 4.24M1 12h6m6 0h6M5.64 18.36l4.24-4.24m4.24-4.24l4.24-4.24"/>
              </svg>
            </template>
            <el-option
              v-for="model in llmModels"
              :key="model.id"
              :label="model.displayName || model.name"
              :value="model.id"
            >
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span style="font-weight: 500;">{{ model.displayName || model.name }}</span>
                <div style="display: flex; align-items: center; gap: 8px;">
                  <el-tag v-if="model.isDefault" type="success" size="small" effect="plain">默认</el-tag>
                  <span style="color: #909399; font-size: 12px;">{{ model.provider }}</span>
                </div>
              </div>
            </el-option>
          </el-select>
          <div class="form-item-tip">
            <svg viewBox="0 0 24 24" fill="currentColor" style="width: 12px; height: 12px; margin-right: 4px;">
              <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
            </svg>
            选择此节点使用的LLM模型，不同模型具有不同的能力和性能特点
          </div>
        </el-form-item>

        <el-divider content-position="left">
          <span style="display: flex; align-items: center; gap: 6px;">
            <svg viewBox="0 0 24 24" fill="currentColor" style="width: 16px; height: 16px;">
              <path d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
            </svg>
            提示词配置
          </span>
        </el-divider>
        
        <el-form-item label="提示词模板" prop="prompt">
          <el-input
            v-model="formData.prompt"
            type="textarea"
            :rows="8"
            placeholder="例如：请分析以下文本的情感倾向：{input.text}

你需要判断文本是积极、消极还是中性的，并给出详细的理由。

参考信息：{knowledge_node.output}

请用JSON格式返回结果。"
            class="prompt-textarea"
          />
          <div class="variable-hint-box">
            <div class="variable-hint-title">
              <svg viewBox="0 0 24 24" fill="currentColor" style="width: 14px; height: 14px;">
                <path d="M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/>
              </svg>
              变量语法说明
            </div>
            <div class="variable-hint-content">
              <div class="variable-example">
                <code>{`{input.参数名}`}</code> - 引用工作流输入参数
              </div>
              <div class="variable-example">
                <code>{`{节点ID}`}</code> - 引用其他节点的完整输出
              </div>
              <div class="variable-example">
                <code>{`{节点ID.字段名}`}</code> - 引用其他节点输出的特定字段
              </div>
            </div>
          </div>
          <div class="form-item-tip">
            <svg viewBox="0 0 24 24" fill="currentColor" style="width: 12px; height: 12px; margin-right: 4px;">
              <path d="M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z"/>
            </svg>
            提示词支持变量替换，运行时会自动将变量替换为实际值。使用花括号包裹变量名。
          </div>
        </el-form-item>

        <el-divider content-position="left">
          <span style="display: flex; align-items: center; gap: 6px;">
            <svg viewBox="0 0 24 24" fill="currentColor" style="width: 16px; height: 16px;">
              <path d="M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z"/>
            </svg>
            模型参数
          </span>
        </el-divider>

        <el-form-item prop="temperature">
          <template #label>
            <div style="display: flex; align-items: center; gap: 6px;">
              <span>温度参数 (Temperature)</span>
              <el-tooltip
                effect="dark"
                placement="top"
              >
                <template #content>
                  <div style="max-width: 300px;">
                    <p style="margin: 0 0 8px 0; font-weight: 600;">温度参数说明：</p>
                    <p style="margin: 0 0 4px 0;">• 较低值(0.0-0.3)：输出更确定、一致</p>
                    <p style="margin: 0 0 4px 0;">• 中等值(0.4-0.7)：平衡创造性和准确性</p>
                    <p style="margin: 0;">• 较高值(0.8-2.0)：输出更随机、创造性</p>
                  </div>
                </template>
                <svg viewBox="0 0 24 24" fill="currentColor" style="width: 14px; height: 14px; color: #909399; cursor: help;">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z"/>
                </svg>
              </el-tooltip>
            </div>
          </template>
          <div class="parameter-control-row">
            <el-slider
              v-model="formData.temperature"
              :min="0"
              :max="2"
              :step="0.1"
              :marks="temperatureMarks"
              style="flex: 1;"
            />
            <el-input-number
              v-model="formData.temperature"
              :min="0"
              :max="2"
              :step="0.1"
              :precision="1"
              :controls="false"
              style="width: 80px; margin-left: 16px;"
            />
          </div>
          <div class="parameter-hint">
            <span v-if="formData.temperature <= 0.3" class="hint-badge hint-badge-blue">
              <svg viewBox="0 0 24 24" fill="currentColor" style="width: 12px; height: 12px;">
                <path d="M12 2C8.13 2 5 5.13 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.87-3.13-7-7-7zm2 11.7V16h-4v-2.3C8.48 12.63 7 11.53 7 9c0-2.76 2.24-5 5-5s5 2.24 5 5c0 2.53-1.48 3.63-3 4.7z"/>
              </svg>
              确定性输出 - 适合事实问答、数据提取
            </span>
            <span v-else-if="formData.temperature <= 0.7" class="hint-badge hint-badge-green">
              <svg viewBox="0 0 24 24" fill="currentColor" style="width: 12px; height: 12px;">
                <path d="M12 2C8.13 2 5 5.13 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.87-3.13-7-7-7zm2 11.7V16h-4v-2.3C8.48 12.63 7 11.53 7 9c0-2.76 2.24-5 5-5s5 2.24 5 5c0 2.53-1.48 3.63-3 4.7z"/>
              </svg>
              平衡模式 - 适合大多数对话场景
            </span>
            <span v-else class="hint-badge hint-badge-orange">
              <svg viewBox="0 0 24 24" fill="currentColor" style="width: 12px; height: 12px;">
                <path d="M12 2C8.13 2 5 5.13 5 9c0 2.38 1.19 4.47 3 5.74V17c0 .55.45 1 1 1h6c.55 0 1-.45 1-1v-2.26c1.81-1.27 3-3.36 3-5.74 0-3.87-3.13-7-7-7zm2 11.7V16h-4v-2.3C8.48 12.63 7 11.53 7 9c0-2.76 2.24-5 5-5s5 2.24 5 5c0 2.53-1.48 3.63-3 4.7z"/>
              </svg>
              创造性输出 - 适合内容创作、头脑风暴
            </span>
          </div>
        </el-form-item>

        <el-form-item prop="maxTokens">
          <template #label>
            <div style="display: flex; align-items: center; gap: 6px;">
              <span>最大Token数 (Max Tokens)</span>
              <el-tooltip
                effect="dark"
                placement="top"
              >
                <template #content>
                  <div style="max-width: 300px;">
                    <p style="margin: 0 0 8px 0; font-weight: 600;">Token数量说明：</p>
                    <p style="margin: 0 0 4px 0;">• 1个Token ≈ 0.75个英文单词</p>
                    <p style="margin: 0 0 4px 0;">• 1个Token ≈ 0.5个中文字符</p>
                    <p style="margin: 0;">• 限制模型生成的最大长度，防止输出过长</p>
                  </div>
                </template>
                <svg viewBox="0 0 24 24" fill="currentColor" style="width: 14px; height: 14px; color: #909399; cursor: help;">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z"/>
                </svg>
              </el-tooltip>
            </div>
          </template>
          <div class="parameter-control-row">
            <el-slider
              v-model="formData.maxTokens"
              :min="100"
              :max="10000"
              :step="100"
              :marks="maxTokensMarks"
              style="flex: 1;"
            />
            <el-input-number
              v-model="formData.maxTokens"
              :min="1"
              :max="10000"
              :step="100"
              :controls="false"
              style="width: 100px; margin-left: 16px;"
            />
          </div>
          <div class="parameter-hint">
            <span class="hint-badge hint-badge-gray">
              <svg viewBox="0 0 24 24" fill="currentColor" style="width: 12px; height: 12px;">
                <path d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z"/>
              </svg>
              约 {{ Math.round(formData.maxTokens * 0.5) }} 个中文字 / {{ Math.round(formData.maxTokens * 0.75) }} 个英文单词
            </span>
          </div>
        </el-form-item>
      </template>

      <!-- HTTP请求节点配置 -->
      <template v-else-if="nodeType === 'http'">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="请求URL" prop="url">
          <el-input
            v-model="formData.url"
            placeholder="请输入请求URL，支持变量替换"
          />
          <div class="form-item-tip">
            支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>

        <el-divider content-position="left">请求配置</el-divider>

        <el-form-item label="请求方法" prop="method">
          <el-radio-group v-model="formData.method">
            <el-radio label="GET">GET</el-radio>
            <el-radio label="POST">POST</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="请求头" prop="headers">
        <div class="key-value-editor">
            <div
              v-for="(key, index) in headerKeys"
              :key="key"
              class="key-value-item"
            >
              <el-input
                v-model="headerKeys[index]"
                placeholder="Header名称"
                style="width: 40%"
                @input="updateHeaderKey(index, $event)"
              />
              <el-input
                v-model="formData.headers[key]"
                placeholder="Header值，支持变量替换"
                style="width: 60%"
              />
              <el-button
                type="danger"
                :icon="Delete"
                circle
                size="small"
                @click="removeHeader(key)"
              />
            </div>
            <el-button
              type="primary"
              :icon="Plus"
              size="small"
              @click="addHeader"
            >
              添加请求头
            </el-button>
          </div>
        </el-form-item>

        <el-form-item
          v-if="formData.method === 'POST'"
          label="请求体"
          prop="body"
        >
          <el-input
            v-model="bodyString"
            type="textarea"
            :rows="6"
            placeholder="请输入请求体（JSON格式），支持变量替换"
            @blur="handleBodyChange"
          />
          <div class="form-item-tip">
            支持JSON格式，支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>
      </template>

      <!-- 知识库检索节点配置 -->
      <template v-else-if="nodeType === 'knowledge'">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="知识库" prop="knowledgeBaseId">
          <el-select
            v-model="formData.knowledgeBaseId"
            placeholder="请选择知识库"
            filterable
            clearable
            style="width: 100%"
            :loading="loadingKnowledgeBases"
            @visible-change="loadKnowledgeBasesIfNeeded"
          >
            <el-option
              v-for="kb in knowledgeBases"
              :key="kb.id"
              :label="kb.name"
              :value="kb.id"
            />
          </el-select>
          <div class="form-item-tip">选择要检索的知识库</div>
        </el-form-item>

        <el-divider content-position="left">检索配置</el-divider>

        <el-form-item label="查询文本" prop="query">
          <el-input
            v-model="formData.query"
            type="textarea"
            :rows="3"
            placeholder="请输入查询文本，支持变量替换"
          />
          <div class="form-item-tip">
            支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>

        <el-form-item label="Top-K" prop="topK">
          <el-input-number
            v-model="formData.topK"
            :min="1"
            :max="10"
            style="width: 100%"
          />
          <div class="form-item-tip">返回最相似的K个文档块，范围：1-10，默认5</div>
        </el-form-item>

        <el-form-item label="相似度阈值" prop="similarityThreshold">
          <el-slider
            v-model="formData.similarityThreshold"
            :min="0"
            :max="1"
            :step="0.01"
            show-input
            :show-input-controls="false"
            style="width: 100%"
          />
          <div class="form-item-tip">范围：0-1，默认0.7</div>
        </el-form-item>
      </template>

      <!-- 意图识别节点配置 -->
      <template v-else-if="nodeType === 'intent'">
        <el-divider content-position="left">基础信息</el-divider>
        <el-form-item label="输入文本" prop="inputText">
          <el-input
            v-model="formData.inputText"
            type="textarea"
            :rows="3"
            placeholder="请输入要识别的文本，支持变量替换"
          />
          <div class="form-item-tip">
            支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>

        <el-form-item label="意图类别" prop="intentCategories">
          <el-select
            v-model="formData.intentCategories"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请输入或选择意图类别"
            style="width: 100%"
          >
            <el-option
              v-for="category in formData.intentCategories"
              :key="category"
              :label="category"
              :value="category"
            />
          </el-select>
          <div class="form-item-tip">可以输入新的意图类别，多个类别用逗号分隔</div>
        </el-form-item>

        <el-divider content-position="left">识别配置</el-divider>

        <el-form-item label="识别方式" prop="recognitionMethod">
          <el-radio-group v-model="formData.recognitionMethod">
            <el-radio label="llm">LLM识别</el-radio>
            <el-radio label="keyword">关键词匹配</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item
          v-if="formData.recognitionMethod === 'llm'"
          label="LLM模型"
          prop="llmModelId"
        >
          <el-select
            v-model="formData.llmModelId"
            placeholder="请选择LLM模型"
            filterable
            clearable
            style="width: 100%"
            :loading="loadingModels"
            @visible-change="loadModelsIfNeeded"
          >
            <el-option
              v-for="model in llmModels"
              :key="model.id"
              :label="model.displayName || model.name"
              :value="model.id"
            >
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span>{{ model.displayName || model.name }}</span>
                <span style="color: #909399; font-size: 12px; margin-left: 8px;">{{ model.provider }}</span>
              </div>
            </el-option>
          </el-select>
          <div class="form-item-tip">选择用于意图识别的LLM模型</div>
        </el-form-item>

        <el-form-item
          v-if="formData.recognitionMethod === 'keyword'"
          label="关键词映射"
          prop="keywords"
        >
          <div class="keywords-editor">
            <div
              v-for="(key, index) in keywordIntentKeys"
              :key="key"
              class="keyword-item"
            >
              <el-input
                v-model="keywordIntentKeys[index]"
                placeholder="意图名称"
                style="width: 30%"
                @input="updateKeywordIntentKey(index, $event)"
              />
              <el-select
                v-model="formData.keywords[key]"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="关键词列表"
                style="width: 70%"
              />
              <el-button
                type="danger"
                :icon="Delete"
                circle
                size="small"
                @click="removeKeywordIntent(key)"
              />
            </div>
            <el-button
              type="primary"
              :icon="Plus"
              size="small"
              @click="addKeywordIntent"
            >
              添加意图关键词
            </el-button>
          </div>
        </el-form-item>
      </template>

      <!-- 字符串处理节点配置 -->
      <template v-else-if="nodeType === 'string'">
        <el-divider content-position="left">基础信息</el-divider>
        <el-divider content-position="left">操作配置</el-divider>

        <el-form-item label="操作类型" prop="operation">
          <el-select
            v-model="formData.operation"
            placeholder="请选择操作类型"
            style="width: 100%"
          >
            <el-option label="拼接 (concat)" value="concat" />
            <el-option label="替换 (replace)" value="replace" />
            <el-option label="截取 (substring)" value="substring" />
            <el-option label="格式化 (format)" value="format" />
            <el-option label="去空格 (trim)" value="trim" />
            <el-option label="转大写 (upper)" value="upper" />
            <el-option label="转小写 (lower)" value="lower" />
          </el-select>
        </el-form-item>

        <el-form-item label="输入字符串" prop="inputString">
          <el-input
            v-model="formData.inputString"
            type="textarea"
            :rows="3"
            placeholder="请输入字符串，支持变量替换"
          />
          <div class="form-item-tip">
            支持变量替换：{input.param}、{node_id}、{node_id.field}
          </div>
        </el-form-item>

        <el-divider content-position="left">操作参数</el-divider>

        <!-- 根据操作类型显示不同的参数配置 -->
        <template v-if="formData.operation === 'concat'">
          <el-form-item label="分隔符" prop="parameters.separator">
            <el-input
              v-model="formData.parameters.separator"
              placeholder="分隔符（可选，默认为空）"
            />
            <div class="form-item-tip">用于分隔多个字符串的分隔符</div>
          </el-form-item>
          <el-form-item label="拼接字符串列表" prop="parameters.strings">
            <div class="key-value-editor">
              <div
                v-for="(_str, index) in concatStrings"
                :key="index"
                class="key-value-item"
              >
                <el-input
                  v-model="concatStrings[index]"
                  :placeholder="`字符串 ${index + 1}`"
                  style="width: calc(100% - 40px)"
                />
                <el-button
                  type="danger"
                  :icon="Delete"
                  circle
                  size="small"
                  @click="removeConcatString(index)"
                />
              </div>
              <el-button
                type="primary"
                :icon="Plus"
                size="small"
                @click="addConcatString"
              >
                添加字符串
              </el-button>
            </div>
            <div class="form-item-tip">要拼接的字符串列表，将按顺序用分隔符连接</div>
          </el-form-item>
        </template>

        <template v-else-if="formData.operation === 'replace'">
          <el-form-item label="查找字符串" prop="parameters.target">
            <el-input
              v-model="formData.parameters.target"
              placeholder="要查找的字符串"
            />
            <div class="form-item-tip">在输入字符串中要查找并替换的文本</div>
          </el-form-item>
          <el-form-item label="替换为" prop="parameters.replacement">
            <el-input
              v-model="formData.parameters.replacement"
              placeholder="替换为的字符串"
            />
            <div class="form-item-tip">用于替换查找字符串的文本</div>
          </el-form-item>
        </template>

        <template v-else-if="formData.operation === 'substring'">
          <el-form-item label="起始位置" prop="parameters.start">
            <el-input-number
              v-model="formData.parameters.start"
              :min="-9999"
              style="width: 100%"
            />
            <div class="form-item-tip">支持负数索引，-1表示最后一个字符</div>
          </el-form-item>
          <el-form-item label="结束位置" prop="parameters.end">
            <el-input-number
              v-model="formData.parameters.end"
              :min="-9999"
              style="width: 100%"
            />
            <div class="form-item-tip">可选，不填则截取到末尾。支持负数索引</div>
          </el-form-item>
        </template>

        <template v-else-if="formData.operation === 'format'">
          <el-form-item label="格式化值映射" prop="parameters.values">
            <div class="key-value-editor">
              <div
                v-for="(_value, key, index) in formatValues"
                :key="index"
                class="key-value-item"
              >
                <el-input
                  v-model="formatKeys[index]"
                  placeholder="占位符key（如：name）"
                  style="width: 40%"
                  @input="updateFormatKey(index, $event)"
                />
                <el-input
                  v-model="formatValues[key]"
                  placeholder="替换值，支持变量替换"
                  style="width: 60%"
                />
                <el-button
                  type="danger"
                  :icon="Delete"
                  circle
                  size="small"
                  @click="removeFormatValue(key)"
                />
              </div>
              <el-button
                type="primary"
                :icon="Plus"
                size="small"
                @click="addFormatValue"
              >
                添加格式化值
              </el-button>
            </div>
            <div class="form-item-tip" v-pre>
              在输入字符串中使用双大括号占位符，如：Hello {{name}}，您有 {{count}} 条消息
            </div>
          </el-form-item>
        </template>
      </template>
    </el-form>
    </div>
    <div class="config-footer">
      <el-button @click="handleClose" style="flex: 1;">取消</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving" style="flex: 1;">
        保存
      </el-button>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { getAgentList } from '@/api/agent'
import { getKnowledgeBaseList } from '@/api/knowledgeBase'
import { getLlmModelList } from '@/api/llm'
import type { Agent, LlmModel } from '@/types/entity'
import type { KnowledgeBase } from '@/api/knowledgeBase'
import type { WorkflowNode } from '@/api/workflow'

interface Props {
  modelValue: boolean
  node: WorkflowNode | null
  availableNodes?: WorkflowNode[] // 用于变量替换提示
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'save', config: any): void
}

const props = withDefaults(defineProps<Props>(), {
  availableNodes: () => []
})

const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const nodeType = computed(() => props.node?.type || '')
const dialogTitle = computed(() => {
  const nodeName = props.node?.label || '节点'
  return `配置 ${nodeName}`
})

const saving = ref(false)

// 表单数据
const formData = ref<any>({})
const headerKeys = ref<string[]>([])
const keywordIntentKeys = ref<string[]>([])
const bodyString = ref('')

// LLM节点参数标记
const temperatureMarks = {
  0: '0',
  0.7: '0.7',
  1.0: '1.0',
  2.0: '2.0'
}

const maxTokensMarks = {
  100: '100',
  2000: '2000',
  5000: '5000',
  10000: '10000'
}

// 字符串处理节点相关
const concatStrings = ref<string[]>([])
const formatKeys = ref<string[]>([])
const formatValues = ref<Record<string, string>>({})

// 智能体列表（用于意图识别节点）
const agents = ref<Agent[]>([])
const loadingAgents = ref(false)
const agentsLoaded = ref(false)

// LLM模型列表（用于LLM节点）
const llmModels = ref<LlmModel[]>([])
const loadingModels = ref(false)
const modelsLoaded = ref(false)

// 知识库列表
const knowledgeBases = ref<KnowledgeBase[]>([])
const loadingKnowledgeBases = ref(false)
const knowledgeBasesLoaded = ref(false)

// 表单验证规则
const formRules = computed<FormRules>(() => {
  const rules: FormRules = {}
  
  if (nodeType.value === 'llm') {
    rules.llmModelId = [
      { required: true, message: '请选择LLM模型', trigger: 'change' }
    ]
    rules.prompt = [
      { required: true, message: '请输入提示词', trigger: 'blur' }
    ]
    rules.temperature = [
      { type: 'number', min: 0, max: 2, message: '温度参数必须在0-2之间', trigger: 'blur' }
    ]
    rules.maxTokens = [
      { type: 'number', min: 1, message: '最大Token数必须大于0', trigger: 'blur' }
    ]
  } else if (nodeType.value === 'http') {
    rules.url = [
      { required: true, message: '请输入请求URL', trigger: 'blur' }
    ]
  } else if (nodeType.value === 'knowledge') {
    rules.knowledgeBaseId = [
      { required: true, message: '请选择知识库', trigger: 'change' }
    ]
    rules.query = [
      { required: true, message: '请输入查询文本', trigger: 'blur' }
    ]
    rules.topK = [
      { type: 'number', min: 1, max: 10, message: 'Top-K必须在1-10之间', trigger: 'blur' }
    ]
    rules.similarityThreshold = [
      { type: 'number', min: 0, max: 1, message: '相似度阈值必须在0-1之间', trigger: 'blur' }
    ]
  } else if (nodeType.value === 'intent') {
    rules.inputText = [
      { required: true, message: '请输入输入文本', trigger: 'blur' }
    ]
    rules.intentCategories = [
      { required: true, message: '请输入意图类别', trigger: 'change' }
    ]
    rules.llmModelId = [
      { 
        validator: (_rule, value, callback) => {
          if (formData.value.recognitionMethod === 'llm' && !value) {
            callback(new Error('使用LLM识别方式时，请选择LLM模型'))
          } else {
            callback()
          }
        },
        trigger: 'change'
      }
    ]
    rules.keywords = [
      {
        validator: (_rule, value, callback) => {
          if (formData.value.recognitionMethod === 'keyword' && (!value || Object.keys(value).length === 0)) {
            callback(new Error('使用关键词匹配方式时，请配置关键词映射'))
          } else {
            callback()
          }
        },
        trigger: 'change'
      }
    ]
  } else if (nodeType.value === 'string') {
    rules.operation = [
      { required: true, message: '请选择操作类型', trigger: 'change' }
    ]
    rules.inputString = [
      { required: true, message: '请输入输入字符串', trigger: 'blur' }
    ]
  }
  
  return rules
})

// 初始化表单数据
const initFormData = () => {
  if (!props.node) {
    formData.value = {}
    return
  }

  const config = props.node.config || {}
  const type = props.node.type

  // 根据节点类型初始化表单数据
  if (type === 'start' || type === 'end') {
    formData.value = {}
  } else if (type === 'llm') {
    formData.value = {
      llmModelId: config.llmModelId || '',
      prompt: config.prompt || '',
      temperature: config.temperature ?? 0.7,
      maxTokens: config.maxTokens ?? 2000
    }
  } else if (type === 'http') {
    formData.value = {
      url: config.url || '',
      method: config.method || 'GET',
      headers: config.headers || {},
      body: config.body || null
    }
    headerKeys.value = Object.keys(formData.value.headers)
    bodyString.value = config.body ? JSON.stringify(config.body, null, 2) : ''
  } else if (type === 'knowledge') {
    // 知识库ID可能是数字或字符串，统一处理为字符串
    const kbId = config.knowledgeBaseId
    formData.value = {
      knowledgeBaseId: kbId ? String(kbId) : null,
      query: config.query || '',
      topK: config.topK ?? 5,
      similarityThreshold: config.similarityThreshold ?? 0.7
    }
  } else if (type === 'intent') {
    formData.value = {
      inputText: config.inputText || '',
      intentCategories: config.intentCategories || [],
      recognitionMethod: config.recognitionMethod || 'llm',
      llmModelId: config.llmModelId || '',
      keywords: config.keywords || {}
    }
    keywordIntentKeys.value = Object.keys(formData.value.keywords)
  } else if (type === 'string') {
    formData.value = {
      operation: config.operation || '',
      inputString: config.inputString || '',
      parameters: config.parameters || {}
    }
    
    // 初始化concat字符串列表
    if (config.operation === 'concat' && config.parameters?.strings) {
      concatStrings.value = [...(config.parameters.strings as string[])]
    } else {
      concatStrings.value = []
    }
    
    // 初始化format值映射
    if (config.operation === 'format' && config.parameters?.values) {
      formatValues.value = { ...(config.parameters.values as Record<string, string>) }
      formatKeys.value = Object.keys(formatValues.value)
    } else {
      formatValues.value = {}
      formatKeys.value = []
    }
  } else {
    // 重置字符串处理相关数据
    concatStrings.value = []
    formatValues.value = {}
    formatKeys.value = []
  }
}

// 加载智能体列表
const loadAgents = async () => {
  if (agentsLoaded.value) return
  
  loadingAgents.value = true
  try {
    agents.value = await getAgentList()
    agentsLoaded.value = true
  } catch (error: any) {
    console.error('加载智能体列表失败:', error)
    ElMessage.error(error.message || '加载智能体列表失败')
  } finally {
    loadingAgents.value = false
  }
}

const loadAgentsIfNeeded = (visible: boolean) => {
  if (visible && !agentsLoaded.value) {
    loadAgents()
  }
}

// 加载LLM模型列表
const loadModels = async () => {
  if (modelsLoaded.value) return
  
  loadingModels.value = true
  try {
    llmModels.value = await getLlmModelList()
    modelsLoaded.value = true
  } catch (error: any) {
    console.error('加载LLM模型列表失败:', error)
    ElMessage.error(error.message || '加载LLM模型列表失败')
  } finally {
    loadingModels.value = false
  }
}

const loadModelsIfNeeded = (visible: boolean) => {
  if (visible && !modelsLoaded.value) {
    loadModels()
  }
}

// 加载知识库列表
const loadKnowledgeBases = async () => {
  if (knowledgeBasesLoaded.value) return
  
  loadingKnowledgeBases.value = true
  try {
    const result = await getKnowledgeBaseList({ pageSize: 1000 })
    knowledgeBases.value = result.list || []
    knowledgeBasesLoaded.value = true
  } catch (error: any) {
    console.error('加载知识库列表失败:', error)
    ElMessage.error(error.message || '加载知识库列表失败')
  } finally {
    loadingKnowledgeBases.value = false
  }
}

const loadKnowledgeBasesIfNeeded = (visible: boolean) => {
  if (visible && !knowledgeBasesLoaded.value) {
    loadKnowledgeBases()
  }
}

// HTTP节点：请求头管理
const addHeader = () => {
  const key = `header_${Date.now()}`
  formData.value.headers[key] = ''
  headerKeys.value.push(key)
}

const removeHeader = (key: string) => {
  delete formData.value.headers[key]
  const index = headerKeys.value.indexOf(key)
  if (index > -1) {
    headerKeys.value.splice(index, 1)
  }
}

const updateHeaderKey = (index: number, newKey: string) => {
  const oldKey = headerKeys.value[index]
  if (oldKey && oldKey !== newKey) {
    const value = formData.value.headers[oldKey]
    delete formData.value.headers[oldKey]
    formData.value.headers[newKey] = value
    headerKeys.value[index] = newKey
  }
}

// HTTP节点：请求体处理
const handleBodyChange = () => {
  if (!bodyString.value.trim()) {
    formData.value.body = null
    return
  }
  
  try {
    formData.value.body = JSON.parse(bodyString.value)
  } catch (error) {
    ElMessage.warning('请求体格式不正确，请检查JSON格式')
  }
}

// 意图识别节点：关键词映射管理
const addKeywordIntent = () => {
  const intent = `intent_${Date.now()}`
  formData.value.keywords[intent] = []
  keywordIntentKeys.value.push(intent)
}

const removeKeywordIntent = (intent: string) => {
  delete formData.value.keywords[intent]
  const index = keywordIntentKeys.value.indexOf(intent)
  if (index > -1) {
    keywordIntentKeys.value.splice(index, 1)
  }
}

const updateKeywordIntentKey = (index: number, newKey: string) => {
  const oldKey = keywordIntentKeys.value[index]
  if (oldKey && oldKey !== newKey && newKey) {
    const keywords = formData.value.keywords[oldKey] || []
    delete formData.value.keywords[oldKey]
    formData.value.keywords[newKey] = keywords
    keywordIntentKeys.value[index] = newKey
  }
}

// 字符串处理节点：concat字符串管理
const addConcatString = () => {
  concatStrings.value.push('')
}

const removeConcatString = (index: number) => {
  concatStrings.value.splice(index, 1)
}

// 字符串处理节点：format值映射管理
const addFormatValue = () => {
  const key = `key_${Date.now()}`
  formatValues.value[key] = ''
  formatKeys.value.push(key)
}

const removeFormatValue = (key: string) => {
  delete formatValues.value[key]
  const index = formatKeys.value.indexOf(key)
  if (index > -1) {
    formatKeys.value.splice(index, 1)
  }
}

const updateFormatKey = (index: number, newKey: string) => {
  const oldKey = formatKeys.value[index]
  if (oldKey && oldKey !== newKey && newKey) {
    const value = formatValues.value[oldKey] || ''
    delete formatValues.value[oldKey]
    formatValues.value[newKey] = value
    formatKeys.value[index] = newKey
  }
}

// 保存配置
const handleSave = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    
    // 处理HTTP节点的headers（转换为正确的格式）
    if (nodeType.value === 'http') {
      const headers: Record<string, string> = {}
      headerKeys.value.forEach(key => {
        if (formData.value.headers[key]) {
          headers[key] = formData.value.headers[key]
        }
      })
      formData.value.headers = headers
    }

    // 处理意图识别节点的keywords（转换为正确的格式）
    if (nodeType.value === 'intent' && formData.value.recognitionMethod === 'keyword') {
      const keywords: Record<string, string[]> = {}
      keywordIntentKeys.value.forEach(intent => {
        if (intent && formData.value.keywords[intent] && formData.value.keywords[intent].length > 0) {
          keywords[intent] = formData.value.keywords[intent]
        }
      })
      formData.value.keywords = keywords
    }
    
    // 如果使用LLM方式，清空keywords
    if (nodeType.value === 'intent' && formData.value.recognitionMethod === 'llm') {
      delete formData.value.keywords
    }
    
    // 如果使用关键词方式，清空llmModelId
    if (nodeType.value === 'intent' && formData.value.recognitionMethod === 'keyword') {
      delete formData.value.llmModelId
    }

    // 处理知识库节点的knowledgeBaseId（转换为数字，如果是纯数字字符串）
    if (nodeType.value === 'knowledge' && formData.value.knowledgeBaseId) {
      const kbId = formData.value.knowledgeBaseId
      const numId = Number(kbId)
      // 如果ID是纯数字字符串，转换为数字；否则保持字符串
      if (!isNaN(numId) && isFinite(numId) && numId > 0) {
        formData.value.knowledgeBaseId = numId
      }
      // 否则保持字符串（后端可能需要修改以支持String类型）
    }

    // 处理字符串处理节点的parameters
    if (nodeType.value === 'string') {
      const operation = formData.value.operation
      const parameters: Record<string, any> = {}
      
      if (operation === 'concat') {
        // concat操作：需要strings数组和separator
        parameters.separator = formData.value.parameters?.separator || ''
        parameters.strings = concatStrings.value.filter(s => s && s.trim())
      } else if (operation === 'replace') {
        // replace操作：需要target和replacement
        parameters.target = formData.value.parameters?.target || ''
        parameters.replacement = formData.value.parameters?.replacement || ''
      } else if (operation === 'substring') {
        // substring操作：需要start和end（可选）
        if (formData.value.parameters?.start !== undefined) {
          parameters.start = formData.value.parameters.start
        }
        if (formData.value.parameters?.end !== undefined) {
          parameters.end = formData.value.parameters.end
        }
      } else if (operation === 'format') {
        // format操作：需要values对象
        const values: Record<string, string> = {}
        formatKeys.value.forEach(key => {
          if (key && formatValues.value[key] !== undefined) {
            values[key] = formatValues.value[key]
          }
        })
        parameters.values = values
      }
      // trim、upper、lower操作不需要parameters
      
      formData.value.parameters = parameters
    }

    // 发送保存事件
    emit('save', { ...formData.value })
    ElMessage.success('配置保存成功')
    visible.value = false
  } catch (error) {
    console.error('表单验证失败:', error)
  }
}

// 关闭对话框
const handleClose = () => {
  visible.value = false
}

// 监听节点变化，重新初始化表单
watch(() => props.node, () => {
  if (props.node) {
    initFormData()
  }
}, { immediate: true })

// 监听对话框显示状态
watch(visible, (newVal) => {
  if (newVal && props.node) {
    initFormData()
  }
})
</script>

<style scoped>
/* 配置内容容器 */
.config-content {
  padding: 0;
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  margin-bottom: 20px;
}

/* 底部按钮区域 */
.config-footer {
  display: flex;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.config-content::-webkit-scrollbar {
  width: 8px;
}

.config-content::-webkit-scrollbar-track {
  background: #f1f3f5;
  border-radius: 4px;
}

.config-content::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 4px;
  transition: background 0.3s ease;
}

.config-content::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}

/* 抽屉样式优化 */
:deep(.el-drawer) {
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
}

:deep(.el-drawer__body) {
  padding: 20px;
  height: calc(100% - 60px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

:deep(.el-drawer__header) {
  margin-bottom: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

:deep(.el-drawer__title) {
  font-weight: 600;
  font-size: 18px;
  color: #303133;
}

/* 表单样式优化 */
:deep(.el-form) {
  padding: 0;
}

:deep(.el-form-item__label) {
  color: #374151;
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
  padding: 0;
}

:deep(.el-divider) {
  margin: 20px 0;
}

:deep(.el-divider__text) {
  font-weight: 600;
  color: #606266;
  font-size: 14px;
}

:deep(.el-input__wrapper) {
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 2px 6px rgba(14, 165, 233, 0.2);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.1);
}

:deep(.el-textarea__inner) {
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

:deep(.el-textarea__inner:hover) {
  box-shadow: 0 2px 6px rgba(14, 165, 233, 0.2);
}

:deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.1);
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-select .el-input__wrapper) {
  border-radius: 8px;
}

:deep(.el-radio-group) {
  display: flex;
  gap: 16px;
}

:deep(.el-radio) {
  margin-right: 0;
}

:deep(.el-radio__input.is-checked .el-radio__inner) {
  background-color: #0ea5e9;
  border-color: #0ea5e9;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 600;
  padding: 10px 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

:deep(.el-button--primary) {
  background: var(--gradient-bg-card-header);
  border: none;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.3);
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(14, 165, 233, 0.4);
}

:deep(.el-button--primary:active) {
  transform: translateY(0);
}

:deep(.el-alert) {
  border-radius: 10px;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

:deep(.el-alert--info) {
  background: linear-gradient(135deg, #e0e7ff 0%, #c7d2fe 100%);
  color: #4338ca;
}

:deep(.el-slider) {
  margin-top: 8px;
}

:deep(.el-slider__runway) {
  background-color: #e5e7eb;
  border-radius: 4px;
}

:deep(.el-slider__bar) {
  background: var(--gradient-bg-card-header);
  border-radius: 4px;
}

:deep(.el-slider__button) {
  border: 3px solid #0ea5e9;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(14, 165, 233, 0.3);
  transition: all 0.3s ease;
}

:deep(.el-slider__button:hover) {
  transform: scale(1.2);
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.4);
}

.form-item-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  line-height: 1.5;
}

.key-value-editor {
  width: 100%;
  background: #f9fafb;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.key-value-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.key-value-item:hover {
  box-shadow: 0 2px 6px rgba(14, 165, 233, 0.15);
  transform: translateX(2px);
}

.key-value-item:last-child {
  margin-bottom: 0;
}

.keywords-editor {
  width: 100%;
  background: #f9fafb;
  padding: 16px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
}

.keyword-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  padding: 10px;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.keyword-item:hover {
  box-shadow: 0 2px 6px rgba(236, 72, 153, 0.15);
  transform: translateX(2px);
}

.keyword-item:last-child {
  margin-bottom: 0;
}

/* LLM节点特定样式 */
.prompt-textarea :deep(.el-textarea__inner) {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
}

.variable-hint-box {
  margin-top: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 8px;
  border: 1px solid #bae6fd;
}

.variable-hint-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #0369a1;
  margin-bottom: 8px;
}

.variable-hint-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.variable-example {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #0c4a6e;
}

.variable-example code {
  display: inline-block;
  padding: 2px 8px;
  margin-right: 8px;
  background: rgba(255, 255, 255, 0.8);
  border: 1px solid #7dd3fc;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 12px;
  color: #0369a1;
  font-weight: 600;
  min-width: 140px;
}

.parameter-control-row {
  display: flex;
  align-items: center;
  width: 100%;
}

.parameter-hint {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hint-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.5;
}

.hint-badge-blue {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
  color: #1e40af;
  border: 1px solid #93c5fd;
}

.hint-badge-green {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #065f46;
  border: 1px solid #6ee7b7;
}

.hint-badge-orange {
  background: linear-gradient(135deg, #fed7aa 0%, #fdba74 100%);
  color: #9a3412;
  border: 1px solid #fb923c;
}

.hint-badge-gray {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  color: #374151;
  border: 1px solid #d1d5db;
}

/* 优化slider样式 */
:deep(.el-slider__marks-text) {
  font-size: 11px;
  color: #6b7280;
  margin-top: 8px;
}

:deep(.el-slider__mark) {
  width: 2px;
  height: 4px;
  background-color: #d1d5db;
}

/* 优化tooltip样式 */
:deep(.el-tooltip__trigger) {
  display: inline-flex;
  align-items: center;
}
</style>

