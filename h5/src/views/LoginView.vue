<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog, showSuccessToast, showToast } from 'vant'
import { login } from '@/api'
import { useUserStore } from '@/stores/user'

const REMEMBER_KEY = 'stockwork_h5_username'

const router = useRouter()
const userStore = useUserStore()

const remembered = localStorage.getItem(REMEMBER_KEY) || ''
const username = ref(remembered || 'op_zhang')
const password = ref('123456')
const remember = ref(!!remembered)
const loading = ref(false)

async function onSubmit() {
  if (!username.value.trim() || !password.value) {
    showToast('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const result = await login(username.value.trim(), password.value)
    if (result.userInfo.roleCode !== 'OPERATOR') {
      userStore.clear()
      showDialog({
        title: '无法登录',
        message: '请使用 Web 管理端',
        confirmButtonText: '知道了'
      })
      return
    }
    if (remember.value) {
      localStorage.setItem(REMEMBER_KEY, username.value.trim())
    } else {
      localStorage.removeItem(REMEMBER_KEY)
    }
    userStore.setAuth(result.token, result.userInfo)
    showSuccessToast('登录成功')
    await router.replace('/')
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-hero">
      <div class="hero-icon">
        <van-icon name="cluster-o" />
      </div>
      <h1>双基地仓储报工系统</h1>
      <p>宁波基地 · 合肥基地　仓储库存与生产报工一体化管理</p>
    </div>

    <van-form class="login-card" @submit="onSubmit">
      <h2 class="card-title">账号登录</h2>
      <p class="card-sub">请使用工号与密码登录车间操作端</p>

      <label class="field-label">工号 / 手机号</label>
      <van-field
        v-model="username"
        class="field-input"
        left-icon="manager-o"
        placeholder="请输入操作工工号"
        :border="false"
        clearable
      />

      <label class="field-label">登录密码</label>
      <van-field
        v-model="password"
        type="password"
        class="field-input"
        left-icon="lock"
        placeholder="请输入登录密码"
        :border="false"
      />

      <div class="field-row">
        <van-checkbox v-model="remember" shape="square" icon-size="16px">
          记住工号
        </van-checkbox>
        <span class="field-link" @click="showToast('请联系车间主管重置密码')">忘记密码？</span>
      </div>

      <van-button
        class="login-btn"
        block
        type="primary"
        native-type="submit"
        :loading="loading"
        loading-text="登录中..."
      >
        登 录
      </van-button>

      <p class="login-tip">演示账号：op_zhang / 123456</p>
    </van-form>

    <div class="login-footer">
      <p>遇到登录问题请联系车间主管 · 内线 8021</p>
      <p class="version">v1.0.0 © 2026 宁波汽车软轴软管有限公司</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 0 18px calc(18px + env(safe-area-inset-bottom));
  background: linear-gradient(150deg, #1736c8 0%, #2358e6 42%, #3d86f8 76%, #5b9bfc 100%);
  box-sizing: border-box;
}

.login-hero {
  padding: 44px 0 26px;
  text-align: center;
  color: #fff;
}

.hero-icon {
  width: 60px;
  height: 60px;
  margin: 0 auto 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.26);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  color: #fff;
}

.login-hero h1 {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}

.login-hero p {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.4px;
  color: rgba(255, 255, 255, 0.72);
}

.login-card {
  background: #fff;
  border-radius: 18px;
  padding: 22px 20px 24px;
  box-shadow: 0 18px 40px rgba(10, 32, 120, 0.24);
}

.card-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #1d2333;
}

.card-sub {
  margin: 6px 0 18px;
  font-size: 11px;
  color: #9aa3b5;
}

.field-label {
  display: block;
  margin: 0 0 8px 2px;
  font-size: 12px;
  color: #5a6478;
}

.field-input {
  margin-bottom: 14px;
  padding: 10px 14px;
  background: #f4f6fb;
  border-radius: 10px;
}

.field-input :deep(.van-field__left-icon) {
  margin-right: 8px;
  color: #9aa3b5;
  font-size: 16px;
}

.field-input :deep(.van-field__control) {
  font-size: 14px;
  color: #1d2333;
}

.field-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 18px;
}

.field-row :deep(.van-checkbox__label) {
  font-size: 12px;
  color: #5a6478;
}

.field-link {
  font-size: 12px;
  color: #2258e8;
}

.login-btn {
  height: 46px;
  border-radius: 10px;
  font-size: 16px;
  letter-spacing: 6px;
  box-shadow: 0 10px 22px rgba(34, 88, 232, 0.28);
}

.login-tip {
  margin: 14px 0 0;
  text-align: center;
  font-size: 11px;
  color: #9aa3b5;
}

.login-footer {
  margin-top: auto;
  padding-top: 26px;
  text-align: center;
  font-size: 11px;
  line-height: 1.9;
  color: rgba(255, 255, 255, 0.72);
}

.login-footer p {
  margin: 0;
}

.login-footer .version {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
}
</style>
