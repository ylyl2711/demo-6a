<template>
  <div>
    <a-card :bordered="false">
      <a-form layout="inline" :model="query" class="filter-bar">
        <a-form-item label="生产基地">
          <a-select
            v-model:value="query.baseSite"
            :options="baseSites"
            allow-clear
            placeholder="全部基地"
            style="width: 140px"
          />
        </a-form-item>
        <a-form-item label="工单状态">
          <a-select
            v-model:value="query.workStatus"
            :options="statusOptions"
            allow-clear
            placeholder="全部状态"
            style="width: 140px"
          />
        </a-form-item>
        <a-form-item label="操作工">
          <a-select
            v-model:value="query.operatorUserId"
            :options="operatorSelectOptions"
            allow-clear
            show-search
            option-filter-prop="label"
            placeholder="全部操作工"
            style="width: 160px"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="loadData(1)">查询</a-button>
            <a-button @click="resetQuery">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <div class="toolbar">
        <a-button type="primary" @click="openModal()">
          <template #icon><Icon icon="ant-design:plus-outlined" /></template>
          新建工单
        </a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        size="middle"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'workStatus'">
            <a-tag :color="statusColor(record.workStatus)">{{ statusName(record.workStatus) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'progress'">
            <a-progress
              :percent="record.totalProcess ? Math.round((record.doneProcess / record.totalProcess) * 100) : 0"
              size="small"
              :stroke-color="record.workStatus === 3 ? '#52c41a' : '#d94f7b'"
            />
            <span class="muted">{{ record.doneProcess }}/{{ record.totalProcess }} 工序</span>
          </template>
          <template v-else-if="column.key === 'quality'">
            <span class="good-text">{{ record.goodTotal }}</span>
            <span class="muted"> / </span>
            <span class="bad-text">{{ record.badTotal }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openDetail(record)">详情</a>
              <a v-if="record.workStatus === 0" @click="openModal(record)">编辑</a>
              <a
                v-if="record.workStatus === 3"
                @click="qualityApi.exportReport(record.id)"
              >质检报告</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="form.id ? '编辑工单' : '新建生产工单'"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form layout="vertical" :model="form">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="生产基地" required>
              <a-select v-model:value="form.baseSite" :options="baseSites" placeholder="请选择" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="生产产品" required>
              <a-select
                v-model:value="form.materialId"
                show-search
                option-filter-prop="label"
                :options="productOptions"
                placeholder="请选择成品物料"
                @change="handleProductChange"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="计划数量" required>
              <a-input-number v-model:value="form.planNum" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="车间操作工" required>
              <a-select
                v-model:value="form.operatorUserId"
                :options="operatorSelectOptions"
                placeholder="请分配操作工"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="成品批次号">
              <a-input v-model:value="form.productBatch" placeholder="留空则自动生成" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-alert
          type="info"
          show-icon
          message="保存工单时会自动按产品的工序模板复制工序，请先确认工序模板已维护。"
        />
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailOpen" title="工单详情" width="760">
      <template v-if="detail">
        <a-descriptions bordered size="small" :column="2">
          <a-descriptions-item label="工单编号">{{ detail.workOrderNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColor(detail.workStatus)">{{ statusName(detail.workStatus) }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="生产基地">{{ detail.baseSite }}</a-descriptions-item>
          <a-descriptions-item label="成品批次">{{ detail.productBatch }}</a-descriptions-item>
          <a-descriptions-item label="生产产品">
            {{ detail.materialCode }} {{ detail.materialName }}
          </a-descriptions-item>
          <a-descriptions-item label="计划数量">{{ detail.planNum }}</a-descriptions-item>
          <a-descriptions-item label="操作工">{{ detail.operatorName }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detail.createTime }}</a-descriptions-item>
        </a-descriptions>

        <a-divider orientation="left">工序进度</a-divider>
        <a-steps
          :current="detail.summary.doneProcess"
          size="small"
          :items="detail.processes.map((p: any) => ({
            title: p.processName,
            description: p.processStatus === 2 ? '已报工' : '未报工',
          }))"
        />

        <a-table
          class="mt-3"
          :columns="processColumns"
          :data-source="detail.processes"
          :pagination="false"
          row-key="id"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'reportGood'">
              {{ record.report?.goodNum ?? '-' }}
            </template>
            <template v-else-if="column.key === 'reportBad'">
              {{ record.report?.badNum ?? '-' }}
              <span v-if="record.report?.badNum > 0" class="muted">
                （{{ record.report?.badReason }}）
              </span>
            </template>
            <template v-else-if="column.key === 'photo'">
              <a-image
                v-if="record.report?.photoUrl"
                :src="record.report.photoUrl"
                :width="48"
                :height="48"
                style="object-fit: cover"
              />
              <span v-else class="muted">无</span>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.processStatus === 2 ? 'green' : 'default'">
                {{ record.processStatus === 2 ? '已报工' : '未报工' }}
              </a-tag>
            </template>
          </template>
        </a-table>

        <a-divider orientation="left">报工汇总</a-divider>
        <a-space size="large">
          <a-statistic title="良品总数" :value="detail.summary.goodTotal" />
          <a-statistic title="不良总数" :value="detail.summary.badTotal" />
          <a-statistic title="完成工序" :value="`${detail.summary.doneProcess}/${detail.summary.totalProcess}`" />
        </a-space>

        <div class="drawer-footer">
          <a-button
            v-if="detail.workStatus === 3"
            type="primary"
            @click="qualityApi.exportReport(detail.id)"
          >
            导出质检报告
          </a-button>
        </div>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { Icon } from '@/components/basic/icon';
  import { qualityApi, workOrderApi, type WorkOrder } from '@/api/stockwork';

  const loading = ref(false);
  const saving = ref(false);
  const list = ref<WorkOrder[]>([]);
  const baseSites = ref<{ label: string; value: string }[]>([]);
  const productRaw = ref<any[]>([]);
  const operatorRaw = ref<any[]>([]);

  const statusOptions = [
    { label: '待生产', value: 0 },
    { label: '生产中', value: 1 },
    { label: '待质检', value: 2 },
    { label: '已完成', value: 3 },
  ];

  const query = reactive({
    baseSite: undefined as string | undefined,
    workStatus: undefined as number | undefined,
    operatorUserId: undefined as number | undefined,
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
    { title: '工单编号', dataIndex: 'workOrderNo', key: 'workOrderNo', width: 180 },
    { title: '基地', dataIndex: 'baseSite', key: 'baseSite', width: 80 },
    { title: '生产产品', dataIndex: 'materialName', key: 'materialName' },
    { title: '计划数量', dataIndex: 'planNum', key: 'planNum', width: 90 },
    { title: '操作工', dataIndex: 'operatorName', key: 'operatorName', width: 90 },
    { title: '成品批次', dataIndex: 'productBatch', key: 'productBatch', width: 150 },
    { title: '状态', key: 'workStatus', width: 90 },
    { title: '工序进度', key: 'progress', width: 150 },
    { title: '良品/不良', key: 'quality', width: 110 },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
    { title: '操作', key: 'action', width: 170 },
  ];

  const processColumns = [
    { title: '序号', dataIndex: 'processSeq', key: 'processSeq', width: 60 },
    { title: '工序名称', dataIndex: 'processName', key: 'processName', width: 110 },
    { title: '工艺要求', dataIndex: 'requirement', key: 'requirement' },
    { title: '良品', key: 'reportGood', width: 70 },
    { title: '不良', key: 'reportBad', width: 130 },
    { title: '现场照片', key: 'photo', width: 80 },
    { title: '状态', key: 'status', width: 90 },
  ];

  const productOptions = ref<{ label: string; value: number }[]>([]);
  const operatorSelectOptions = ref<{ label: string; value: number }[]>([]);

  const form = reactive({
    id: undefined as number | undefined,
    baseSite: '宁波',
    materialId: undefined as number | undefined,
    planNum: 100,
    operatorUserId: undefined as number | undefined,
    productBatch: '',
  });
  const modalOpen = ref(false);
  const detailOpen = ref(false);
  const detail = ref<any>(null);

  const statusName = (s: number) => ['待生产', '生产中', '待质检', '已完成'][s] ?? '-';
  const statusColor = (s: number) =>
    ({ 0: 'default', 1: 'processing', 2: 'warning', 3: 'success' })[s] ?? 'default';

  const loadOptions = async () => {
    const data = await workOrderApi.options();
    baseSites.value = data.baseSites;
    productRaw.value = data.products;
    operatorRaw.value = data.operators;
    productOptions.value = data.products.map((p: any) => ({
      label: `[${p.baseSite}] ${p.materialCode} ${p.materialName}`,
      value: p.id,
    }));
    operatorSelectOptions.value = data.operators.map((o: any) => ({
      label: `${o.realName}（${o.username}）`,
      value: o.id,
    }));
  };

  const loadData = async (page = query.pageNum) => {
    query.pageNum = page;
    loading.value = true;
    try {
      const data = await workOrderApi.list({ ...query });
      list.value = data.list;
      pagination.total = data.total;
      pagination.current = page;
      pagination.pageSize = query.pageSize;
    } finally {
      loading.value = false;
    }
  };

  const handleTableChange = (page: any) => {
    query.pageSize = page.pageSize;
    loadData(page.current);
  };

  const resetQuery = () => {
    query.baseSite = undefined;
    query.workStatus = undefined;
    query.operatorUserId = undefined;
    loadData(1);
  };

  const openModal = (record?: WorkOrder) => {
    Object.assign(form, {
      id: undefined,
      baseSite: '宁波',
      materialId: undefined,
      planNum: 100,
      operatorUserId: undefined,
      productBatch: '',
      ...(record
        ? {
            id: record.id,
            baseSite: record.baseSite,
            materialId: record.materialId,
            planNum: record.planNum,
            operatorUserId: record.operatorUserId,
            productBatch: record.productBatch,
          }
        : {}),
    });
    modalOpen.value = true;
  };

  const handleProductChange = (materialId: number) => {
    const product = productRaw.value.find((p) => p.id === materialId);
    if (product?.baseSite) form.baseSite = product.baseSite;
  };

  const handleSave = async () => {
    if (!form.baseSite || !form.materialId || !form.operatorUserId) {
      return message.warning('请完整填写基地、产品与操作工');
    }
    if (!form.planNum || form.planNum <= 0) return message.warning('计划数量必须大于 0');
    saving.value = true;
    try {
      await workOrderApi.save({ ...form });
      message.success('工单保存成功');
      modalOpen.value = false;
      loadData(1);
    } finally {
      saving.value = false;
    }
  };

  const openDetail = async (record: WorkOrder) => {
    detail.value = await workOrderApi.detail(record.id);
    detailOpen.value = true;
  };

  onMounted(async () => {
    await loadOptions();
    loadData(1);
  });
</script>

<style lang="less" scoped>
  .filter-bar {
    margin-bottom: 12px;
  }

  .toolbar {
    margin-bottom: 12px;
  }

  .muted {
    color: #98a0ae;
    font-size: 12px;
  }

  .good-text {
    color: #389e0d;
    font-weight: 600;
  }

  .bad-text {
    color: #cf1322;
    font-weight: 600;
  }

  .mt-3 {
    margin-top: 16px;
  }

  .drawer-footer {
    margin-top: 24px;
  }
</style>
