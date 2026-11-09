<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { useOrderStore } from '@/stores/order'
import type { WorkOrderStatus } from '@/types'
import { formatNum, statusTagType, statusText } from '@/utils/display'

interface TabItem {
  label: string
  status?: WorkOrderStatus
}

const tabs: TabItem[] = [
  { label: '全部' },
  { label: '待生产', status: 0 },
  { label: '生产中', status: 1 },
  { label: '待质检', status: 2 },
  { label: '已完成', status: 3 }
]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const orderStore = useOrderStore()

const keyword = ref('')
const activeTab = ref(0)
const refreshing = ref(false)

const filterVisible = ref(false)
const baseFilter = ref('')
const sortBy = ref<'time' | 'progress'>('time')
const draftBase = ref('')
const draftSort = ref<'time' | 'progress'>('time')

const lookupVisible = ref(false)
const lookupTitle = ref('扫码报工')
const lookupNo = ref('')

const hasFilter = computed(() => !!baseFilter.value || sortBy.value !== 'time')

const baseOptions = computed(() => {
  const sites = Array.from(new Set(orderStore.orders.map((order) => order.baseSite)))
  return ['全部', ...sites]
})

const filteredOrders = computed(() => {
  const status = tabs[activeTab.value]?.status
  let list = orderStore.orders
  if (status !== undefined) {
    list = list.filter((order) => order.workStatus === status)
  }
  if (baseFilter.value) {
    list = list.filter((order) => order.baseSite === baseFilter.value)
  }
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    list = list.filter((order) =>
      [order.workOrderNo, order.materialName, order.materialCode].some((value) =>
        (value ?? '').toLowerCase().includes(kw)
      )
    )
  }
  if (sortBy.value === 'progress') {
    list = [...list].sort((a, b) => progressOf(b) - progressOf(a))
  }
  return list
})

const emptyText = computed(() => {
  if (keyword.value.trim()) return '没有匹配的工单'
  const label = tabs[activeTab.value]?.label
  return activeTab.value === 0 ? '暂无工单' : `暂无${label ?? ''}工单`
})

function countOf(status?: WorkOrderStatus) {
  if (status === undefined) return orderStore.orders.length
  return orderStore.orders.filter((order) => order.workStatus === status).length
}

function progressOf(order: { totalProcess: number; doneProcess: number }) {
  if (!order.totalProcess) return 0
  return Math.round((order.doneProcess / order.totalProcess) * 100)
}

function syncTabFromQuery() {
  const raw = Number(route.query.tab)
  if (Number.isInteger(raw) && raw >= 0 && raw < tabs.length) {
    activeTab.value = raw
  }
}

function openFilter() {
  draftBase.value = baseFilter.value
  draftSort.value = sortBy.value
  filterVisible.value = true
}

function applyFilter() {
  baseFilter.value = draftBase.value === '全部' ? '' : draftBase.value
  sortBy.value = draftSort.value
  filterVisible.value = false
}

function resetFilter() {
  draftBase.value = '全部'
  draftSort.value = 'time'
}

function openLookup(title: string) {
  lookupTitle.value = title
  lookupNo.value = ''
  lookupVisible.value = true
}

function onLookup() {
  const no = lookupNo.value.trim().toUpperCase()
  if (!no) {
    showToast('请输入工单号')
    return
  }
  const hit = orderStore.orders.find((order) =>
    order.workOrderNo.toUpperCase().includes(no)
  )
  if (!hit) {
    showToast('未找到匹配的工单')
    return
  }
  lookupVisible.value = false
  router.push(`/order/${hit.id}`)
}

function gotoPending() {
  const hit =
    orderStore.orders.find((order) => order.workStatus === 1) ??
    orderStore.orders.find((order) => order.workStatus === 0)
  if (!hit) {
    showToast('暂无待报工工单')
    return
  }
  router.push(`/order/${hit.id}`)
}

function goDetail(orderId: number) {
  router.push(`/order/${orderId}`)
}

