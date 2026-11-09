<template>
  <div class="logo" :title="fullName">
    <span class="brand-mark">软</span>
    <h2 v-show="!collapsed" class="title">{{ shortName }}</h2>
  </div>
</template>

<script setup>
  defineProps({
    collapsed: {
      type: Boolean,
    },
  });

  const fullName = '双基地仓储库存与生产报工管理系统';
  const shortName = '双基地仓储报工';
</script>

<style lang="less" scoped>
  .logo {
    @apply flex overflow-hidden whitespace-nowrap items-center;

    height: 64px;
    padding-left: 20px;
    line-height: 64px;

    .brand-mark {
      flex: none;
      width: 32px;
      height: 32px;
      margin-right: 10px;
      border-radius: 9px;
      background: #fde7ef;
      color: #d94f7b;
      font-size: 15px;
      font-weight: 600;
      line-height: 32px;
      text-align: center;
    }

    .title {
      @apply mb-0;

      color: #1f2d3d;
      font-size: 15px;
      font-weight: 600;
      letter-spacing: 0.5px;
    }
  }
</style>
