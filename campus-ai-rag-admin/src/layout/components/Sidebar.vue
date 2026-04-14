<template>
  <div class="sidebar-root">
    <div class="sidebar-brand" :class="{ collapsed }">
      <div class="brand-mark">
        <span class="brand-core"></span>
      </div>
      <div v-if="!collapsed" class="brand-text">
        <div class="brand-title">AI Admin</div>
        <div class="brand-sub">Campus RAG</div>
      </div>
    </div>

    <el-menu
      class="sidebar-menu"
      :default-active="activePath"
      :collapse="collapsed"
      :collapse-transition="false"
      router
    >
      <el-menu-item index="/dashboard">
        <span class="menu-dot"></span>
        <span>控制台</span>
      </el-menu-item>
      <el-menu-item index="/models">
        <span class="menu-dot"></span>
        <span>模型配置</span>
      </el-menu-item>
      <el-menu-item v-if="userStore.isSuperAdmin" index="/prompts/system">
        <span class="menu-dot"></span>
        <span>系统提示词</span>
      </el-menu-item>
      <el-menu-item v-if="userStore.isSuperAdmin" index="/admin-users">
        <span class="menu-dot"></span>
        <span>管理员管理</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

defineProps({
  collapsed: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const userStore = useUserStore()
const activePath = computed(() => route.path)
</script>

<style scoped>
.sidebar-root {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-brand {
  height: 64px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(137, 161, 234, 0.26);
}

.sidebar-brand.collapsed {
  justify-content: center;
  padding: 0;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(121, 152, 236, 0.34);
  display: grid;
  place-items: center;
  box-shadow: 0 0 24px rgba(75, 119, 255, 0.18);
}

.brand-core {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, #68a6ff 0%, #8f63ff 100%);
  box-shadow: 0 0 14px rgba(121, 137, 255, 0.85);
  animation: pulse 1.8s ease-in-out infinite;
}

.brand-text {
  min-width: 0;
}

.brand-title {
  color: #1f2a44;
  font-weight: 700;
  font-size: 14px;
  letter-spacing: 0.3px;
}

.brand-sub {
  margin-top: 2px;
  color: rgba(72, 92, 138, 0.72);
  font-size: 12px;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 10px 8px;
}

.sidebar-menu :deep(.el-menu-item) {
  height: 44px;
  border-radius: 12px;
  margin: 4px 0;
  color: #42527b;
}

.sidebar-menu :deep(.el-menu-item:hover) {
  background: rgba(116, 154, 255, 0.16);
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #24408b;
  background: linear-gradient(135deg, rgba(134, 174, 255, 0.32), rgba(176, 149, 255, 0.2));
  box-shadow: 0 0 18px rgba(125, 161, 245, 0.2);
}

.menu-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 10px;
  background: rgba(143, 179, 255, 0.55);
  box-shadow: 0 0 10px rgba(130, 173, 255, 0.55);
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1);
    opacity: 0.8;
  }
  50% {
    transform: scale(1.35);
    opacity: 1;
  }
}
</style>
