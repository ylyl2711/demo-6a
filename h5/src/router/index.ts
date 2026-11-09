import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      name: 'orders',
      component: () => import('@/views/OrderListView.vue'),
      meta: { tabbar: true }
    },
    {
      path: '/notification',
      name: 'notification',
      component: () => import('@/views/NotificationView.vue'),
      meta: { tabbar: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { tabbar: true }
    },
    {
      path: '/order/:id',
      name: 'order-detail',
      component: () => import('@/views/OrderDetailView.vue')
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ]
})

router.beforeEach((to) => {
  const user = useUserStore()
  if (to.meta.public) {
    if (to.path === '/login' && user.token) {
      return '/'
    }
    return true
  }
  if (!user.token) {
    return { path: '/login' }
  }
  return true
})

export default router
