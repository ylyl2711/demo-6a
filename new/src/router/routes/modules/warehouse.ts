import type { RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/warehouse',
    name: 'warehouse',
    redirect: '/warehouse/material',
    meta: {
      title: '仓储管理',
      icon: 'ant-design:database-outlined',
    },
    children: [
      {
        path: 'material',
        name: 'warehouse-material',
        meta: {
          title: '物料档案',
          icon: 'ant-design:profile-outlined',
          perms: ['warehouse'],
        },
        component: () => import('@/views/warehouse/material/index.vue'),
      },
    ],
  },
];

export default routes;
