import type { RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/workorder',
    name: 'workorder',
    redirect: '/workorder/list',
    meta: {
      title: '生产管理',
      icon: 'ant-design:schedule-outlined',
    },
    children: [
      {
        path: 'list',
        name: 'workorder-list',
        meta: {
          title: '生产工单',
          icon: 'ant-design:unordered-list-outlined',
          perms: ['workorder'],
        },
        component: () => import('@/views/workorder/list/index.vue'),
      },
      {
        path: 'config',
        name: 'workorder-config',
        meta: {
          title: 'BOM 与工序模板',
          icon: 'ant-design:build-outlined',
          perms: ['workorder'],
        },
        component: () => import('@/views/workorder/config/index.vue'),
      },
    ],
  },
];

export default routes;
