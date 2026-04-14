<template>
  <div class="login-page">
    <div class="aurora aurora-one"></div>
    <div class="aurora aurora-two"></div>
    <div class="grid-layer"></div>
    <div class="orbit orbit-one"></div>
    <div class="orbit orbit-two"></div>

    <div class="particle-group">
      <span
        v-for="particle in particleList"
        :key="particle.id"
        class="particle"
        :style="particle.style"
      ></span>
    </div>

    <div class="login-panel">
      <div class="brand-section">
        <div class="brand-badge">
          <span class="brand-dot"></span>
          AI Admin
        </div>
        <h1 class="brand-title">后台管理系统</h1>
      </div>

      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        label-position="top"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="loginForm.username"
            size="large"
            placeholder="请输入账号"
            clearable
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            size="large"
            placeholder="请输入密码"
            show-password
            clearable
          />
        </el-form-item>

        <el-button
          class="submit-button"
          type="primary"
          size="large"
          :loading="submitLoading"
          @click="handleLogin"
        >
          {{ submitLoading ? '登录中...' : '进入后台' }}
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const submitLoading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const particleList = [
  { id: 1, style: { left: '10%', top: '16%', animationDelay: '0s', animationDuration: '9s' } },
  { id: 2, style: { left: '18%', top: '72%', animationDelay: '1.4s', animationDuration: '11s' } },
  { id: 3, style: { left: '28%', top: '36%', animationDelay: '3.2s', animationDuration: '10s' } },
  { id: 4, style: { left: '64%', top: '22%', animationDelay: '2.1s', animationDuration: '12s' } },
  { id: 5, style: { left: '72%', top: '68%', animationDelay: '0.8s', animationDuration: '8.8s' } },
  { id: 6, style: { left: '82%', top: '40%', animationDelay: '4.2s', animationDuration: '10.6s' } },
  { id: 7, style: { left: '88%', top: '78%', animationDelay: '2.6s', animationDuration: '9.8s' } }
]

const handleLogin = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }

  submitLoading.value = true

  try {
    const res = await login({
      username: loginForm.username.trim(),
      password: loginForm.password
    })

    userStore.setToken(res.data?.token)
    userStore.setTokenName(res.data?.tokenName)

    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  height: 100vh;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 20% 20%, rgba(117, 171, 255, 0.26), transparent 28%),
    radial-gradient(circle at 80% 15%, rgba(185, 158, 255, 0.24), transparent 30%),
    radial-gradient(circle at 50% 80%, rgba(132, 222, 255, 0.18), transparent 34%),
    linear-gradient(135deg, #f9fbff 0%, #eff5ff 48%, #f7faff 100%);
}

.login-page::before,
.login-page::after {
  content: '';
  position: absolute;
  inset: -20%;
  pointer-events: none;
}

.login-page::before {
  background:
    linear-gradient(90deg, transparent 0%, rgba(122, 163, 255, 0.16) 50%, transparent 100%);
  transform: rotate(12deg);
  animation: sweepGlow 8s linear infinite;
}

.login-page::after {
  background:
    radial-gradient(circle, rgba(90, 120, 184, 0.08) 0, transparent 8px);
  background-size: 160px 160px;
  opacity: 0.18;
}

.aurora {
  position: absolute;
  width: 520px;
  height: 520px;
  border-radius: 50%;
  filter: blur(40px);
  opacity: 0.55;
  pointer-events: none;
}

.aurora-one {
  top: -140px;
  left: -100px;
  background: radial-gradient(circle, rgba(102, 160, 255, 0.42) 0%, rgba(53, 122, 255, 0) 70%);
  animation: floatAurora 12s ease-in-out infinite;
}

.aurora-two {
  right: -120px;
  bottom: -180px;
  background: radial-gradient(circle, rgba(171, 133, 255, 0.38) 0%, rgba(138, 87, 255, 0) 70%);
  animation: floatAurora 14s ease-in-out infinite reverse;
}

.grid-layer {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(108, 141, 224, 0.2) 1px, transparent 1px),
    linear-gradient(90deg, rgba(108, 141, 224, 0.2) 1px, transparent 1px);
  background-size: 52px 52px;
  mask-image: linear-gradient(to bottom, transparent, rgba(0, 0, 0, 0.9), transparent);
  transform: perspective(900px) rotateX(72deg) translateY(28%);
  transform-origin: center top;
  opacity: 0.35;
  animation: gridMove 10s linear infinite;
}

.orbit {
  position: absolute;
  border: 1px solid rgba(133, 166, 236, 0.34);
  border-radius: 50%;
  pointer-events: none;
}

