import type { RouteRecordRaw } from 'vue-router';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/system',
    name: 'system',
    redirect: '/system/user',
    meta: {
      title: '系统管理',
      icon: 'ant-design:setting-outlined',
    },
    children: [
      {
        path: 'user',
        name: 'system-user',
        meta: {
          title: '用户管理',
          icon: 'ant-design:team-outlined',
          perms: ['system:user'],
        },
        component: () => import('@/views/system/user/index.vue'),
      },
      {
        path: 'role',
        name: 'system-role',
        meta: {
          title: '角色权限',
          icon: 'ant-design:key-outlined',
          perms: ['system:role'],
        },
        component: () => import('@/views/system/role/index.vue'),
      },
      {
        path: 'dict',
        name: 'system-dict',
        meta: {
          title: '数据字典',
          icon: 'ant-design:book-outlined',
          perms: ['system:dict'],
        },
        component: () => import('@/views/system/dict/index.vue'),
      },
      {
        path: 'log',
        name: 'system-log',
        meta: {
          title: '操作日志',
          icon: 'ant-design:file-text-outlined',
          perms: ['system:log'],
        },
        component: () => import('@/views/system/log/index.vue'),
      },
    ],
  },
];

export default routes;