async function onRefresh() {
  refreshing.value = true
  await orderStore.load()
  refreshing.value = false
}

function onLogout() {
  showConfirmDialog({
    title: '退出登录',
    message: `确定退出当前账号（${userStore.realName || userStore.userInfo?.username || ''}）吗？`,
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
  syncTabFromQuery()
  orderStore.load()
})

watch(
  () => route.query.tab,
  () => syncTabFromQuery()
)
</script>

<template>
  <div class="page with-tabbar list-page">
    <header class="brand-hero list-hero">
      <div class="hero-bar">
        <span class="hero-title">工序报工</span>
        <span class="logout-text" @click="onLogout">退出</span>
      </div>

      <div class="search-row">
        <div class="search-box">
          <van-icon name="search" class="search-icon" />
          <input v-model="keyword" class="search-input" placeholder="搜索工单号 / 产品名称" />
          <van-icon v-if="keyword" name="clear" class="search-clear" @click="keyword = ''" />
        </div>
        <button class="filter-btn" :class="{ active: hasFilter }" @click="openFilter">
          <van-icon name="filter-o" />
          筛选
        </button>
      </div>

      <div class="quick-row">
        <button class="quick-btn" @click="openLookup('扫码报工')">
          <van-icon name="scan" />
          扫码报工
        </button>
        <button class="quick-btn" @click="gotoPending">
          <van-icon name="photograph" />
          拍照报工
        </button>
      </div>
    </header>

    <section class="overview-card">
      <div class="overview-head">
        <span class="overview-title">报工概览</span>
        <span class="overview-sub">累计数据</span>
      </div>
      <div class="overview-stats">
        <div class="os">
          <b>{{ orderStore.stats.done }}</b>
          <span>完成工单</span>
        </div>
        <div class="os">
          <b>{{ formatNum(orderStore.stats.good) }}</b>
          <span>完成数量</span>
        </div>
        <div class="os">
          <b>{{ orderStore.stats.rate }}%</b>
          <span>良品率</span>
        </div>
      </div>
    </section>

    <van-tabs v-model:active="activeTab" sticky class="status-tabs">
      <van-tab v-for="tab in tabs" :key="tab.label">
        <template #title>
          <span class="tab-title">
            {{ tab.label }}
            <span v-if="countOf(tab.status)" class="tab-count">{{ countOf(tab.status) }}</span>
          </span>
        </template>
      </van-tab>
    </van-tabs>

    <van-pull-refresh v-model="refreshing" class="pull-wrap" @refresh="onRefresh">
      <div class="order-list">
        <div v-if="orderStore.loading && !orderStore.orders.length" class="page-loading">
          <van-loading size="24" />
          正在加载工单...
        </div>

        <van-empty
          v-else-if="filteredOrders.length === 0"
          :description="emptyText"
          class="empty-wrap"
        />

        <template v-else>
          <div
            v-for="order in filteredOrders"
            :key="order.id"
            class="order-card"
            @click="goDetail(order.id)"
          >
            <div class="card-head">
              <span class="wo-no">{{ order.workOrderNo }}</span>
              <van-tag round :type="statusTagType(order.workStatus)">
                {{ statusText(order.workStatus) }}
              </van-tag>
            </div>

            <div class="product-name">{{ order.materialName }}</div>

            <div class="card-meta">
              <van-tag plain type="primary">{{ order.baseSite }}</van-tag>
              <span class="meta-text">{{ order.materialCode }}</span>
              <span class="meta-text">批次 {{ order.productBatch }}</span>
            </div>

            <div class="progress-block">
              <div class="progress-head">
                <span>进度 {{ order.doneProcess }}/{{ order.totalProcess }}</span>
                <span class="progress-num">{{ progressOf(order) }}%</span>
              </div>
              <van-progress
                :percentage="progressOf(order)"
                :show-pivot="false"
                stroke-width="4"
                color="#2258e8"
                track-color="#e8eefb"
              />
            </div>

            <div class="card-foot">
              <div class="foot-meta">
                <span>良品 <b class="good">{{ formatNum(order.goodTotal) }}</b></span>
                <span>不良 <b class="bad">{{ formatNum(order.badTotal) }}</b></span>
              </div>
              <span class="report-link" @click.stop="goDetail(order.id)">
                立即报工
                <van-icon name="arrow" />
              </span>
            </div>
          </div>
        </template>

        <p v-if="!orderStore.loading && filteredOrders.length" class="list-footer">
          共 {{ filteredOrders.length }} 条工单 · 下拉可刷新
        </p>
      </div>
    </van-pull-refresh>

    <van-popup v-model:show="filterVisible" position="bottom" round class="filter-popup">
      <div class="popup-bar">
        <span class="popup-title">筛选工单</span>
        <van-icon name="cross" class="popup-close" @click="filterVisible = false" />
      </div>

      <div class="filter-block">
        <div class="filter-label">生产基地</div>
        <div class="filter-chips">
          <span
            v-for="base in baseOptions"
            :key="base"
            class="chip"
            :class="{ on: draftBase === base }"
            @click="draftBase = base"
          >
            {{ base }}
          </span>
        </div>
      </div>

      <div class="filter-block">
        <div class="filter-label">排序方式</div>
        <div class="filter-chips">
          <span
            class="chip"
            :class="{ on: draftSort === 'time' }"
            @click="draftSort = 'time'"
          >
            按创建时间
          </span>
          <span
            class="chip"
            :class="{ on: draftSort === 'progress' }"
            @click="draftSort = 'progress'"
          >
            按工序进度
          </span>
        </div>
      </div>

      <div class="filter-actions">
        <van-button block round plain @click="resetFilter">重置</van-button>
        <van-button block round type="primary" @click="applyFilter">确定</van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="lookupVisible" round position="center" class="lookup-popup">
      <div class="lookup-title">{{ lookupTitle }}</div>
      <p class="lookup-sub">输入工单号快速定位并进入报工</p>
      <van-field
        v-model="lookupNo"
        class="lookup-input"
        placeholder="例如 WO-20261109-001"
        :border="false"
        clearable
      />
      <div class="lookup-actions">
        <van-button block round plain @click="lookupVisible = false">取消</van-button>
        <van-button block round type="primary" @click="onLookup">定位工单</van-button>
      </div>
    </van-popup>
  </div>
</template>

<style scoped>
.list-page {
  background: #f2f5fb;
}

.list-hero {
  padding: calc(14px + env(safe-area-inset-top)) 18px 22px;
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

.logout-text {
  padding: 4px 0;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.82);
}

.search-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 38px;
  padding: 0 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
}

