import { ref } from 'vue';
import { defineStore } from 'pinia';
import type { RouteRecordRaw } from 'vue-router';
import { store } from '@/store';
import { accessToken, currentUserId, onUnauthorized, registerPermsRefresher } from '@/store/runtime';
import { authApi, type UserInfo } from '@/api/stockwork';
import { resetRouter } from '@/router/reset';
import { generateDynamicRoutes } from '@/router/helper/routeHelper';

const allMenus = () => import('@/router/routes/modules').then((m) => m.default);

export const useUserStore = defineStore(
  'user',
  () => {
    const token = accessToken;
    const perms = ref<string[]>([]);
    const menus = ref<RouteRecordRaw[]>([]);
    const userInfo = ref<Partial<UserInfo>>({});

    const sortMenus = (menus: RouteRecordRaw[] = []) => {
      return menus
        .filter((n) => {
          const flag = !n.meta?.hideInMenu;
          if (flag && n.children?.length) {
            n.children = sortMenus(n.children);
          }
          return flag;
        })
        .sort((a, b) => ~~Number(a.meta?.orderNo) - ~~Number(b.meta?.orderNo));
    };

    /** 按角色权限标识过滤菜单 */
    const filterByPerms = (routes: RouteRecordRaw[]): RouteRecordRaw[] => {
      return routes
        .map((route) => {
          if (!route.children?.length) return { ...route } as RouteRecordRaw;
          return { ...route, children: filterByPerms(route.children) } as RouteRecordRaw;
        })
        .filter((route) => {
          const permList = (route.meta as Recordable<any> | undefined)?.perms as
            | string[]
            | undefined;
          const permOk = !permList?.length || permList.some((p) => perms.value.includes(p));
          const childOk = !route.children || route.children.length > 0;
          return permOk && childOk && !route.meta?.hideInMenu;
        });
    };

    /** 清空登录态(token、userInfo...) */
    const clearLoginStatus = () => {
      token.value = '';
      currentUserId.value = undefined;
      perms.value = [];
      menus.value = [];
      userInfo.value = {};
      resetRouter();
      setTimeout(() => {
        localStorage.clear();
      });
    };
    /** 登录成功保存token */
    const setToken = (_token: string) => {
      token.value = _token;
    };
    /** 登录 */
    const login = async (params: { username: string; password: string }) => {
      try {
        const data = await authApi.login(params);
        setToken(data.token);
        userInfo.value = data.userInfo;
        await afterLogin();
      } catch (error) {
        return Promise.reject(error);
      }
    };
    /** 登录成功之后, 获取用户信息以及生成权限路由 */
    const afterLogin = async () => {
      try {
        const profile = userInfo.value.id ? userInfo.value : await authApi.me();
        userInfo.value = profile;
        currentUserId.value = profile.id;
        await fetchPermsAndMenus();
      } catch (error) {
        return Promise.reject(error);
      }
    };
    /** 获取权限及菜单 */
    const fetchPermsAndMenus = async () => {
      const profile = userInfo.value.id ? userInfo.value : await authApi.me();
      userInfo.value = profile;
      perms.value = (profile.perms || '')
        .split(',')
        .map((n) => n.trim())
        .filter(Boolean);

      // 注册静态业务路由（不依赖后端菜单表）
      const routes = generateDynamicRoutes([]);
      const modules = routes.length ? routes : await allMenus();
      menus.value = sortMenus(filterByPerms(modules));
    };
    /** 登出 */
    const logout = async () => {
      try {
        await authApi.logout();
      } finally {
        clearLoginStatus();
      }
    };

    return {
      token,
      perms,
      menus,
      userInfo,
      login,
      afterLogin,
      logout,
      clearLoginStatus,
      fetchPermsAndMenus,
    };
  },
  {
    persist: {
      pick: ['token'],
    },
  },
);

// 在组件setup函数外使用
export function useUserStoreWithOut() {
  return useUserStore(store);
}

onUnauthorized(() => {
  useUserStore(store).clearLoginStatus();
});

registerPermsRefresher(() => {
  void useUserStore(store).fetchPermsAndMenus();
});
