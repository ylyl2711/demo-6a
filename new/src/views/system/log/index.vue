<template>
  <div>
    <a-card :bordered="false">
      <a-form layout="inline" class="filter-bar">
        <a-form-item label="操作人">
          <a-input v-model:value="query.operUser" allow-clear placeholder="姓名 / 用户名" style="width: 160px" />
        </a-form-item>
        <a-form-item label="时间范围">
          <a-range-picker v-model:value="range" value-format="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="loadData(1)">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
            <a-button @click="exportLog">导出</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        size="middle"
        @change="handleTableChange"
      />
    </a-card>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import { systemApi } from '@/api/stockwork';

  const loading = ref(false);
  const list = ref<any[]>([]);
  const range = ref<[string, string] | undefined>();
  const query = reactive({
    operUser: '',
    pageNum: 1,
    pageSize: 10,
  });
  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showSizeChanger: true,
    showTotal: (t: number) => `共 ${t} 条`,
  });

  const columns = [
    { title: '操作人', dataIndex: 'operUser', key: 'operUser', width: 120 },
    { title: '操作时间', dataIndex: 'operTime', key: 'operTime', width: 180 },
    { title: '模块', dataIndex: 'operModule', key: 'operModule', width: 140 },
    { title: '操作内容', dataIndex: 'operContent', key: 'operContent' },
    { title: 'IP 地址', dataIndex: 'ipAddress', key: 'ipAddress', width: 150 },
  ];

  const params = () => ({
    operUser: query.operUser || undefined,
    startTime: range.value?.[0],
    endTime: range.value?.[1],
  });

  const loadData = async (page = query.pageNum) => {
    query.pageNum = page;
    loading.value = true;
    try {
      const data = await systemApi.logList({ ...query, ...params() });
      list.value = data.list;
      pagination.total = data.total;
      pagination.current = page;
    } finally {
      loading.value = false;
    }
  };

  const handleTableChange = (page: any) => {
    query.pageSize = page.pageSize;
    loadData(page.current);
  };

  const resetQuery = () => {
    query.operUser = '';
    range.value = undefined;
    loadData(1);
  };

  const exportLog = () => systemApi.exportLog(params());

  onMounted(() => loadData(1));
</script>

<style lang="less" scoped>
  .filter-bar {
    margin-bottom: 12px;
  }
</style>
