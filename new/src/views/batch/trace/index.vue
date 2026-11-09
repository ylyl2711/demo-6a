<template>
  <div>
    <a-card :bordered="false">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="forward" tab="原材料批次正向追溯">
          <a-space class="search-line">
            <a-input
              v-model:value="forwardBatchNo"
              placeholder="请输入原材料批次号，如 B-GS-20260801"
              style="width: 320px"
              allow-clear
              @press-enter="searchForward"
            />
            <a-button type="primary" :loading="loading" @click="searchForward">追溯</a-button>
            <a-button v-if="forwardResult" @click="batchApi.exportForward(forwardBatchNo)">
              导出结果
            </a-button>
          </a-space>

          <template v-if="forwardResult">
            <a-descriptions bordered size="small" :column="3" class="mt-3">
              <a-descriptions-item label="批次号">{{ forwardResult.batchNo }}</a-descriptions-item>
              <a-descriptions-item label="物料">
                {{ forwardResult.materialCode }} {{ forwardResult.materialName }}
              </a-descriptions-item>
              <a-descriptions-item label="生产基地">{{ forwardResult.baseSite }}</a-descriptions-item>
              <a-descriptions-item label="批次剩余">{{ forwardResult.remainStock }}</a-descriptions-item>
              <a-descriptions-item label="最近出入库">{{ forwardResult.lastIoTime }}</a-descriptions-item>
            </a-descriptions>
            <a-table
              class="mt-3"
              :columns="forwardColumns"
              :data-source="forwardResult.orders"
              :pagination="false"
              row-key="workOrderNo"
              size="small"
              :locale="{ emptyText: '该批次暂未用于生产领料' }"
            />
          </template>
        </a-tab-pane>

        <a-tab-pane key="backward" tab="成品批次反向溯源">
          <a-space class="search-line">
            <a-input
              v-model:value="productBatch"
              placeholder="请输入成品批次号，如 PB-20261015-A"
              style="width: 320px"
              allow-clear
              @press-enter="searchBackward"
            />
            <a-button type="primary" :loading="loading" @click="searchBackward">溯源</a-button>
            <a-button v-if="backwardResult" @click="batchApi.exportBackward(productBatch)">
              导出结果
            </a-button>
          </a-space>

          <template v-if="backwardResult">
            <a-alert
              class="mt-3"
              type="info"
              show-icon
              :message="`成品批次 ${backwardResult.productBatch} 共追溯 ${backwardResult.orders.length} 张工单、${backwardResult.materials.length} 条原料耗用记录`"
            />
            <a-table
              class="mt-3"
              :columns="orderColumns"
              :data-source="backwardResult.orders"
              :pagination="false"
              row-key="workOrderNo"
              size="small"
              title="生产工单"
            />
            <a-table
              class="mt-3"
              :columns="materialColumns"
              :data-source="backwardResult.materials"
              :pagination="false"
              row-key="batchNo"
              size="small"
              title="原材料批次与耗用"
            />
          </template>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
  import { ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { batchApi } from '@/api/stockwork';

  const activeTab = ref('forward');
  const loading = ref(false);
  const forwardBatchNo = ref('B-GS-20260801');
  const productBatch = ref('PB-20261015-A');
  const forwardResult = ref<any>(null);
  const backwardResult = ref<any>(null);

  const forwardColumns = [
    { title: '工单编号', dataIndex: 'workOrderNo', key: 'workOrderNo' },
    { title: '基地', dataIndex: 'baseSite', key: 'baseSite', width: 80 },
    { title: '产出成品', dataIndex: 'productName', key: 'productName' },
    { title: '成品批次', dataIndex: 'productBatch', key: 'productBatch' },
    { title: '计划数量', dataIndex: 'planNum', key: 'planNum', width: 90 },
    { title: '领用数量', dataIndex: 'useNum', key: 'useNum', width: 90 },
    { title: '工单状态', dataIndex: 'workStatusLabel', key: 'workStatusLabel', width: 100 },
    { title: '领料时间', dataIndex: 'billTime', key: 'billTime', width: 170 },
  ];

  const orderColumns = [
    { title: '工单编号', dataIndex: 'workOrderNo', key: 'workOrderNo' },
    { title: '基地', dataIndex: 'baseSite', key: 'baseSite', width: 80 },
    { title: '成品名称', dataIndex: 'productName', key: 'productName' },
    { title: '计划数量', dataIndex: 'planNum', key: 'planNum', width: 90 },
    { title: '状态', dataIndex: 'workStatusLabel', key: 'workStatusLabel', width: 100 },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  ];

  const materialColumns = [
    { title: '工单编号', dataIndex: 'workOrderNo', key: 'workOrderNo', width: 180 },
    { title: '原材料', dataIndex: 'materialName', key: 'materialName' },
    { title: '物料编码', dataIndex: 'materialCode', key: 'materialCode', width: 140 },
    { title: '原材料批次', dataIndex: 'batchNo', key: 'batchNo', width: 160 },
    { title: '耗用数量', dataIndex: 'operNum', key: 'operNum', width: 100 },
    { title: '领料时间', dataIndex: 'billTime', key: 'billTime', width: 170 },
  ];

  const searchForward = async () => {
    if (!forwardBatchNo.value.trim()) return message.warning('请输入原材料批次号');
    loading.value = true;
    try {
      forwardResult.value = await batchApi.traceForward(forwardBatchNo.value.trim());
    } finally {
      loading.value = false;
    }
  };

  const searchBackward = async () => {
    if (!productBatch.value.trim()) return message.warning('请输入成品批次号');
    loading.value = true;
    try {
      backwardResult.value = await batchApi.traceBackward(productBatch.value.trim());
    } finally {
      loading.value = false;
    }
  };
</script>

<style lang="less" scoped>
  .search-line {
    margin-bottom: 8px;
  }

  .mt-3 {
    margin-top: 16px;
  }
</style>
