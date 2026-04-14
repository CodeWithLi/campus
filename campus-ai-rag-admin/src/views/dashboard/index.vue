<template>
  <div class="dashboard-page">
    <div class="dashboard-card">
      <h1 class="dashboard-title">Campus AI RAG Admin</h1>
      <p class="dashboard-desc">登录已完成，后续我可以继续为你补后台布局和业务页面。</p>
    </div>
  </div>
</template>

<script setup>
</script>

<style scoped>
.dashboard-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow: hidden;
  border-radius: 18px;
  background:
    radial-gradient(circle at top, rgba(115, 160, 255, 0.22), transparent 35%),
    radial-gradient(circle at bottom right, rgba(179, 147, 255, 0.2), transparent 30%),
    #f5f9ff;
}

.dashboard-card {
  width: min(680px, 100%);
  padding: 48px;
  border: 1px solid rgba(134, 161, 232, 0.38);
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 20px 50px rgba(119, 145, 210, 0.2);
  text-align: center;
}

.dashboard-title {
  margin: 0 0 16px;
  color: #243456;
  font-size: 36px;
  font-weight: 700;
}

.dashboard-desc {
  margin: 0;
  color: rgba(83, 102, 145, 0.82);
  font-size: 16px;
}
</style>
