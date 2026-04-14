<template>
  <el-container class="layout-root">
    <el-aside class="layout-aside" :width="isCollapsed ? '72px' : '248px'">
      <Sidebar :collapsed="isCollapsed" />
    </el-aside>

    <el-container class="layout-body">
      <el-header class="layout-header">
        <HeaderBar v-model:collapsed="isCollapsed" />
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import Sidebar from './components/Sidebar.vue'
import HeaderBar from './components/HeaderBar.vue'

const isCollapsed = ref(false)
</script>

<style scoped>
.layout-root {
  width: 100vw;
  height: 100vh;
  background:
    radial-gradient(circle at 10% 10%, rgba(93, 143, 255, 0.2), transparent 38%),
    radial-gradient(circle at 90% 20%, rgba(164, 132, 255, 0.18), transparent 40%),
    linear-gradient(180deg, #f7f9ff 0%, #edf3ff 55%, #f8fbff 100%);
}

.layout-aside {
  height: 100vh;
  overflow: hidden;
  border-right: 1px solid rgba(137, 161, 234, 0.3);
  background:
    linear-gradient(180deg, rgba(245, 249, 255, 0.96) 0%, rgba(234, 241, 255, 0.95) 100%);
  box-shadow: 12px 0 38px rgba(111, 138, 211, 0.15);
  transition: width 180ms ease;
}

.layout-body {
  height: 100vh;
  overflow: hidden;
}

.layout-header {
  height: 64px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  border-bottom: 1px solid rgba(134, 159, 229, 0.28);
  background:
    linear-gradient(180deg, rgba(250, 252, 255, 0.95) 0%, rgba(239, 245, 255, 0.88) 100%);
  backdrop-filter: blur(14px);
}

.layout-main {
  height: calc(100vh - 64px);
  overflow: hidden;
  padding: 18px;
}
</style>
