<template>
  <div class="kb-category-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">知识库分类管理</h2>
        <p class="toolbar-desc">对齐后端 /admin/kb/categories，支持分类增删改查</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增分类</el-button>
    </div>

    <div class="table-card" v-loading="tableLoading">
      <el-table :data="categoryList" height="100%">
        <el-table-column prop="id" label="ID" min-width="70" />
        <el-table-column prop="categoryCode" label="分类编码" min-width="140" />
        <el-table-column prop="categoryName" label="分类名称" min-width="160" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新增分类' : '编辑分类'"
      width="680px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="分类编码" prop="categoryCode">
              <el-input v-model="formData.categoryCode" :disabled="dialogMode === 'edit'" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类名称" prop="categoryName">
              <el-input v-model="formData.categoryName" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态" prop="enabled">
              <el-select v-model="formData.enabled" class="full-width">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sortNo">
              <el-input-number v-model="formData.sortNo" :min="0" :step="1" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" />
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
  createKbCategory,
  deleteKbCategory,
  getKbCategoryDetail,
  getKbCategoryList,
  updateKbCategory
} from '@/api/kb'

const tableLoading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const dialogMode = ref('create')
const currentId = ref(null)
const formRef = ref()
const categoryList = ref([])

const createDefaultFormData = () => ({
  categoryCode: '',
  categoryName: '',
  enabled: 1,
  sortNo: 100,
  remark: ''
})

const formData = reactive(createDefaultFormData())

const formRules = {
  categoryCode: [{ required: true, message: '请输入分类编码', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const resetFormData = () => {
  Object.assign(formData, createDefaultFormData())
}

const loadCategoryList = async () => {
  tableLoading.value = true
  try {
    const res = await getKbCategoryList()
    categoryList.value = res.data || []
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
  resetFormData()
  dialogVisible.value = true

  const res = await getKbCategoryDetail(row.id)
  Object.assign(formData, {
    ...createDefaultFormData(),
    ...res.data
  })
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createKbCategory({
        categoryCode: formData.categoryCode,
        categoryName: formData.categoryName,
        enabled: formData.enabled,
        sortNo: formData.sortNo,
        remark: formData.remark
      })
      ElMessage.success('新增成功')
    } else {
      await updateKbCategory(currentId.value, {
        categoryName: formData.categoryName,
        enabled: formData.enabled,
        sortNo: formData.sortNo,
        remark: formData.remark
      })
      ElMessage.success('更新成功')
    }
    dialogVisible.value = false
    await loadCategoryList()
  } finally {
    submitLoading.value = false
  }
}

const handleDelete = async (row) => {
  const confirmed = await ElMessageBox.confirm(`确认删除分类「${row.categoryName}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  await deleteKbCategory(row.id)
  ElMessage.success('删除成功')
  await loadCategoryList()
}

loadCategoryList()
</script>

<style scoped>
.kb-category-page {
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

.toolbar-left { min-width: 0; }
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

.full-width { width: 100%; }
</style>