.search-icon {
  font-size: 16px;
  color: #9aa3b5;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 13px;
  color: #1d2333;
}

.search-input::placeholder {
  color: #a9b2c4;
}

.search-clear {
  font-size: 14px;
  color: #b3bbcb;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 38px;
  padding: 0 14px;
  border: none;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  font-size: 13px;
}

.filter-btn.active {
  background: #fff;
  color: #2258e8;
}

.quick-row {
  display: flex;
  gap: 10px;
  margin-top: 12px;
}

.quick-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 40px;
  border: none;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.24);
  color: #fff;
  font-size: 13px;
}

.quick-btn:active {
  background: rgba(255, 255, 255, 0.26);
}

.overview-card {
  margin: -12px 14px 0;
  padding: 16px;
  border-radius: 16px;
  color: #fff;
  background: linear-gradient(135deg, #1736c8 0%, #2358e6 55%, #3d86f8 100%);
  box-shadow: 0 12px 26px rgba(23, 54, 200, 0.24);
  position: relative;
  z-index: 1;
}

.overview-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}

.overview-title {
  font-size: 13px;
  font-weight: 600;
}

.overview-sub {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.7);
}

.overview-stats {
  display: flex;
  margin-top: 12px;
}

.overview-stats .os {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.overview-stats .os + .os {
  border-left: 1px solid rgba(255, 255, 255, 0.18);
  padding-left: 14px;
}

.overview-stats .os b {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.1;
}

.overview-stats .os span {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.75);
}

