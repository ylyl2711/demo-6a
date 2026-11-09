<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <div class="brand-mark">软</div>
        <div>
          <h1>双基地仓储库存与生产报工管理系统</h1>
          <p>宁波汽车软轴软管有限公司 · 宁波 / 合肥</p>
        </div>
      </div>
      <a-form layout="vertical" :model="formModel" @submit.prevent="handleSubmit">
        <a-form-item label="用户名">
          <a-input v-model:value="formModel.username" size="large" placeholder="请输入用户名">
            <template #prefix><Icon icon="ant-design:user-outlined" /></template>
          </a-input>
        </a-form-item>
        <a-form-item label="密码">
          <a-input-password
            v-model:value="formModel.password"
            size="large"
            placeholder="请输入密码"
            autocomplete="new-password"
          >
            <template #prefix><Icon icon="ant-design:lock-outlined" /></template>
          </a-input-password>
        </a-form-item>
        <a-button type="primary" html-type="submit" size="large" :loading="loading" block>
          登 录
        </a-button>
      </a-form>
      <div class="login-tip">
        <span>演示账号：admin / 123456（管理员）</span>
        <span>manager / 123456（业务人员）</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { message } from 'ant-design-vue';
  import { Icon } from '@/components/basic/icon';
  import { useUserStore } from '@/store/modules/user';
  import { to } from '@/utils/awaitTo';

  const route = useRoute();
  const router = useRouter();
  const userStore = useUserStore();

  const loading = ref(false);
  const formModel = ref({
    username: 'manager',
    password: '123456',
  });

  const handleSubmit = async () => {
    const { username, password } = formModel.value;
    if (!username.trim() || !password.trim()) {
      return message.warning('用户名或密码不能为空');
    }
    loading.value = true;
    const [err] = await to(userStore.login({ username, password }));
    loading.value = false;
    if (err) {
      message.error(err.message || '登录失败');
      return;
    }
    if (userStore.userInfo.roleCode === 'OPERATOR') {
      message.info('车间操作工请使用 H5 报工端（手机端）进行报工');
      await userStore.logout();
      return;
    }
    message.success('登录成功');
    router.replace((route.query.redirect as string) || '/');
  };
</script>

<style lang="less" scoped>
  .login-page {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100vw;
    height: 100vh;
    background: linear-gradient(135deg, #fdf3f7 0%, #f7f9ff 55%, #eef5ff 100%);

    .login-card {
      width: 420px;
      padding: 36px 40px 28px;
      background: #fff;
      border-radius: 14px;
      border: 1px solid #f5dfe8;
      box-shadow: 0 12px 36px rgba(87, 112, 152, 0.12);
    }

    .login-brand {
      display: flex;
      gap: 14px;
      align-items: flex-start;
      margin-bottom: 28px;

      .brand-mark {
        flex: none;
        width: 44px;
        height: 44px;
        border-radius: 12px;
        background: #fde7ef;
        color: #d94f7b;
        font-size: 20px;
        font-weight: 600;
        line-height: 44px;
        text-align: center;
      }

      h1 {
        margin: 0 0 6px;
        font-size: 18px;
        font-weight: 600;
        color: #1f2d3d;
      }

      p {
        margin: 0;
        font-size: 12px;
        color: #8c93a1;
      }
    }

    .login-tip {
      display: flex;
      flex-direction: column;
      gap: 2px;
      margin-top: 18px;
      font-size: 12px;
      color: #98a0ae;
    }
  }
</style>
