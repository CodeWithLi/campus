import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/layout/index.vue'),
      redirect: '/login',
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
          path: '/token-billing-logs',
          name: 'token-billing-logs',
          component: () => import('@/views/token-billing-log/index.vue'),
          meta: {
            title: 'Token 消耗日志',
            requiresAdmin: true
          }
        },
        {
          path: '/orders',
          name: 'orders',
          component: () => import('@/views/order/index.vue'),
          meta: {
            title: '订单管理',
            requiresAdmin: true
          }
        },
        {
          path: '/posts',
          name: 'posts',
          component: () => import('@/views/post/index.vue'),
          meta: {
            title: '帖子管理',
            requiresAdmin: true
          }
        },
        {
          path: '/post-reports',
          name: 'post-reports',
          component: () => import('@/views/post-report/index.vue'),
          meta: {
            title: '帖子举报管理',
            requiresAdmin: true
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
        },
        {
          path: '/kb-rag-debug',
          name: 'kb-rag-debug',
          component: () => import('@/views/kb-rag-debug/index.vue'),
          meta: {
            title: 'RAG 调试',
            requiresAdmin: true
          }
        },
        {
          path: '/guides',
          name: 'guides',
          component: () => import('@/views/guide/index.vue'),
          meta: {
            title: '办事指南',
            requiresAdmin: true
          }
        },
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

  if (to.meta?.requiresSuperAdmin && !userStore.isSuperAdmin) {
    return '/dashboard'
  }

  if (to.meta?.requiresAdmin && !userStore.isAdmin) {
    return '/dashboard'
  }

  return true
})

export default router
