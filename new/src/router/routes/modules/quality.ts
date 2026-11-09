import type { RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/reportCheck',
    name: 'reportCheck',
    redirect: '/reportCheck/todo',
    meta: {
      title: '质量管控',
      icon: 'ant-design:safety-certificate-outlined',
    },
    children: [
      {
        path: 'todo',
        name: 'reportCheck-todo',
        meta: {
          title: '工单质检',
          icon: 'ant-design:audit-outlined',
          perms: ['reportCheck'],
        },
        component: () => import('@/views/reportCheck/todo/index.vue'),
      },
      {
        path: 'stat',
        name: 'quality-stat',
        meta: {
          title: '不良品统计',
          icon: 'ant-design:bar-chart-outlined',
          perms: ['quality'],
        },
        component: () => import('@/views/quality/stat/index.vue'),
      },
    ],
  },
];

export default routes;
