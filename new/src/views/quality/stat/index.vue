<template>
  <div>
    <a-card :bordered="false">
      <a-form layout="inline" class="filter-bar">
        <a-form-item label="统计周期">
          <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item label="生产基地">
          <a-select
            v-model:value="baseSite"
            :options="baseSites"
            allow-clear
            placeholder="全部基地"
            style="width: 140px"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="loadData">统计</a-button>
            <a-button @click="exportStat">导出 Excel</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-row :gutter="16" class="mb-3">
        <a-col :xs="24" :sm="8">
          <a-card :bordered="false" class="mini-card">
            <a-statistic title="统计工单数" :value="stat.orderCount || 0" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-card :bordered="false" class="mini-card">
            <a-statistic title="良品总数" :value="stat.totalGood || 0" :value-style="{ color: '#389e0d' }" />
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="8">
          <a-card :bordered="false" class="mini-card">
            <a-statistic title="不良品总数" :value="stat.totalBad || 0" :value-style="{ color: '#cf1322' }" />
          </a-card>
        </a-col>
      </a-row>

      <a-table
        :columns="columns"
        :data-source="stat.items || []"
        :loading="loading"
        :pagination="false"
        row-key="name"
        size="middle"
        :locale="{ emptyText: '所选周期内无不良记录' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'count'">
            <a-tag color="volcano">{{ record.value }}</a-tag>
          </template>
          <template v-else-if="column.key === 'percent'">
            {{ stat.totalBad ? ((record.value / stat.totalBad) * 100).toFixed(1) : '0.0' }}%
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { commonApi, qualityApi } from '@/api/stockwork';

  const loading = ref(false);
  const range = ref<[string, string] | undefined>();
  const baseSite = ref<string | undefined>();
  const baseSites = ref<{ label: string; value: string }[]>([]);
  const stat = ref<any>({});

  const columns = [
    { title: '不良原因', dataIndex: 'name', key: 'name' },
    { title: '数量', key: 'count', width: 120 },
    { title: '占比', key: 'percent', width: 120 },
  ];

  const params = () => ({
    startDate: range.value?.[0],
    endDate: range.value?.[1],
    baseSite: baseSite.value,
  });

  const loadData = async () => {
    loading.value = true;
    try {
      stat.value = await qualityApi.stat(params());
    } finally {
      loading.value = false;
    }
  };

  const exportStat = async () => {
    if (!stat.value.items?.length) return message.warning('暂无可导出的统计数据');
    await qualityApi.exportStat(params());
  };

  onMounted(async () => {
    baseSites.value = await commonApi.baseSite();
    loadData();
  });
</script>

<style lang="less" scoped>
  .filter-bar {
    margin-bottom: 16px;
  }

  .mini-card {
    border-radius: 10px;
    border: 1px solid #eef1f6;
  }

  .mb-3 {
    margin-bottom: 16px;
  }
</style>
