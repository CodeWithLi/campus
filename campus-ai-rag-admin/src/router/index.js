import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/layout/index.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: '/dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: {
            title: '控制台'
          }
        },
        {
          path: '/models',
          name: 'models',
          component: () => import('@/views/model/index.vue'),
          meta: {
            title: '模型配置'
          }
        },
        {
          path: '/prompts/system',
          name: 'system-prompts',
          component: () => import('@/views/prompt/index.vue'),
          meta: {
            title: '系统提示词',
            requiresSuperAdmin: true
          }
        },
        {
          path: '/admin-users',
          name: 'admin-users',
          component: () => import('@/views/admin-user/index.vue'),
          meta: {
            title: '管理员管理',
            requiresSuperAdmin: true
          }
        },
        {
          path: '/wx-users',
          name: 'wx-users',
          component: () => import('@/views/wx-user/index.vue'),
          meta: {
            title: '用户管理',
            requiresAdmin: true
          }
        },
        {
          path: '/kb-categories',
          name: 'kb-categories',
          component: () => import('@/views/kb-category/index.vue'),
          meta: {
            title: '知识库分类',
            requiresAdmin: true
          }
        },
        {
          path: '/kb-documents',
          name: 'kb-documents',
          component: () => import('@/views/kb-document/index.vue'),
          meta: {
            title: '知识库文档',
            requiresAdmin: true
          }
        }
      ]
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/index.vue'),
      meta: {
        title: '后台登录'
      }
    }
  ],
})

router.beforeEach((to) => {
  const userStore = useUserStore()

  if (to.path !== '/login' && !userStore.token) {
    return '/login'
  }

  if (to.path === '/login' && userStore.token) {
    return '/dashboard'
  }

  if (to.meta?.requiresSuperAdmin && !userStore.isSuperAdmin) {
    return '/dashboard'
  }

  if (to.meta?.requiresAdmin && !userStore.isAdmin) {
    return '/dashboard'
  }

  return true
})

export default router
