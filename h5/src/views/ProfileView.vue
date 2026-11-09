<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { useOrderStore } from '@/stores/order'

const router = useRouter()
const userStore = useUserStore()
const orderStore = useOrderStore()

const idCardVisible = ref(false)

const username = computed(() => userStore.userInfo?.username ?? '')
const roleName = computed(() => userStore.userInfo?.roleName ?? '操作工')
const avatarText = computed(() => userStore.realName.slice(0, 1) || '工')
const baseSite = computed(() => orderStore.orders[0]?.baseSite ?? '宁波基地')

const workItems = computed(() => [
  { icon: 'orders-o', label: '我的工单', value: `${orderStore.stats.total} 个`, tab: 0 },
  { icon: 'clock-o', label: '待报工工单', value: `${orderStore.stats.pending} 个`, tab: 2 },
  { icon: 'passed', label: '已完成工单', value: `${orderStore.stats.done} 个`, tab: 4 }
])

function goOrders(tab: number) {
  router.push({ path: '/', query: { tab } })
}

function goPending() {
  goOrders(2)
}

async function copyUsername() {
  const text = username.value
  try {
    await navigator.clipboard.writeText(text)
    showToast('工号已复制')
  } catch {
    showToast(`工号：${text}`)
  }
}

function onLogout() {
  showConfirmDialog({
    title: '退出登录',
    message: `确定退出当前账号（${userStore.realName || username.value}）吗？`,
    confirmButtonText: '退出',
    cancelButtonText: '取消'
  })
    .then(() => {
      orderStore.reset()
      userStore.clear()
      router.replace('/login')
    })
    .catch(() => {})
}

onMounted(() => {
  orderStore.ensureLoaded()
})
</script>

<template>
  <div class="page with-tabbar profile-page">
    <header class="brand-hero profile-hero">
      <div class="hero-bar">
        <span class="hero-title">我的</span>
        <span class="hero-tag">车间端</span>
      </div>

      <div class="user-row">
        <div class="avatar">{{ avatarText }}</div>
        <div class="user-main">
          <div class="user-name">
            {{ userStore.realName || '操作工' }}
            <van-icon name="passed" class="verified" />
          </div>
          <div class="user-sub">工号：{{ username || '—' }}</div>
        </div>
        <van-tag round class="role-tag">{{ roleName }}</van-tag>
      </div>

      <div class="user-meta">
        <span><van-icon name="cluster-o" /> {{ baseSite }}</span>
        <span><van-icon name="shield-o" /> 生产报工权限</span>
      </div>
    </header>

    <section class="stat-card">
      <div class="stat">
        <b>{{ orderStore.stats.total }}</b>
        <span>累计工单</span>
      </div>
      <div class="stat">
        <b>{{ orderStore.stats.rate }}%</b>
        <span>良品率</span>
      </div>
      <div class="stat">
        <b>{{ orderStore.stats.done }}</b>
        <span>已完工</span>
      </div>
    </section>

    <div class="quick-row">
      <button class="quick" @click="idCardVisible = true">
        <van-icon name="qr" class="quick-icon" />
        <div class="quick-text">
          <b>我的工号</b>
          <span>扫码添加同事</span>
        </div>
        <van-icon name="arrow" class="quick-arrow" />
      </button>
      <button class="quick" @click="goPending">
        <van-icon name="scan" class="quick-icon" />
        <div class="quick-text">
          <b>待报工工单</b>
          <span>{{ orderStore.stats.pending }} 条待处理</span>
        </div>
        <van-icon name="arrow" class="quick-arrow" />
      </button>
    </div>

    <div class="group">
      <div class="group-title">工作</div>
      <div class="group-card">
        <div
          v-for="item in workItems"
          :key="item.label"
          class="cell"
          @click="goOrders(item.tab)"
        >
          <van-icon :name="item.icon" class="cell-icon" />
          <span class="cell-label">{{ item.label }}</span>
          <span class="cell-value">{{ item.value }}</span>
          <van-icon name="arrow" class="cell-arrow" />
        </div>
      </div>
    </div>

    <div class="group">
      <div class="group-title">账户</div>
      <div class="group-card">
        <div class="cell" @click="router.push('/notification')">
          <van-icon name="bell" class="cell-icon" />
          <span class="cell-label">消息通知</span>
          <span class="cell-value">
            <van-tag v-if="orderStore.unreadCount" round type="danger">
              {{ orderStore.unreadCount }} 条未读
            </van-tag>
            <template v-else>全部已读</template>
          </span>
          <van-icon name="arrow" class="cell-arrow" />
        </div>
        <div class="cell" @click="onLogout">
          <van-icon name="sign" class="cell-icon danger" />
          <span class="cell-label">退出登录</span>
          <van-icon name="arrow" class="cell-arrow" />
        </div>
      </div>
    </div>

    <p class="version">v1.0.0 · 宁波汽车软轴软管有限公司</p>

    <van-popup v-model:show="idCardVisible" round position="center" class="id-popup">
      <div class="id-title">我的工号</div>
      <p class="id-sub">向同事出示工号，便于工单指派</p>
      <div class="id-code">{{ username || '—' }}</div>
      <div class="id-name">{{ userStore.realName }} · {{ roleName }}</div>
      <van-button block round type="primary" @click="copyUsername">复制工号</van-button>
    </van-popup>
  </div>
