import type { RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/stock',
    name: 'stock',
    redirect: '/stock/inout',
    meta: {
      title: '库存管理',
      icon: 'ant-design:container-outlined',
    },
    children: [
      {
        path: 'inout',
        name: 'stock-inout',
        meta: {
          title: '出入库与台账',
          icon: 'ant-design:swap-outlined',
          perms: ['stock'],
        },
        component: () => import('@/views/stock/inout/index.vue'),
      },
      {
        path: 'stagnant',
        name: 'stock-stagnant',
        meta: {
          title: '呆滞库存预警',
          icon: 'ant-design:clock-circle-outlined',
          perms: ['stock'],
        },
        component: () => import('@/views/stock/stagnant/index.vue'),
      },
    ],
  },
];

export default routes;
