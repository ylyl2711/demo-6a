import type { RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/batch',
    name: 'batch',
    redirect: '/batch/trace',
    meta: {
      title: '批次追溯',
      icon: 'ant-design:node-index-outlined',
    },
    children: [
      {
        path: 'trace',
        name: 'batch-trace',
        meta: {
          title: '正反向追溯',
          icon: 'ant-design:search-outlined',
          perms: ['batch'],
        },
        component: () => import('@/views/batch/trace/index.vue'),
      },
    ],
  },
];

export default routes;
