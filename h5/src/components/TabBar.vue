<script setup lang="ts">
import { onMounted } from 'vue'
import { useOrderStore } from '@/stores/order'

const orderStore = useOrderStore()

onMounted(() => {
  orderStore.ensureLoaded()
})
</script>

<template>
  <van-tabbar route class="app-tabbar">
    <van-tabbar-item replace to="/" icon="orders-o">报工</van-tabbar-item>
    <van-tabbar-item
      replace
      to="/notification"
      icon="bell"
      :badge="orderStore.unreadCount > 0 ? String(orderStore.unreadCount) : ''"
    >
      消息
    </van-tabbar-item>
    <van-tabbar-item replace to="/profile" icon="user-o">我的</van-tabbar-item>
  </van-tabbar>
</template>
