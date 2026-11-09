import type { RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/dashboard',
    name: 'dashboard',
    redirect: '/dashboard/welcome',
    meta: {
      title: '工作台',
      icon: 'ant-design:dashboard-outlined',
    },
    children: [
      {
        path: 'welcome',
        name: 'dashboard-welcome',
        meta: {
          title: '数据概览',
          icon: 'ant-design:home-filled',
          perms: ['dashboard'],
        },
        component: () => import('@/views/dashboard/welcome/index.vue'),
      },
    ],
  },
];

export default routes;