.orbit-one {
  width: 380px;
  height: 380px;
  left: 8%;
  top: 14%;
  animation: spin 18s linear infinite;
}

.orbit-two {
  width: 560px;
  height: 560px;
  right: 6%;
  bottom: 8%;
  animation: spin 24s linear infinite reverse;
}

.particle-group {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: radial-gradient(circle, #7f9ae6 0%, rgba(143, 179, 255, 0.16) 70%, transparent 100%);
  box-shadow: 0 0 12px rgba(130, 173, 255, 0.45);
  animation-name: particleFloat;
  animation-timing-function: ease-in-out;
  animation-iteration-count: infinite;
}

.login-panel {
  position: relative;
  z-index: 1;
  width: min(520px, 100%);
  padding: 40px 36px 34px;
  border: 1px solid rgba(137, 164, 230, 0.35);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.9) 0%, rgba(244, 248, 255, 0.96) 100%);
  box-shadow:
    0 0 0 1px rgba(255, 255, 255, 0.6) inset,
    0 20px 46px rgba(114, 142, 212, 0.25),
    0 0 42px rgba(116, 153, 242, 0.16);
  backdrop-filter: blur(18px);
  animation: panelFloat 6s ease-in-out infinite;
}

.login-panel::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 28px;
  padding: 1px;
  background: linear-gradient(135deg, rgba(110, 165, 255, 0.55), rgba(139, 88, 255, 0.1), rgba(0, 229, 255, 0.35));
  -webkit-mask:
    linear-gradient(#fff 0 0) content-box,
    linear-gradient(#fff 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  pointer-events: none;
}

.brand-section {
  margin-bottom: 28px;
}

.brand-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 32px;
  padding: 0 14px;
  border: 1px solid rgba(118, 158, 240, 0.35);
  border-radius: 999px;
  color: #4f68a8;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.85);
  box-shadow: 0 6px 16px rgba(133, 163, 235, 0.22);
}

.brand-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #68a6ff 0%, #8f63ff 100%);
  box-shadow: 0 0 12px rgba(121, 137, 255, 0.85);
  animation: pulseDot 1.8s ease-in-out infinite;
}

.brand-title {
  margin: 18px 0 10px;
  color: #1f2a44;
  font-size: 32px;
  font-weight: 700;
  line-height: 1.3;
}

.brand-desc {
  margin: 0;
  color: rgba(209, 222, 255, 0.72);
  font-size: 14px;
  line-height: 1.7;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.login-form :deep(.el-form-item__label) {
  color: #4c6397;
  font-size: 14px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 48px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow:
    0 0 0 1px rgba(120, 150, 226, 0.24) inset,
    0 8px 18px rgba(129, 158, 224, 0.2);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow:
    0 0 0 1px rgba(102, 148, 255, 0.72) inset,
    0 0 18px rgba(81, 124, 255, 0.22);
}

.login-form :deep(.el-input__inner) {
  color: #243456;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(142, 160, 198, 0.66);
}

.submit-button {
  width: 100%;
  height: 48px;
  margin-top: 6px;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #4f84ff 0%, #7f5bff 55%, #4be1ff 100%);
  box-shadow:
    0 16px 28px rgba(65, 110, 255, 0.32),
    0 0 18px rgba(79, 132, 255, 0.28);
}

.submit-button:hover {
  transform: translateY(-1px);
  filter: brightness(1.05);
}

.submit-button:active {
  transform: translateY(0);
}

@keyframes floatAurora {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(24px, -18px, 0) scale(1.08);
  }
}

@keyframes gridMove {
  0% {
    transform: perspective(900px) rotateX(72deg) translateY(28%);
    background-position: 0 0, 0 0;
  }
  100% {
    transform: perspective(900px) rotateX(72deg) translateY(28%);
    background-position: 0 52px, 52px 0;
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes sweepGlow {
  0% {
    transform: translateX(-30%) rotate(12deg);
  }
  100% {
    transform: translateX(30%) rotate(12deg);
  }
}

@keyframes particleFloat {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(0.9);
    opacity: 0.45;
  }
  50% {
    transform: translate3d(10px, -18px, 0) scale(1.2);
    opacity: 1;
  }
}

@keyframes panelFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
  }
}

@keyframes pulseDot {
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

@media (max-width: 768px) {
  .login-page {
    padding: 16px;
  }

  .login-panel {
    padding: 28px 22px 24px;
    border-radius: 22px;
  }

  .brand-title {
    font-size: 24px;
  }

  .orbit-one,
  .orbit-two {
    display: none;
  }
}
</style>
