<template>
  <div class="wx-user-page">
    <div class="toolbar-card">
      <div class="toolbar-left">
        <h2 class="toolbar-title">用户管理</h2>
        <p class="toolbar-desc">对齐后端 /admin/wx-users，管理员与超级管理员可查询/更新/冻结/解冻/调整算力点</p>
      </div>
      <el-button @click="loadUserList">刷新</el-button>
    </div>

    <div class="table-card" v-loading="tableLoading">
      <el-table :data="userList" height="100%">
        <el-table-column prop="id" label="ID" min-width="70" />
        <el-table-column label="头像" min-width="90">
          <template #default="{ row }">
            <div class="avatar-wrap">
              <img v-if="row.avatarUrl" class="avatar-img" :src="row.avatarUrl" alt="avatar" />
              <div v-else class="avatar-fallback">{{ (row.nickname || row.username || 'U').slice(0, 1).toUpperCase() }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="nickname" label="昵称" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '正常' : '冻结' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="registerSource" label="注册来源" min-width="120" />
        <el-table-column prop="powerPoints" label="算力点" min-width="90" />
        <el-table-column prop="lastLoginAt" label="最后登录" min-width="180" />
        <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
        <el-table-column label="操作" fixed="right" min-width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button link type="warning" @click="openPowerPointsDialog(row)">改算力点</el-button>
            <el-button
              v-if="row.status === 1"
              link
              type="danger"
              @click="handleFreeze(row)"
            >
              冻结
            </el-button>
            <el-button
              v-else
              link
              type="success"
              @click="handleUnfreeze(row)"
            >
              解冻
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="editDialogVisible" title="编辑用户" width="720px" destroy-on-close>
      <el-form ref="editFormRef" :model="editFormData" :rules="editFormRules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名">
              <el-input v-model="editFormData.username" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="注册来源">
              <el-input v-model="editFormData.registerSource" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="editFormData.nickname" placeholder="可为空" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="editFormData.email" placeholder="可为空，例如 test@example.com" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="editFormData.status" class="full-width">
                <el-option label="正常" :value="1" />
                <el-option label="冻结" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="算力点" prop="powerPoints">
              <el-input-number v-model="editFormData.powerPoints" :min="0" :step="1" class="full-width" />
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

    <el-dialog v-model="ppDialogVisible" title="修改用户算力点" width="560px" destroy-on-close>
      <el-form ref="ppFormRef" :model="ppFormData" :rules="ppFormRules" label-width="120px">
        <el-form-item label="用户名">
          <el-input v-model="ppFormData.username" disabled />
        </el-form-item>
        <el-form-item label="新的算力点" prop="powerPoints">
          <el-input-number v-model="ppFormData.powerPoints" :min="0" :step="1" class="full-width" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="ppDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ppLoading" @click="handleSubmitPowerPoints">
          {{ ppLoading ? '提交中...' : '确认' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  freezeWxUser,
  getWxUserDetail,
  getWxUserList,
  unfreezeWxUser,
  updateWxUser,
  updateWxUserPowerPoints
} from '@/api/wx-user'

const tableLoading = ref(false)
const userList = ref([])

const editDialogVisible = ref(false)
const submitLoading = ref(false)
const editFormRef = ref()
const currentId = ref(null)

const editFormData = reactive({
  username: '',
  registerSource: '',
  nickname: '',
  email: '',
  status: 1,
  powerPoints: 0
})

const editFormRules = {
  email: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          callback()
          return
        }
        const normalized = String(value).trim()
        if (!normalized) {
          callback()
          return
        }
        if (!normalized.includes('@') || !normalized.includes('.')) {
          callback(new Error('邮箱格式不正确'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  powerPoints: [
    {
      validator: (rule, value, callback) => {
        if (value == null) {
          callback()
          return
        }
        if (value < 0) {
          callback(new Error('算力点不能小于0'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ]
}

const ppDialogVisible = ref(false)
const ppLoading = ref(false)
const ppFormRef = ref()
const ppFormData = reactive({
  id: null,
  username: '',
  powerPoints: 0
})
const ppFormRules = {
  powerPoints: [{ required: true, message: '请输入算力点', trigger: 'change' }]
}

const loadUserList = async () => {
  tableLoading.value = true
  try {
    const res = await getWxUserList()
    userList.value = res.data || []
  } finally {
    tableLoading.value = false
  }
}

const openEditDialog = async (row) => {
  currentId.value = row.id
  const res = await getWxUserDetail(row.id)
  const data = res.data || {}

  editFormData.username = data.username || ''
  editFormData.registerSource = data.registerSource || ''
  editFormData.nickname = data.nickname || ''
  editFormData.email = data.email || ''
  editFormData.status = data.status ?? 1
  editFormData.powerPoints = data.powerPoints ?? 0

  editDialogVisible.value = true
}

const handleSubmitEdit = async () => {
  const valid = await editFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true
  try {
    await updateWxUser(currentId.value, {
      nickname: editFormData.nickname,
      email: editFormData.email,
      status: editFormData.status,
      powerPoints: editFormData.powerPoints
    })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    await loadUserList()
  } finally {
    submitLoading.value = false
  }
}

const openPowerPointsDialog = (row) => {
  ppFormData.id = row.id
  ppFormData.username = row.username
  ppFormData.powerPoints = row.powerPoints ?? 0
  ppDialogVisible.value = true
}

const handleSubmitPowerPoints = async () => {
  const valid = await ppFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  ppLoading.value = true
  try {
    await updateWxUserPowerPoints(ppFormData.id, {
      powerPoints: ppFormData.powerPoints
    })
    ElMessage.success('算力点已更新')
    ppDialogVisible.value = false
    await loadUserList()
  } finally {
    ppLoading.value = false
  }
}

const handleFreeze = async (row) => {
  const confirmed = await ElMessageBox.confirm(`确认冻结用户「${row.username}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '冻结',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  await freezeWxUser(row.id)
  ElMessage.success('已冻结')
  await loadUserList()
}

const handleUnfreeze = async (row) => {
  const confirmed = await ElMessageBox.confirm(`确认解冻用户「${row.username}」吗？`, '提示', {
    type: 'warning',
    confirmButtonText: '解冻',
    cancelButtonText: '取消'
  }).catch(() => false)
  if (!confirmed) return

  await unfreezeWxUser(row.id)
  ElMessage.success('已解冻')
  await loadUserList()
}

loadUserList()
</script>

<style scoped>
.wx-user-page {
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

.avatar-wrap {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(133, 160, 230, 0.26);
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 6px 14px rgba(123, 150, 219, 0.12);
  display: grid;
  place-items: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-fallback {
  font-weight: 700;
  color: #4962a8;
  font-size: 13px;
}

.full-width {
  width: 100%;
}
</style>

