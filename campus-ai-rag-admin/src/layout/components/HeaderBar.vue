<template>
  <div class="header-root">
    <div class="left">
      <el-button class="collapse-btn" text @click="toggleCollapsed">
        <span class="collapse-icon" :class="{ collapsed }"></span>
      </el-button>
      <div class="breadcrumb">
        <span class="breadcrumb-title">{{ pageTitle }}</span>
        <span class="breadcrumb-sub">/ admin</span>
      </div>
    </div>

    <div class="right">
      <div class="status-pill">
        <span class="status-dot"></span>
        Online
      </div>

      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-chip">
          <div class="avatar"></div>
          <div class="user-info">
            <div class="user-name">Admin</div>
            <div class="user-role">{{ currentRoleLabel }}</div>
          </div>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:collapsed'])

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const pageTitle = computed(() => route.meta?.title || '控制台')
const currentRoleLabel = computed(() => userStore.roleCode || 'ADMIN')

const toggleCollapsed = () => {
  emit('update:collapsed', !props.collapsed)
}

const handleCommand = async (command) => {
  if (command !== 'logout') {
    return
  }

  const confirmed = await ElMessageBox.confirm('确认退出登录？', '提示', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning'
  }).catch(() => false)

  if (!confirmed) {
    return
  }

  userStore.clearUserInfo()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.header-root {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.collapse-btn {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(131, 159, 234, 0.28);
  box-shadow: 0 4px 16px rgba(125, 157, 235, 0.16);
}

.collapse-icon {
  display: inline-block;
  width: 16px;
  height: 12px;
  position: relative;
}

.collapse-icon::before,
.collapse-icon::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  height: 2px;
  border-radius: 999px;
  background: rgba(73, 98, 168, 0.86);
  box-shadow: 0 0 10px rgba(120, 160, 255, 0.15);
  transition: transform 180ms ease, top 180ms ease, width 180ms ease;
}

.collapse-icon::before {
  top: 2px;
}

.collapse-icon::after {
  top: 8px;
  width: 72%;
}

.collapse-icon.collapsed::after {
  width: 100%;
}

.breadcrumb {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.breadcrumb-title {
  color: #243456;
  font-weight: 700;
  font-size: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.breadcrumb-sub {
  color: rgba(115, 133, 176, 0.75);
  font-size: 12px;
}

.right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-pill {
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(116, 152, 236, 0.28);
  background: rgba(255, 255, 255, 0.86);
  color: #4a5f96;
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4be1ff 0%, #4f84ff 100%);
  box-shadow: 0 0 16px rgba(75, 225, 255, 0.45);
}

.user-chip {
  height: 40px;
  padding: 6px 10px 6px 8px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(122, 156, 236, 0.26);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 8px 20px rgba(126, 155, 226, 0.16);
  cursor: pointer;
  user-select: none;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(79, 132, 255, 0.9), rgba(127, 91, 255, 0.8));
  box-shadow: 0 0 16px rgba(79, 132, 255, 0.22);
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.user-name {
  color: #243456;
  font-weight: 700;
  font-size: 12px;
}

.user-role {
  margin-top: 2px;
  color: rgba(104, 124, 171, 0.76);
  font-size: 11px;
}
</style>
