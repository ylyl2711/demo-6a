<template>
  <div>
    <a-card :bordered="false">
      <a-form layout="inline" class="filter-bar">
        <a-form-item label="生产基地">
          <a-select
            v-model:value="query.baseSite"
            :options="baseSites"
            allow-clear
            placeholder="全部基地"
            style="width: 140px"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="loadData(1)">查询</a-button>
            <a-button @click="loadData(1)">
              <template #icon><Icon icon="ant-design:reload-outlined" /></template>
              刷新
            </a-button>
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
        :locale="{ emptyText: '暂无待质检工单' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'summary'">
            <span class="good-text">{{ record.summary?.goodTotal }}</span>
            <span class="muted"> / </span>
            <span class="bad-text">{{ record.summary?.badTotal }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openDetail(record)">查看报工</a>
              <a class="good-text" @click="openAudit(record, 1)">合格</a>
              <a class="bad-text" @click="openAudit(record, 2)">不合格</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailOpen" title="报工与现场照片" width="720">
      <template v-if="detail">
        <a-descriptions bordered size="small" :column="2">
          <a-descriptions-item label="工单编号">{{ detail.workOrderNo }}</a-descriptions-item>
          <a-descriptions-item label="成品批次">{{ detail.productBatch }}</a-descriptions-item>
          <a-descriptions-item label="产品">{{ detail.materialName }}</a-descriptions-item>
          <a-descriptions-item label="计划数量">{{ detail.planNum }}</a-descriptions-item>
          <a-descriptions-item label="操作工">{{ detail.operatorName }}</a-descriptions-item>
          <a-descriptions-item label="基地">{{ detail.baseSite }}</a-descriptions-item>
        </a-descriptions>
        <a-table
          class="mt-3"
          :columns="processColumns"
          :data-source="detail.processes"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'good'">{{ record.report?.goodNum ?? '-' }}</template>
            <template v-else-if="column.key === 'bad'">
              {{ record.report?.badNum ?? '-' }}
              <span v-if="record.report?.badNum > 0" class="muted">（{{ record.report?.badReason }}）</span>
            </template>
            <template v-else-if="column.key === 'photo'">
              <a-image
                v-if="record.report?.photoUrl"
                :src="record.report.photoUrl"
                :width="52"
                :height="52"
                style="object-fit: cover"
              />
              <span v-else class="muted">无</span>
            </template>
            <template v-else-if="column.key === 'reportTime'">
              {{ record.report?.reportTime || '-' }}
            </template>
          </template>
        </a-table>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="auditOpen"
      :title="auditForm.result === 1 ? '质检合格确认' : '质检不合格退回'"
      :confirm-loading="auditing"
      @ok="submitAudit"
    >
      <a-form layout="vertical">
        <a-form-item label="工单">
          <a-input :value="`${current?.workOrderNo} · ${current?.materialName}`" disabled />
        </a-form-item>
        <a-form-item label="质检结论">
          <a-radio-group v-model:value="auditForm.result">
            <a-radio :value="1">合格（成品自动入库）</a-radio>
            <a-radio :value="2">不合格（退回重工）</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item v-if="auditForm.result === 2" label="质检不良原因" required>
          <a-select v-model:value="auditForm.badReasonId" :options="badReasons" placeholder="请选择" />
        </a-form-item>
        <a-form-item label="质检备注">
          <a-textarea v-model:value="auditForm.remark" :rows="3" placeholder="选填" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { Icon } from '@/components/basic/icon';
  import { commonApi, reportCheckApi, workOrderApi, type WorkOrder } from '@/api/stockwork';

  const loading = ref(false);
  const auditing = ref(false);
  const list = ref<any[]>([]);
  const baseSites = ref<{ label: string; value: string }[]>([]);
  const badReasons = ref<{ label: string; value: number }[]>([]);

  const query = reactive({
    baseSite: undefined as string | undefined,
    pageNum: 1,
    pageSize: 10,
  });

  const pagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showTotal: (t: number) => `共 ${t} 条`,
  });

  const columns = [
    { title: '工单编号', dataIndex: 'workOrderNo', key: 'workOrderNo', width: 180 },
    { title: '基地', dataIndex: 'baseSite', key: 'baseSite', width: 80 },
    { title: '产品', dataIndex: 'materialName', key: 'materialName' },
    { title: '成品批次', dataIndex: 'productBatch', key: 'productBatch', width: 160 },
    { title: '计划数量', dataIndex: 'planNum', key: 'planNum', width: 90 },
    { title: '操作工', dataIndex: 'operatorName', key: 'operatorName', width: 90 },
    { title: '良品/不良', key: 'summary', width: 110 },
    { title: '操作', key: 'action', width: 210 },
  ];

  const processColumns = [
    { title: '序号', dataIndex: 'processSeq', key: 'processSeq', width: 60 },
    { title: '工序', dataIndex: 'processName', key: 'processName', width: 100 },
    { title: '良品', key: 'good', width: 70 },
    { title: '不良', key: 'bad', width: 140 },
    { title: '报工时间', key: 'reportTime', width: 160 },
    { title: '现场照片', key: 'photo', width: 80 },
  ];

  const detailOpen = ref(false);
  const detail = ref<any>(null);
  const auditOpen = ref(false);
  const current = ref<WorkOrder | null>(null);
  const auditForm = reactive({
    result: 1,
    badReasonId: undefined as number | undefined,
    remark: '',
  });

  const loadData = async (page = query.pageNum) => {
    query.pageNum = page;
    loading.value = true;
    try {
      const data = await reportCheckApi.todo({ ...query });
      list.value = data.list;
      pagination.total = data.total;
      pagination.current = page;
    } finally {
      loading.value = false;
    }
  };

  const handleTableChange = (page: any) => {
    loadData(page.current);
  };

  const openDetail = async (record: WorkOrder) => {
    detail.value = await workOrderApi.detail(record.id);
    detailOpen.value = true;
  };

  const openAudit = (record: WorkOrder, result: number) => {
    current.value = record;
    auditForm.result = result;
    auditForm.badReasonId = undefined;
    auditForm.remark = '';
    auditOpen.value = true;
  };

  const submitAudit = async () => {
    if (auditForm.result === 2 && !auditForm.badReasonId) {
      return message.warning('不合格必须选择质检不良原因');
    }
    auditing.value = true;
    try {
      await reportCheckApi.audit({
        workOrderId: current.value?.id,
        result: auditForm.result,
        badReasonId: auditForm.badReasonId,
        remark: auditForm.remark,
      });
      message.success(auditForm.result === 1 ? '质检合格，成品已自动入库' : '已退回重工');
      auditOpen.value = false;
      loadData(1);
    } finally {
      auditing.value = false;
    }
  };

  onMounted(async () => {
    baseSites.value = await commonApi.baseSite();
    badReasons.value = (await commonApi.badReason()).map((r) => ({
      label: r.label,
      value: r.id,
    }));
    loadData(1);
  });
</script>

<style lang="less" scoped>
  .filter-bar {
    margin-bottom: 12px;
  }

  .muted {
    color: #98a0ae;
    font-size: 12px;
  }

  .good-text {
    color: #389e0d;
  }

  .bad-text {
    color: #cf1322;
  }

  .mt-3 {
    margin-top: 16px;
  }
</style>
