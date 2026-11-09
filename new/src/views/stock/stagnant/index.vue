<template>
  <div>
    <a-card :bordered="false">
      <div class="head-line">
        <div>
          <h3>呆滞库存预警</h3>
          <p>按批次未消耗库存计算库龄，超过物料呆滞阈值天数时预警。</p>
        </div>
        <a-button @click="loadData">
          <template #icon><Icon icon="ant-design:reload-outlined" /></template>
          刷新
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        row-key="batchNo"
        size="middle"
        :locale="{ emptyText: '暂无呆滞批次，库存周转良好' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'days'">
            <a-tag color="volcano">{{ record.days }} 天</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="openConfig(record)">调整阈值</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      title="调整呆滞阈值"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form layout="vertical">
        <a-form-item :label="`${current?.materialCode} ${current?.materialName}`">
          <a-input-number v-model:value="day" :min="1" style="width: 100%" addon-after="天" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { Icon } from '@/components/basic/icon';
  import { stockApi } from '@/api/stockwork';

  const loading = ref(false);
  const saving = ref(false);
  const list = ref<any[]>([]);
  const modalOpen = ref(false);
  const current = ref<any>(null);
  const day = ref(90);

  const columns = [
    { title: '基地', dataIndex: 'baseSite', key: 'baseSite', width: 80 },
    { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 150 },
    { title: '物料编码', dataIndex: 'materialCode', key: 'materialCode', width: 140 },
    { title: '物料名称', dataIndex: 'materialName', key: 'materialName' },
    { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 150 },
    { title: '剩余库存', dataIndex: 'remainStock', key: 'remainStock', width: 100 },
    { title: '最近出入库', dataIndex: 'lastIoTime', key: 'lastIoTime', width: 170 },
    { title: '已呆滞', key: 'days', width: 100 },
    { title: '阈值(天)', dataIndex: 'stagnantDay', key: 'stagnantDay', width: 100 },
    { title: '操作', key: 'action', width: 100 },
  ];

  const loadData = async () => {
    loading.value = true;
    try {
      list.value = await stockApi.stagnant();
    } finally {
      loading.value = false;
    }
  };

  const openConfig = (record: any) => {
    current.value = record;
    day.value = record.stagnantDay || 90;
    modalOpen.value = true;
  };

  const handleSave = async () => {
    saving.value = true;
    try {
      await stockApi.configStagnant(current.value.materialId, day.value);
      message.success('阈值已更新');
      modalOpen.value = false;
      loadData();
    } finally {
      saving.value = false;
    }
  };

  onMounted(loadData);
</script>

<style lang="less" scoped>
  .head-line {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 16px;

    h3 {
      margin: 0 0 4px;
      font-size: 16px;
    }

    p {
      margin: 0;
      font-size: 12px;
      color: #8c93a1;
    }
  }
</style>
