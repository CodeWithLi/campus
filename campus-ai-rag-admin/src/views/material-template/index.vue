<template>
  <div class="page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">材料模板管理</h2>
        <p class="toolbar-desc">为办事指南步骤快速套用材料清单</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增模板</el-button>
    </div>

    <div class="table-card" v-loading="tableLoading">
      <el-table :data="list" height="100%">
        <el-table-column prop="id" label="ID" min-width="80" />
        <el-table-column prop="templateName" label="模板名称" min-width="200" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">{{ row.enabled === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortNo" label="排序" min-width="90" />
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" fixed="right" min-width="170">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="dialogMode === 'create' ? '新增模板' : '编辑模板'" width="720px" destroy-on-close>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="formData.templateName" :disabled="dialogMode === 'edit'" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="formData.enabled" class="full-width">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="formData.sortNo" :min="0" class="full-width" />
        </el-form-item>

        <el-form-item label="材料项" required>
          <div class="items-wrap">
            <div class="tags">
              <el-tag
                v-for="(it, idx) in formData.items"
                :key="`${it}-${idx}`"
                closable
                @close="removeItem(idx)"
                type="info"
              >
                {{ it }}
              </el-tag>
              <span v-if="!formData.items.length" class="empty">暂无材料项</span>
            </div>
            <div class="row">
              <el-input v-model="itemInput" placeholder="输入材料后回车或点击添加" @keyup.enter="addItem" />
              <el-button type="primary" plain @click="addItem">添加</el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createMaterialTemplate,
  deleteMaterialTemplate,
  getMaterialTemplateDetail,
  getMaterialTemplateList,
  updateMaterialTemplate
} from '@/api/material-template'

const tableLoading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const currentId = ref(null)
const list = ref([])
const formRef = ref()
const itemInput = ref('')

const defaultForm = () => ({
  templateName: '',
  enabled: 1,
  sortNo: 100,
  remark: '',
  items: []
})

const formData = reactive(defaultForm())

const formRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

const loadList = async () => {
  tableLoading.value = true
  try {
    const res = await getMaterialTemplateList()
    list.value = res.data || []
  } finally {
    tableLoading.value = false
  }
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  currentId.value = null
  Object.assign(formData, defaultForm())
  itemInput.value = ''
  dialogVisible.value = true
}

const openEditDialog = async (row) => {
  dialogMode.value = 'edit'
  currentId.value = row.id
  Object.assign(formData, defaultForm())
  itemInput.value = ''
  dialogVisible.value = true
  const res = await getMaterialTemplateDetail(row.id)
  const detail = res.data || {}
  let items = []
  try {
    items = Array.isArray(JSON.parse(detail.itemsJson || '[]')) ? JSON.parse(detail.itemsJson || '[]') : []
  } catch (e) {
    items = []
  }
  Object.assign(formData, { ...detail, items })
}

const addItem = () => {
  const value = String(itemInput.value || '').trim()
  if (!value) return
  if (formData.items.includes(value)) {
    itemInput.value = ''
    return
  }
  formData.items = [...formData.items, value]
  itemInput.value = ''
}

const removeItem = (idx) => {
  const next = [...(formData.items || [])]
  next.splice(idx, 1)
  formData.items = next
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!formData.items.length) {
    ElMessage.warning('请至少添加一个材料项')
    return
  }
  submitLoading.value = true
  try {
    const payload = {
      templateName: formData.templateName,
      enabled: formData.enabled,
      sortNo: formData.sortNo,
      remark: formData.remark,
      itemsJson: JSON.stringify(formData.items)
    }
    if (dialogMode.value === 'create') {
      await createMaterialTemplate(payload)
      ElMessage.success('新增成功')
    } else {
      await updateMaterialTemplate(currentId.value, payload)
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    await loadList()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  const confirmed = await ElMessageBox.confirm(`确认删除模板「${row.templateName}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return
  await deleteMaterialTemplate(row.id)
  ElMessage.success('删除成功')
  await loadList()
}

loadList()
</script>

<style scoped>
.page {
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
.toolbar-title { margin: 0; color: #243456; font-size: 18px; font-weight: 700; }
.toolbar-desc { margin: 8px 0 0; color: rgba(84, 105, 152, 0.76); font-size: 13px; }
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
}
.full-width { width: 100%; }
.items-wrap {
  width: 100%;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid rgba(133, 160, 230, 0.2);
  background: rgba(245, 248, 255, 0.55);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.tags { display: flex; flex-wrap: wrap; gap: 6px; min-height: 28px; }
.empty { font-size: 12px; color: #9aa8c3; }
.row { display: flex; gap: 8px; }
</style>

