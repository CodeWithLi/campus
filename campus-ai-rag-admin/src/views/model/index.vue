<template>
  <div class="model-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">模型配置管理</h2>
      </div>
      <el-button v-if="canEditModelConfig" type="primary" @click="openCreateDialog">新增模型配置</el-button>
    </div>

    <div class="table-card" v-loading="tableLoading">
      <el-table :data="modelList" height="100%">
        <el-table-column prop="modelCode" label="模型编码" min-width="140" />
        <el-table-column prop="modelName" label="模型名称" min-width="160" />
        <el-table-column label="调用方式" min-width="160">
          <template #default="{ row }">
            <el-tag :type="getInvokeTypeTag(row.invokeType)">{{ getInvokeTypeLabel(row.invokeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="provider" label="提供商" min-width="110" />
        <el-table-column prop="modelNameRemote" label="远程模型名" min-width="150" />
        <el-table-column prop="enabled" label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="图片对话" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.supportsImageChat === 1 ? 'success' : 'info'">
              {{ row.supportsImageChat === 1 ? '支持' : '不支持' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" min-width="80" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column v-if="canEditModelConfig || canTestModelConfig" label="操作" fixed="right" min-width="240">
          <template #default="{ row }">
            <el-button link type="success" @click="openTestDialog(row)">连通测试</el-button>
            <el-button v-if="canEditModelConfig" link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button v-if="canEditModelConfig" link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增模型配置' : '编辑模型配置'"
      width="860px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="130px"
        class="config-form"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模型编码" prop="modelCode">
              <el-input
                v-model="formData.modelCode"
                placeholder="例如 qwen_plus"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model="formData.modelName" placeholder="例如 通义千问Plus" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="调用方式" prop="invokeType">
              <el-select v-model="formData.invokeType" class="full-width">
                <el-option
                  v-for="item in invokeTypeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提供商" prop="provider">
              <el-input v-model="formData.provider" placeholder="例如 qwen / xunfei / deepseek" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="API Base URL" prop="apiBaseUrl">
              <el-input v-model="formData.apiBaseUrl" placeholder="例如 https://api.example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="API Key" prop="apiKey">
              <el-input
                v-model="formData.apiKey"
                type="password"
                show-password
                :placeholder="dialogMode === 'edit' ? '留空表示不修改' : '请输入调用密钥'"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="远程模型名" prop="modelNameRemote">
              <el-input v-model="formData.modelNameRemote" placeholder="例如 qwen-plus" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="温度" prop="temperature">
              <el-input-number v-model="formData.temperature" :min="0" :max="2" :step="0.1" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Max Tokens" prop="maxTokens">
              <el-input-number v-model="formData.maxTokens" :min="1" :step="1" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>

        <template v-if="formData.invokeType === INVOKE_TYPE_GENERIC_HTTP">
          <el-divider content-position="left">通用HTTP配置（invokeType=3）</el-divider>

          <el-row :gutter="16">
            <el-col :span="12">
              <el-form-item label="请求体类型" prop="genericReqType">
                <el-select v-model="formData.genericReqType" class="full-width" placeholder="messages / prompt / input">
                  <el-option label="messages" value="messages" />
                  <el-option label="prompt" value="prompt" />
                  <el-option label="input" value="input" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="请求字段名" prop="genericReqField">
                <el-input v-model="formData.genericReqField" placeholder="例如 messages" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="响应提取路径" prop="genericRespPath">
            <el-input v-model="formData.genericRespPath" placeholder="例如 data.choices[0].message.content（可为空）" />
          </el-form-item>

          <el-form-item label="额外请求头JSON" prop="genericHeadersJson">
            <el-input
              v-model="formData.genericHeadersJson"
              type="textarea"
              :rows="3"
              placeholder='例如 {"Authorization":"Bearer xxx"}'
            />
          </el-form-item>
        </template>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="状态" prop="enabled">
              <el-select v-model="formData.enabled" class="full-width">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="图片对话" prop="supportsImageChat">
              <el-select v-model="formData.supportsImageChat" class="full-width">
                <el-option label="支持" :value="1" />
                <el-option label="不支持" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="排序号" prop="sortNo">
              <el-input-number v-model="formData.sortNo" :min="0" :step="1" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="8"></el-col>
        </el-row>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="可选备注信息" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ submitLoading ? '提交中...' : '确认' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="testDialogVisible"
      title="模型连通性测试"
      width="760px"
      destroy-on-close
    >
      <el-form
        ref="testFormRef"
        :model="testFormData"
        :rules="testFormRules"
        label-width="120px"
      >
        <el-form-item label="模型编码" prop="modelCode">
          <el-input v-model="testFormData.modelCode" disabled />
        </el-form-item>
        <el-form-item label="测试问题" prop="message">
          <el-input
            v-model="testFormData.message"
            type="textarea"
            :rows="4"
            placeholder="可选，留空则由后端使用默认问题"
          />
        </el-form-item>
      </el-form>

      <div v-if="testResult" class="test-result-card">
        <div class="test-result-title">测试结果</div>
        <div class="test-result-meta">
          <span>模型：{{ testResult.modelCode }}</span>
          <span>耗时：{{ testResult.elapsedMs }} ms</span>
        </div>
        <div class="test-result-question">
          <b>问题：</b>{{ testResult.question }}
        </div>
        <div class="test-result-answer">
          <b>回复：</b>{{ testResult.answer || '-' }}
        </div>
      </div>

      <template #footer>
        <el-button @click="testDialogVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testLoading" @click="handleTestChat">
          {{ testLoading ? '测试中...' : '开始测试' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createModelConfig,
  deleteModelConfig,
  getModelConfigDetail,
  getModelConfigList,
  testModelChat,
  updateModelConfig
} from '@/api/model'
import { useUserStore } from '@/stores/user'

const INVOKE_TYPE_SPRING_AI = 1
const INVOKE_TYPE_XF_HTTP = 2
const INVOKE_TYPE_GENERIC_HTTP = 3

const userStore = useUserStore()
const canEditModelConfig = computed(() => userStore.isSuperAdmin)
const canTestModelConfig = computed(() => Boolean(userStore.token))

const invokeTypeOptions = [
  { value: INVOKE_TYPE_SPRING_AI, label: '1 - springai' },
  { value: INVOKE_TYPE_XF_HTTP, label: '2 - xf_http（讯飞星火）' },
  { value: INVOKE_TYPE_GENERIC_HTTP, label: '3 - generic_http（通用HTTP）' }
]

const formRef = ref()
const tableLoading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const dialogMode = ref('create')
const currentId = ref(null)
const modelList = ref([])
const testDialogVisible = ref(false)
const testLoading = ref(false)
const testFormRef = ref()
const testResult = ref(null)

const testFormData = reactive({
  modelCode: '',
  message: ''
})

const testFormRules = {
  modelCode: [
    { required: true, message: '模型编码不能为空', trigger: 'blur' }
  ]
}

const createDefaultFormData = () => ({
  modelCode: '',
  modelName: '',
  invokeType: INVOKE_TYPE_SPRING_AI,
  provider: '',
  apiBaseUrl: '',
  apiKey: '',
  modelNameRemote: '',
  temperature: null,
  maxTokens: null,
  genericReqType: '',
  genericReqField: '',
  genericRespPath: '',
  genericHeadersJson: '',
  enabled: 1,
  supportsImageChat: 0,
  sortNo: 100,
  remark: ''
})

const formData = reactive(createDefaultFormData())

const formRules = {
  modelCode: [
    { required: true, message: '请输入模型编码', trigger: 'blur' }
  ],
  modelName: [
    { required: true, message: '请输入模型名称', trigger: 'blur' }
  ],
  invokeType: [
    { required: true, message: '请选择调用方式', trigger: 'change' }
  ],
  genericReqType: [
    {
      validator: (rule, value, callback) => {
        if (formData.invokeType !== INVOKE_TYPE_GENERIC_HTTP) {
          callback()
          return
        }
        if (!value) {
          callback(new Error('invokeType=3 时，请选择请求体类型'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  genericReqField: [
    {
      validator: (rule, value, callback) => {
        if (formData.invokeType !== INVOKE_TYPE_GENERIC_HTTP) {
          callback()
          return
        }
        if (!value || !value.trim()) {
          callback(new Error('invokeType=3 时，请填写请求字段名'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

const resetFormData = () => {
  Object.assign(formData, createDefaultFormData())
}

const getInvokeTypeLabel = (value) => {
  if (value === INVOKE_TYPE_SPRING_AI) return 'springai'
  if (value === INVOKE_TYPE_XF_HTTP) return 'xf_http'
  if (value === INVOKE_TYPE_GENERIC_HTTP) return 'generic_http'
  return '-'
}

const getInvokeTypeTag = (value) => {
  if (value === INVOKE_TYPE_SPRING_AI) return 'success'
  if (value === INVOKE_TYPE_XF_HTTP) return 'warning'
  if (value === INVOKE_TYPE_GENERIC_HTTP) return ''
  return 'info'
}

const loadModelList = async () => {
  tableLoading.value = true
  try {
    const res = await getModelConfigList()
    modelList.value = res.data || []
  } finally {
    tableLoading.value = false
  }
}

const openCreateDialog = () => {
  if (!canEditModelConfig.value) {
    ElMessage.warning('当前账号无新增权限')
    return
  }
  dialogMode.value = 'create'
  currentId.value = null
  resetFormData()
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  if (!canEditModelConfig.value) {
    ElMessage.warning('当前账号无编辑权限')
    return
  }
  dialogMode.value = 'edit'
  currentId.value = row.id
  dialogVisible.value = true
  resetFormData()

  const res = await getModelConfigDetail(row.id)
  Object.assign(formData, {
    ...createDefaultFormData(),
    ...res.data,
    apiKey: ''
  })
}

const openTestDialog = (row) => {
  if (!canTestModelConfig.value) {
    ElMessage.warning('当前账号无测试权限')
    return
  }
  testFormData.modelCode = row.modelCode
  testFormData.message = ''
  testResult.value = null
  testDialogVisible.value = true
}

const handleTestChat = async () => {
  const valid = await testFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  testLoading.value = true
  try {
    const res = await testModelChat({
      modelCode: testFormData.modelCode,
      message: testFormData.message?.trim() || null
    })
    testResult.value = res.data
    ElMessage.success('连通测试成功')
  } finally {
    testLoading.value = false
  }
}

const buildCreatePayload = () => {
  return {
    modelCode: formData.modelCode,
    modelName: formData.modelName,
    invokeType: formData.invokeType,
    provider: formData.provider,
    apiBaseUrl: formData.apiBaseUrl,
    apiKey: formData.apiKey,
    modelNameRemote: formData.modelNameRemote,
    temperature: formData.temperature,
    maxTokens: formData.maxTokens,
    genericReqType: formData.invokeType === INVOKE_TYPE_GENERIC_HTTP ? formData.genericReqType : null,
    genericReqField: formData.invokeType === INVOKE_TYPE_GENERIC_HTTP ? formData.genericReqField : null,
    genericRespPath: formData.invokeType === INVOKE_TYPE_GENERIC_HTTP ? formData.genericRespPath : null,
    genericHeadersJson: formData.invokeType === INVOKE_TYPE_GENERIC_HTTP ? formData.genericHeadersJson : null,
    enabled: formData.enabled,
    supportsImageChat: formData.supportsImageChat,
    sortNo: formData.sortNo,
    remark: formData.remark
  }
}

const buildUpdatePayload = () => {
  const payload = {
    modelName: formData.modelName,
    invokeType: formData.invokeType,
    provider: formData.provider,
    apiBaseUrl: formData.apiBaseUrl,
    modelNameRemote: formData.modelNameRemote,
    temperature: formData.temperature,
    maxTokens: formData.maxTokens,
    genericReqType: formData.invokeType === INVOKE_TYPE_GENERIC_HTTP ? formData.genericReqType : null,
    genericReqField: formData.invokeType === INVOKE_TYPE_GENERIC_HTTP ? formData.genericReqField : null,
    genericRespPath: formData.invokeType === INVOKE_TYPE_GENERIC_HTTP ? formData.genericRespPath : null,
    genericHeadersJson: formData.invokeType === INVOKE_TYPE_GENERIC_HTTP ? formData.genericHeadersJson : null,
    enabled: formData.enabled,
    supportsImageChat: formData.supportsImageChat,
    sortNo: formData.sortNo,
    remark: formData.remark
  }

  if (formData.apiKey && formData.apiKey.trim()) {
    payload.apiKey = formData.apiKey
  }

  return payload
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createModelConfig(buildCreatePayload())
      ElMessage.success('新增成功')
    } else {
      await updateModelConfig(currentId.value, buildUpdatePayload())
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    await loadModelList()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  if (!canEditModelConfig.value) {
    ElMessage.warning('当前账号无删除权限')
    return
  }

  const confirmed = await ElMessageBox.confirm(
    `确认删除模型「${row.modelName}」吗？`,
    '提示',
    {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    }
  ).catch(() => false)

  if (!confirmed) {
    return
  }

  await deleteModelConfig(row.id)
  ElMessage.success('删除成功')
  await loadModelList()
}

loadModelList()
</script>

<style scoped>
.model-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow: hidden;
}

.toolbar-card {
  flex-shrink: 0;
  min-height: 76px;
  border-radius: 16px;
  border: 1px solid rgba(133, 160, 230, 0.25);
  background: rgba(255, 255, 255, 0.86);
  box-shadow: 0 10px 24px rgba(123, 150, 219, 0.16);
  padding: 14px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-left {
  min-width: 0;
}

.toolbar-title {
  margin: 0;
  color: #243456;
  font-size: 18px;
  font-weight: 700;
}

.toolbar-desc {
  margin: 8px 0 0;
  color: rgba(84, 105, 152, 0.76);
  font-size: 13px;
}

.table-card {
  flex: 1;
  min-height: 0;
  border-radius: 16px;
  border: 1px solid rgba(133, 160, 230, 0.24);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 12px 24px rgba(123, 150, 219, 0.16);
  padding: 14px;
  overflow: hidden;
}

.table-card :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(126, 159, 241, 0.1);
  --el-table-border-color: rgba(133, 160, 230, 0.2);
  --el-table-row-hover-bg-color: rgba(126, 159, 241, 0.12);
  --el-table-text-color: #31446d;
  --el-table-header-text-color: #3d5384;
}

.config-form {
  max-height: 62vh;
  overflow: auto;
  padding-right: 4px;
}

.full-width {
  width: 100%;
}

.test-result-card {
  margin-top: 10px;
  padding: 14px 16px;
  border-radius: 12px;
  border: 1px solid rgba(133, 160, 230, 0.24);
  background: rgba(255, 255, 255, 0.82);
}

.test-result-title {
  color: #243456;
  font-size: 15px;
  font-weight: 700;
}

.test-result-meta {
  margin-top: 8px;
  display: flex;
  gap: 16px;
  color: #4b5f96;
  font-size: 13px;
}

.test-result-question,
.test-result-answer {
  margin-top: 8px;
  color: #34456f;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
