<template>
  <div class="prompt-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">系统提示词管理</h2>
        <p class="toolbar-desc">对齐后端 /admin/prompts/system，仅 SUPER_ADMIN 可访问，支持 CHAT / RAG / BOTH 场景</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增系统提示词</el-button>
    </div>

    <div class="table-card" v-loading="tableLoading">
      <el-table :data="promptList" height="100%">
        <el-table-column prop="promptCode" label="提示词编码" min-width="170" />
        <el-table-column prop="promptName" label="提示词名称" min-width="160" />
        <el-table-column label="场景" min-width="100">
          <template #default="{ row }">
            <el-tag :type="getSceneTag(row.scene)">{{ row.scene }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="默认" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.isDefault === 1 ? 'warning' : 'info'">
              {{ row.isDefault === 1 ? '默认' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" min-width="90" />
        <el-table-column prop="version" label="版本号" min-width="90" />
        <el-table-column label="提示词内容" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.content }}
          </template>
        </el-table-column>
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" fixed="right" min-width="170">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增系统提示词' : '编辑系统提示词'"
      width="900px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        class="prompt-form"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="提示词编码" prop="promptCode">
              <el-input
                v-model="formData.promptCode"
                placeholder="例如 SYS_CHAT_DEFAULT"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提示词名称" prop="promptName">
              <el-input v-model="formData.promptName" placeholder="例如 系统提示词-对话默认" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="适用场景" prop="scene">
              <el-select v-model="formData.scene" class="full-width">
                <el-option label="CHAT" value="CHAT" />
                <el-option label="RAG" value="RAG" />
                <el-option label="BOTH" value="BOTH" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="状态" prop="enabled">
              <el-select v-model="formData.enabled" class="full-width">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="是否默认" prop="isDefault">
              <el-select v-model="formData.isDefault" class="full-width">
                <el-option label="否" :value="0" />
                <el-option label="是" :value="1" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-input-number v-model="formData.priority" :min="0" :step="1" class="full-width" />
            </el-form-item>
          </el-col>
          <el-col :span="12"></el-col>
        </el-row>

        <el-form-item label="提示词内容" prop="content">
          <el-input
            v-model="formData.content"
            type="textarea"
            :rows="8"
            placeholder="请输入 system role 提示词正文"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="可选备注信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ submitLoading ? '提交中...' : '确认' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createSystemPrompt,
  deleteSystemPrompt,
  getSystemPromptDetail,
  getSystemPromptList,
  updateSystemPrompt
} from '@/api/prompt'

const formRef = ref()
const tableLoading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const dialogMode = ref('create')
const currentId = ref(null)
const promptList = ref([])

const createDefaultFormData = () => ({
  promptCode: '',
  promptName: '',
  scene: 'CHAT',
  content: '',
  enabled: 1,
  priority: 100,
  isDefault: 0,
  remark: ''
})

const formData = reactive(createDefaultFormData())

const formRules = {
  promptCode: [
    { required: true, message: '请输入提示词编码', trigger: 'blur' }
  ],
  promptName: [
    { required: true, message: '请输入提示词名称', trigger: 'blur' }
  ],
  scene: [
    { required: true, message: '请选择适用场景', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入提示词内容', trigger: 'blur' }
  ],
  isDefault: [
    {
      validator: (rule, value, callback) => {
        if (value === 1 && formData.enabled === 0) {
          callback(new Error('禁用状态的提示词不能设置为默认'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const resetFormData = () => {
  Object.assign(formData, createDefaultFormData())
}

const getSceneTag = (scene) => {
  if (scene === 'CHAT') return 'success'
  if (scene === 'RAG') return 'warning'
  if (scene === 'BOTH') return 'primary'
  return 'info'
}

const loadPromptList = async () => {
  tableLoading.value = true
  try {
    const res = await getSystemPromptList()
    promptList.value = res.data || []
  } finally {
    tableLoading.value = false
  }
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  currentId.value = null
  resetFormData()
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  dialogMode.value = 'edit'
  currentId.value = row.id
  dialogVisible.value = true
  resetFormData()

  const res = await getSystemPromptDetail(row.id)
  Object.assign(formData, {
    ...createDefaultFormData(),
    ...res.data
  })
}

const buildCreatePayload = () => {
  return {
    promptCode: formData.promptCode,
    promptName: formData.promptName,
    scene: formData.scene,
    content: formData.content,
    enabled: formData.enabled,
    priority: formData.priority,
    isDefault: formData.isDefault,
    remark: formData.remark
  }
}

const buildUpdatePayload = () => {
  return {
    promptName: formData.promptName,
    scene: formData.scene,
    content: formData.content,
    enabled: formData.enabled,
    priority: formData.priority,
    isDefault: formData.isDefault,
    remark: formData.remark
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createSystemPrompt(buildCreatePayload())
      ElMessage.success('新增成功')
    } else {
      await updateSystemPrompt(currentId.value, buildUpdatePayload())
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    await loadPromptList()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  const confirmed = await ElMessageBox.confirm(
    `确认删除提示词「${row.promptName}」吗？`,
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

  await deleteSystemPrompt(row.id)
  ElMessage.success('删除成功')
  await loadPromptList()
}

loadPromptList()
</script>

<style scoped>
.prompt-page {
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

.prompt-form {
  max-height: 62vh;
  overflow: auto;
  padding-right: 4px;
}

.full-width {
  width: 100%;
}
</style>
