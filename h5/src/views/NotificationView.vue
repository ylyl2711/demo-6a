<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showConfirmDialog, showToast } from 'vant'
import { useOrderStore } from '@/stores/order'
import type { AppMessage, MessageType } from '@/types'
import { relativeTime } from '@/utils/message'

type FilterKey = 'all' | 'unread' | MessageType

const router = useRouter()
const orderStore = useOrderStore()

const activeFilter = ref<FilterKey>('all')

const filters = computed<{ key: FilterKey; label: string; count: number }[]>(() => {
  const all = orderStore.visibleMessages
  return [
    { key: 'all', label: '全部', count: all.length },
    { key: 'unread', label: '未读', count: orderStore.unreadCount },
    { key: 'order', label: '工单', count: all.filter((msg) => msg.type === 'order').length },
    { key: 'alert', label: '预警', count: all.filter((msg) => msg.type === 'alert').length },
    { key: 'system', label: '系统', count: all.filter((msg) => msg.type === 'system').length }
  ]
})

const messageList = computed(() => {
  const all = orderStore.visibleMessages
  if (activeFilter.value === 'all') return all
  if (activeFilter.value === 'unread') return all.filter((msg) => !orderStore.isRead(msg.id))
  return all.filter((msg) => msg.type === activeFilter.value)
})

const ICONS: Record<MessageType, string> = {
  order: 'orders-o',
  alert: 'warning-o',
  system: 'info-o'
}

function openMessage(msg: AppMessage) {
  orderStore.markRead(msg.id)
  if (msg.orderId) {
    router.push(`/order/${msg.orderId}`)
  }
}

function onMarkAllRead() {
  if (!orderStore.unreadCount) {
    showToast('暂无未读消息')
    return
  }
  orderStore.markAllRead()
  showToast('已全部标记为已读')
}

function onClear() {
  if (!orderStore.visibleMessages.length) {
    showToast('暂无消息可清空')
    return
  }
  showConfirmDialog({
    title: '清空消息',
    message: '清空后将不再展示当前消息列表，确认清空吗？',
    confirmButtonText: '清空',
    cancelButtonText: '取消'
  })
    .then(() => {
      orderStore.clearMessages()
      showToast('消息已清空')
    })
    .catch(() => {})
}

onMounted(() => {
  orderStore.ensureLoaded()
})
</script>

<template>
  <div class="page with-tabbar notif-page">
    <header class="brand-hero notif-hero">
      <div class="hero-bar">
        <span class="hero-title">消息通知</span>
        <span class="read-all" @click="onMarkAllRead">全部已读</span>
      </div>

      <div class="notif-actions">
        <button class="action-btn" @click="onMarkAllRead">
          <van-icon name="passed" />
          标记已读
        </button>
        <button class="action-btn" @click="onClear">
          <van-icon name="delete-o" />
          清空消息
        </button>
      </div>
    </header>

    <div class="filter-tabs">
      <div
        v-for="item in filters"
        :key="item.key"
        class="filter-tab"
        :class="{ on: activeFilter === item.key }"
        @click="activeFilter = item.key"
      >
        {{ item.label }}
        <span v-if="item.count" class="filter-count">{{ item.count }}</span>
      </div>
    </div>

    <div class="msg-list">
      <div v-if="orderStore.loading && !orderStore.messages.length" class="page-loading">
        <van-loading size="24" />
        正在加载消息...
      </div>

      <van-empty
        v-else-if="!messageList.length"
        :description="activeFilter === 'unread' ? '没有未读消息' : '暂无消息'"
        class="empty-wrap"
      />

      <template v-else>
        <div
          v-for="msg in messageList"
          :key="msg.id"
          class="msg-card"
          :class="{ unread: !orderStore.isRead(msg.id) }"
          @click="openMessage(msg)"
        >
          <div class="msg-icon" :class="msg.type">
            <van-icon :name="ICONS[msg.type]" />
          </div>
          <div class="msg-main">
            <div class="msg-title">
              {{ msg.title }}
              <i v-if="!orderStore.isRead(msg.id)" class="red-dot" />
            </div>
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-time">{{ relativeTime(msg.time) }}</div>
          </div>
          <van-icon name="arrow" class="msg-arrow" />
        </div>
      </template>

      <p v-if="messageList.length" class="list-footer">
        共 {{ messageList.length }} 条消息
      </p>
    </div>
  </div>
</template>

<style scoped>
.notif-page {
  background: #f2f5fb;
}

.notif-hero {
  padding: calc(14px + env(safe-area-inset-top)) 18px 20px;
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

.read-all {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}

.notif-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.action-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 38px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
  font-size: 13px;
}

.action-btn:active {
  background: rgba(255, 255, 255, 0.26);
}

.filter-tabs {
  display: flex;
  gap: 18px;
  padding: 14px 18px 6px;
  overflow-x: auto;
  background: #f2f5fb;
}

.filter-tab {
  position: relative;
  padding-bottom: 6px;
  font-size: 13px;
  color: #7b869c;
  white-space: nowrap;
}

.filter-tab.on {
  color: #2258e8;
  font-weight: 600;
}

.filter-tab.on::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 0;
  width: 18px;
  height: 3px;
  border-radius: 2px;
  background: #2258e8;
  transform: translateX(-50%);
}

.filter-count {
  margin-left: 2px;
  font-size: 10px;
  color: inherit;
}

.msg-list {
  padding: 8px 14px 8px;
}

.msg-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  margin-bottom: 10px;
  background: #fff;
  border-radius: 16px;
  border-left: 3px solid transparent;
  box-shadow: 0 8px 24px rgba(23, 54, 200, 0.07);
}

.msg-card:active {
  transform: scale(0.99);
}

.msg-card.unread {
  border-left-color: #2258e8;
}

.msg-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.msg-icon.order {
  background: #eef2ff;
  color: #2258e8;
}

.msg-icon.alert {
  background: #fff4e6;
  color: #f0a03c;
}

.msg-icon.system {
  background: #eef5ff;
  color: #4b7fd8;
}

.msg-main {
  flex: 1;
  min-width: 0;
}

.msg-title {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2333;
}

.red-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #e35d6a;
}

.msg-content {
  margin-top: 5px;
  font-size: 12px;
  line-height: 1.55;
  color: #7b869c;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.msg-time {
  margin-top: 6px;
  font-size: 10px;
  color: #b3bbcb;
}

.msg-arrow {
  margin-top: 10px;
  font-size: 13px;
  color: #c4ccdb;
}

.empty-wrap {
  padding: 50px 0;
}

.list-footer {
  margin: 6px 0 0;
  text-align: center;
  font-size: 11px;
  color: #b3bbcb;
}
</style>
