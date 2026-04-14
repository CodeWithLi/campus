<template>
  <div class="admin-user-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">管理员管理</h2>
        <p class="toolbar-desc">对齐后端 /admin/users，仅 SUPER_ADMIN 可见与操作</p>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增管理员</el-button>
    </div>

    <div class="table-card" v-loading="tableLoading">
      <el-table :data="userList" height="100%">
        <el-table-column prop="id" label="ID" min-width="70" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="130" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="超级管理员" min-width="110">
          <template #default="{ row }">
            <el-tag :type="row.superAdminMarker === 'Y' ? 'warning' : 'info'">
              {{ row.superAdminMarker === 'Y' ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="roleId" label="角色ID" min-width="90" />
        <el-table-column prop="roleCode" label="角色编码" min-width="140" />
        <el-table-column prop="roleName" label="角色名称" min-width="140" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" fixed="right" min-width="230">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="warning" @click="openResetPasswordDialog(row)">重置密码</el-button>
            <el-button
              link
              type="danger"
              :disabled="row.superAdminMarker === 'Y'"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog
      v-model="editDialogVisible"
      :title="dialogMode === 'create' ? '新增管理员' : '编辑管理员'"
      width="720px"
      destroy-on-close
    >
      <el-form ref="editFormRef" :model="editFormData" :rules="editFormRules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="editFormData.username"
                placeholder="请输入用户名"
                :disabled="dialogMode === 'edit'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="editFormData.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row v-if="dialogMode === 'create'" :gutter="16">
          <el-col :span="12">
            <el-form-item label="登录密码" prop="password">
              <el-input
                v-model="editFormData.password"
                type="password"
                show-password
                placeholder="请输入登录密码"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12"></el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="editFormData.status" class="full-width">
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色ID" prop="roleId">
              <el-input-number v-model="editFormData.roleId" :min="1" :step="1" class="full-width" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitEdit">
          {{ submitLoading ? '提交中...' : '确认' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="resetPasswordDialogVisible"
      title="重置管理员密码"
      width="560px"
      destroy-on-close
    >
      <el-form ref="resetFormRef" :model="resetFormData" :rules="resetFormRules" label-width="120px">
        <el-form-item label="用户名">
          <el-input v-model="resetFormData.username" disabled />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="resetFormData.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="resetPasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetPasswordLoading" @click="handleSubmitResetPassword">
          {{ resetPasswordLoading ? '提交中...' : '确认重置' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAdminUser,
  deleteAdminUser,
  getAdminUserDetail,
  getAdminUserList,
  resetAdminUserPassword,
  updateAdminUser
} from '@/api/admin-user'

const tableLoading = ref(false)
const userList = ref([])

const dialogMode = ref('create')
const currentId = ref(null)
const editDialogVisible = ref(false)
const submitLoading = ref(false)
const editFormRef = ref()

const editFormData = reactive({
  username: '',
  password: '',
  nickname: '',
  status: 1,
  roleId: 1
})

const editFormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    {
      validator: (rule, value, callback) => {
        if (dialogMode.value !== 'create') {
          callback()
          return
        }
        if (!value || !value.trim()) {
          callback(new Error('请输入登录密码'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  roleId: [{ required: true, message: '请输入角色ID', trigger: 'change' }]
}

const resetPasswordDialogVisible = ref(false)
const resetPasswordLoading = ref(false)
const resetFormRef = ref()
const resetFormData = reactive({
  id: null,
  username: '',
  newPassword: ''
})

const resetFormRules = {
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }]
}

const resetEditForm = () => {
  editFormData.username = ''
  editFormData.password = ''
  editFormData.nickname = ''
  editFormData.status = 1
  editFormData.roleId = 1
}

const loadUserList = async () => {
  tableLoading.value = true
  try {
    const res = await getAdminUserList()
    userList.value = res.data || []
  } finally {
    tableLoading.value = false
  }
}

const openCreateDialog = () => {
  dialogMode.value = 'create'
  currentId.value = null
  resetEditForm()
  editDialogVisible.value = true
}

const openEditDialog = async (row) => {
  dialogMode.value = 'edit'
  currentId.value = row.id
  resetEditForm()
  editDialogVisible.value = true

  const res = await getAdminUserDetail(row.id)
  editFormData.username = res.data?.username || ''
  editFormData.nickname = res.data?.nickname || ''
  editFormData.status = res.data?.status ?? 1
  editFormData.roleId = res.data?.roleId ?? 1
}

const handleSubmitEdit = async () => {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    if (dialogMode.value === 'create') {
      await createAdminUser({
        username: editFormData.username,
        password: editFormData.password,
        nickname: editFormData.nickname,
        status: editFormData.status,
        roleId: editFormData.roleId
      })
      ElMessage.success('新增成功')
    } else {
      await updateAdminUser(currentId.value, {
        nickname: editFormData.nickname,
        status: editFormData.status,
        roleId: editFormData.roleId
      })
      ElMessage.success('更新成功')
    }
    editDialogVisible.value = false
    await loadUserList()
  } finally {
    submitLoading.value = false
  }
}

const openResetPasswordDialog = (row) => {
  resetFormData.id = row.id
  resetFormData.username = row.username
  resetFormData.newPassword = ''
  resetPasswordDialogVisible.value = true
}

const handleSubmitResetPassword = async () => {
  const valid = await resetFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  resetPasswordLoading.value = true
  try {
    await resetAdminUserPassword(resetFormData.id, {
      newPassword: resetFormData.newPassword
    })
    ElMessage.success('密码重置成功')
    resetPasswordDialogVisible.value = false
  } finally {
    resetPasswordLoading.value = false
  }
}

const handleDelete = async (row) => {
  const confirmed = await ElMessageBox.confirm(
    `确认删除管理员「${row.username}」吗？`,
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

  await deleteAdminUser(row.id)
  ElMessage.success('删除成功')
  await loadUserList()
}

loadUserList()
</script>

<style scoped>
.admin-user-page {
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

.full-width {
  width: 100%;
}
</style>
