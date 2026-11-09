<template>
  <div class="dashboard-page">
    <a-card :bordered="false" class="hero-card">
      <div class="hero-main">
        <div>
          <h2>{{ greeting }}，{{ userStore.userInfo.realName || userStore.userInfo.username }}</h2>
          <p>
            {{ userStore.userInfo.roleName }} · 当前生产基地数据实时汇总 ·
            {{ today }}
          </p>
        </div>
        <a-space>
          <a-tag color="pink">宁波基地</a-tag>
          <a-tag color="blue">合肥基地</a-tag>
        </a-space>
      </div>
    </a-card>

    <a-spin :spinning="loading">
      <a-row :gutter="[16, 16]" class="stat-row">
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card :bordered="false" class="stat-card">
            <a-statistic title="生产工单总数" :value="orderTotal">
              <template #prefix><Icon icon="ant-design:schedule-outlined" class="stat-icon" /></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card :bordered="false" class="stat-card">
            <a-statistic title="待质检工单" :value="checkTotal" :value-style="{ color: '#d94f7b' }">
              <template #prefix><Icon icon="ant-design:audit-outlined" class="stat-icon" /></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card :bordered="false" class="stat-card">
            <a-statistic title="库存下限预警" :value="warningList.length" :value-style="{ color: '#e8912d' }">
              <template #prefix><Icon icon="ant-design:warning-outlined" class="stat-icon" /></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :lg="6">
          <a-card :bordered="false" class="stat-card">
            <a-statistic title="呆滞批次" :value="stagnantList.length" :value-style="{ color: '#8c8c8c' }">
              <template #prefix><Icon icon="ant-design:clock-circle-outlined" class="stat-icon" /></template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :lg="10">
          <a-card :bordered="false" title="工单状态分布">
            <div v-for="item in statusCounts" :key="item.label" class="status-line">
              <span class="status-label">{{ item.label }}</span>
              <a-progress
                :percent="orderTotal ? Math.round((item.count / orderTotal) * 100) : 0"
                :stroke-color="item.color"
                size="small"
              />
              <span class="status-count">{{ item.count }}</span>
            </div>
            <a-empty v-if="!orderTotal" description="暂无工单数据" />
          </a-card>
        </a-col>

        <a-col :xs="24" :lg="14">
          <a-card :bordered="false" title="库存下限预警">
            <a-table
              :columns="warningColumns"
              :data-source="warningList"
              :pagination="false"
              size="small"
              row-key="materialId"
              :locale="{ emptyText: '当前库存均在安全范围内' }"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'stock'">
                  <span class="danger-text">{{ record.totalStock }}</span>
                  <span class="muted"> / 下限 {{ record.stockMin }}</span>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
      </a-row>

      <a-card :bordered="false" title="呆滞库存提示" class="mt-4">
        <a-table
          :columns="stagnantColumns"
          :data-source="stagnantList"
          :pagination="false"
          size="small"
          row-key="batchNo"
          :locale="{ emptyText: '暂无超期呆滞批次' }"
        />
      </a-card>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  import dayjs from 'dayjs';
  import { Icon } from '@/components/basic/icon';
  import { useUserStore } from '@/store/modules/user';
  import { commonApi, reportCheckApi, stockApi, workOrderApi } from '@/api/stockwork';

  const userStore = useUserStore();
  const loading = ref(false);
  const orderTotal = ref(0);
  const checkTotal = ref(0);
  const warningList = ref<any[]>([]);
  const stagnantList = ref<any[]>([]);
  const statusCounts = ref([
    { label: '待生产', count: 0, color: '#bfbfbf' },
    { label: '生产中', count: 0, color: '#5b8ff9' },
    { label: '待质检', count: 0, color: '#d94f7b' },
    { label: '已完成', count: 0, color: '#52c41a' },
  ]);

  const today = dayjs().format('YYYY 年 MM 月 DD 日');
  const greeting = computed(() => {
    const hour = dayjs().hour();
    if (hour < 11) return '早上好';
    if (hour < 13) return '中午好';
    if (hour < 18) return '下午好';
    return '晚上好';
  });

  const warningColumns = [
    { title: '物料编码', dataIndex: 'materialCode', key: 'materialCode', width: 130 },
    { title: '物料名称', dataIndex: 'materialName', key: 'materialName' },
    { title: '基地', dataIndex: 'baseSite', key: 'baseSite', width: 80 },
    { title: '库存 / 下限', key: 'stock', width: 160 },
  ];

  const stagnantColumns = [
    { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 150 },
    { title: '物料名称', dataIndex: 'materialName', key: 'materialName' },
    { title: '基地', dataIndex: 'baseSite', key: 'baseSite', width: 80 },
    { title: '剩余数量', dataIndex: 'remainStock', key: 'remainStock', width: 100 },
    { title: '呆滞天数', key: 'days', width: 140 },
  ];

  const loadData = async () => {
    loading.value = true;
    const [orders, todo, warnings, stagnant, allOrders] = await Promise.allSettled([
      workOrderApi.list({ pageNum: 1, pageSize: 1 }),
      reportCheckApi.todo({ pageNum: 1, pageSize: 1 }),
      commonApi.warning(),
      stockApi.stagnant(),
      workOrderApi.list({ pageNum: 1, pageSize: 100 }),
    ]);
    if (orders.status === 'fulfilled') orderTotal.value = orders.value.total;
    if (todo.status === 'fulfilled') checkTotal.value = todo.value.total;
    if (warnings.status === 'fulfilled') warningList.value = warnings.value.slice(0, 6);
    if (stagnant.status === 'fulfilled') stagnantList.value = stagnant.value.slice(0, 6);
    if (allOrders.status === 'fulfilled') {
      const counts = [0, 0, 0, 0];
      allOrders.value.list.forEach((o) => {
        counts[o.workStatus] = (counts[o.workStatus] || 0) + 1;
      });
      statusCounts.value = statusCounts.value.map((item, index) => ({
        ...item,
        count: counts[index],
      }));
    }
    loading.value = false;
  };

  onMounted(loadData);
</script>

<style lang="less" scoped>
  .dashboard-page {
    .hero-card {
      margin-bottom: 16px;
      border-radius: 12px;
      background: linear-gradient(120deg, #fff6fa 0%, #ffffff 60%);
      border: 1px solid #f7e2ea;

      .hero-main {
        display: flex;
        align-items: center;
        justify-content: space-between;
      }

      h2 {
        margin: 0 0 6px;
        font-size: 20px;
        color: #1f2d3d;
      }

      p {
        margin: 0;
        color: #8c93a1;
        font-size: 13px;
      }
    }

    .stat-card {
      border-radius: 12px;
      border: 1px solid #eef1f6;

      .stat-icon {
        margin-right: 6px;
        color: #9aa4b5;
      }
    }

    .status-line {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 8px;

      .status-label {
        width: 64px;
        font-size: 13px;
        color: #5c6675;
      }

      :deep(.ant-progress) {
        flex: 1;
        margin-bottom: 0;
      }

      .status-count {
        width: 36px;
        text-align: right;
        font-weight: 600;
        color: #394452;
      }
    }

    .danger-text {
      color: #d4380d;
      font-weight: 600;
    }

    .muted {
      color: #98a0ae;
      font-size: 12px;
    }

    .mt-4 {
      margin-top: 16px;
    }
  }
</style>