</template>

<style scoped>
.profile-page {
  background: #f2f5fb;
}

.profile-hero {
  padding: calc(14px + env(safe-area-inset-top)) 18px 34px;
  border-radius: 0 0 22px 22px;
}

.hero-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-title {
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.hero-tag {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 11px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
}

.avatar {
  width: 54px;
  height: 54px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.32);
  color: #fff;
  font-size: 22px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-main {
  flex: 1;
  min-width: 0;
}

.user-name {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 19px;
  font-weight: 600;
  color: #fff;
}

.verified {
  font-size: 14px;
  color: #7ff0c4;
}

.user-sub {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.75);
}

.role-tag {
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
}

.user-meta {
  display: flex;
  gap: 18px;
  margin-top: 16px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.78);
}

.user-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.stat-card {
  display: flex;
  margin: -20px 14px 0;
  padding: 16px 0;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 26px rgba(23, 54, 200, 0.1);
  position: relative;
  z-index: 1;
}

.stat {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat + .stat {
  border-left: 1px solid #eef1f8;
}

.stat b {
  font-size: 20px;
  font-weight: 600;
  color: #1d2333;
}

.stat span {
  font-size: 10px;
  color: #9aa3b5;
}

.quick-row {
  display: flex;
  gap: 12px;
  margin: 14px 14px 0;
}

.quick {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border: none;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 6px 18px rgba(23, 54, 200, 0.06);
  text-align: left;
}

.quick-icon {
  font-size: 22px;
  color: #2258e8;
  flex-shrink: 0;
}

.quick-text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.quick-text b {
  font-size: 13px;
  color: #1d2333;
}

.quick-text span {
  font-size: 10px;
  color: #9aa3b5;
}

.quick-arrow {
  font-size: 12px;
  color: #c4ccdb;
}

.group {
  margin-top: 18px;
}

.group-title {
  padding: 0 20px 8px;
  font-size: 12px;
  color: #8a93a6;
}

.group-card {
  margin: 0 14px;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(23, 54, 200, 0.06);
}

.cell {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
}

.cell + .cell {
  border-top: 1px solid #f3f5fa;
}

.cell:active {
  background: #fafbfe;
}

.cell-icon {
  font-size: 18px;
  color: #2258e8;
}

.cell-icon.danger {
  color: #e35d6a;
}

.cell-label {
  flex: 1;
  font-size: 14px;
  color: #1d2333;
}

.cell-value {
  font-size: 12px;
  color: #9aa3b5;
}

.cell-arrow {
  font-size: 13px;
  color: #c4ccdb;
}

.version {
  margin: 22px 0 8px;
  text-align: center;
  font-size: 10px;
  color: #b3bbcb;
}

.id-popup {
  width: 290px;
  padding: 24px 22px 22px;
  text-align: center;
}

.id-title {
  font-size: 17px;
  font-weight: 600;
  color: #1d2333;
}

.id-sub {
  margin: 6px 0 18px;
  font-size: 12px;
  color: #9aa3b5;
}

.id-code {
  padding: 16px 0;
  border-radius: 12px;
  background: #f4f6fb;
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 2px;
  color: #2258e8;
}

.id-name {
  margin: 12px 0 18px;
  font-size: 12px;
  color: #7b869c;
}

.id-popup :deep(.van-button) {
  height: 42px;
  border-radius: 10px;
  font-size: 14px;
}
</style>