.status-tabs {
  margin-top: 14px;
}

.status-tabs :deep(.van-tabs__wrap) {
  background: #f2f5fb;
  box-shadow: none;
}

.status-tabs :deep(.van-tabs__nav) {
  background: #f2f5fb;
}

.status-tabs :deep(.van-tab) {
  color: #7b869c;
  font-size: 13px;
}

.status-tabs :deep(.van-tab--active) {
  color: #2258e8;
  font-weight: 600;
}

.status-tabs :deep(.van-tabs__line) {
  width: 20px;
  height: 3px;
  border-radius: 2px;
  background: #2258e8;
}

.pull-wrap {
  flex: 1;
  background: #f2f5fb;
}

.order-list {
  padding: 12px 14px 8px;
  min-height: 40vh;
}

.order-card {
  background: #fff;
  border-radius: 16px;
  padding: 14px 16px 12px;
  margin-bottom: 12px;
  box-shadow: 0 8px 24px rgba(23, 54, 200, 0.07);
  transition: transform 0.15s ease;
}

.order-card:active {
  transform: scale(0.985);
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.wo-no {
  font-size: 12px;
  font-weight: 600;
  color: #7b869c;
  letter-spacing: 0.3px;
}

.product-name {
  margin-top: 8px;
  font-size: 17px;
  font-weight: 600;
  color: #1d2333;
  line-height: 1.3;
}

.card-meta {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-text {
  font-size: 11px;
  color: #9aa3b5;
}

.progress-block {
  margin-top: 14px;
  padding: 10px 12px;
  background: #f4f6fb;
  border-radius: 12px;
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11px;
  color: #8a93a6;
  margin-bottom: 8px;
}

.progress-num {
  font-weight: 600;
  color: #2258e8;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.foot-meta {
  display: flex;
  gap: 14px;
  font-size: 11px;
  color: #9aa3b5;
}

.foot-meta b {
  font-size: 14px;
  color: #333c4e;
}

.foot-meta b.good {
  color: #17a673;
}

.foot-meta b.bad {
  color: #e35d6a;
}

.report-link {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  font-weight: 600;
  color: #2258e8;
}

.report-link :deep(.van-icon) {
  font-size: 12px;
}

.empty-wrap {
  padding: 40px 0;
}

.list-footer {
  margin: 6px 0 0;
  text-align: center;
  font-size: 11px;
  color: #b3bbcb;
}

.filter-popup {
  padding: 6px 20px calc(20px + env(safe-area-inset-bottom));
}

.popup-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0 4px;
}

.popup-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2333;
}

.popup-close {
  font-size: 18px;
  color: #b3bbcb;
  padding: 4px;
}

.filter-block {
  margin-top: 18px;
}

.filter-label {
  font-size: 13px;
  font-weight: 600;
  color: #333c4e;
  margin-bottom: 10px;
}

.filter-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  padding: 7px 16px;
  border-radius: 18px;
  background: #f4f6fb;
  color: #5a6478;
  font-size: 12px;
  border: 1px solid transparent;
}

.chip.on {
  background: rgba(34, 88, 232, 0.1);
  border-color: #2258e8;
  color: #2258e8;
  font-weight: 600;
}

.filter-actions {
  display: flex;
  gap: 12px;
  margin-top: 26px;
}

.filter-actions :deep(.van-button) {
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
}

.lookup-popup {
  width: 300px;
  padding: 22px 20px 20px;
}

.lookup-title {
  font-size: 17px;
  font-weight: 600;
  color: #1d2333;
  text-align: center;
}

.lookup-sub {
  margin: 6px 0 16px;
  font-size: 12px;
  color: #9aa3b5;
  text-align: center;
}

.lookup-input {
  background: #f4f6fb;
  border-radius: 10px;
  padding: 10px 12px;
}

.lookup-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.lookup-actions :deep(.van-button) {
  height: 42px;
  border-radius: 10px;
  font-size: 14px;
}
</style>
