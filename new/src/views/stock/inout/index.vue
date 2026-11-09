<template>
  <div>
    <a-card :bordered="false">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="inout" tab="出入库登记">
          <a-row :gutter="16">
            <a-col :xs="24" :lg="10">
              <a-form layout="vertical" :model="form">
                <a-form-item label="业务类型">
                  <a-radio-group v-model:value="form.billType" button-style="solid">
                    <a-radio-button :value="1">入库</a-radio-button>
                    <a-radio-button :value="2">出库</a-radio-button>
                  </a-radio-group>
                </a-form-item>
                <a-form-item label="物料" required>
                  <a-select
                    v-model:value="form.materialId"
                    show-search
                    option-filter-prop="label"
                    :options="materialOptions"
                    placeholder="搜索物料编码或名称"
                    @change="handleMaterialChange"
                  />
                </a-form-item>
                <a-form-item label="数量" required>
                  <a-input-number v-model:value="form.operNum" :min="0.01" style="width: 100%" />
                </a-form-item>
                <a-form-item v-if="form.billType === 1" label="批次号" required>
                  <a-input v-model:value="form.batchNo" placeholder="入库必须填写批次号，如 B-NB-20261109" />
                </a-form-item>
                <a-form-item v-else label="出库批次">
                  <a-select
                    v-model:value="form.batchId"
                    allow-clear
                    placeholder="不选则按 FIFO 先进先出自动分配"
                    :options="batchOptions"
                  />
                </a-form-item>
                <a-form-item label="关联工单（可选）">
                  <a-select
                    v-model:value="form.workOrderId"
                    allow-clear
                    show-search
                    option-filter-prop="label"
                    :options="orderOptions"
                    placeholder="生产领料 / 成品入库可关联工单"
                  />
                </a-form-item>
                <a-form-item label="备注">
                  <a-input v-model:value="form.remark" placeholder="选填" />
                </a-form-item>
                <a-space>
                  <a-button type="primary" :loading="submitting" @click="submitInOut">
                    提交{{ form.billType === 1 ? '入库' : '出库' }}
                  </a-button>
                  <a-button @click="resetForm">重置</a-button>
                </a-space>
              </a-form>
            </a-col>
            <a-col :xs="24" :lg="14">
              <a-card :bordered="false" size="small" title="批次 FIFO 明细（先进先出）">
                <a-table
                  :columns="batchColumns"
                  :data-source="fifoList"
                  :pagination="false"
                  size="small"
                  row-key="id"
                  :locale="{ emptyText: '请选择物料查看可用批次' }"
                />
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>

        <a-tab-pane key="bills" tab="库存单据">
          <a-form layout="inline" class="filter-bar">
            <a-form-item label="单据类型">
              <a-select
                v-model:value="billQuery.billType"
                allow-clear
                placeholder="全部"
                style="width: 130px"
                :options="[
                  { label: '入库单', value: 1 },
                  { label: '出库单', value: 2 },
                ]"
              />
            </a-form-item>
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="loadBills(1)">查询</a-button>
                <a-button @click="resetBills">重置</a-button>
              </a-space>
            </a-form-item>
          </a-form>
          <a-table
            :columns="billColumns"
            :data-source="bills"
            :loading="billLoading"
            :pagination="billPagination"
            row-key="id"
            size="middle"
            @change="handleBillTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'billType'">
                <a-tag :color="record.billType === 1 ? 'green' : 'orange'">
                  {{ record.billType === 1 ? '入库' : '出库' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'billStatus'">
                <a-tag :color="record.billStatus === 1 ? 'default' : 'processing'">
                  {{ record.billStatus === 1 ? '已作废' : '正常' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-popconfirm
                  v-if="record.billStatus !== 1"
                  title="作废后将反冲库存，确认作废？"
                  @confirm="voidBill(record.id)"
                >
                  <a class="danger-text">作废</a>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="check" tab="库存盘点">
          <a-form layout="inline" :model="checkForm">
            <a-form-item label="物料">
              <a-select
                v-model:value="checkForm.materialId"
                show-search
                option-filter-prop="label"
                :options="materialOptions"
                placeholder="选择盘点物料"
                style="width: 300px"
              />
            </a-form-item>
            <a-form-item label="实盘数量">
              <a-input-number v-model:value="checkForm.realNum" :min="0" style="width: 160px" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" :loading="checking" @click="submitCheck">提交盘点</a-button>
            </a-form-item>
          </a-form>
          <a-alert
            v-if="checkResult"
            class="mt-3"
            type="success"
            show-icon
            :message="`${checkResult.materialName}：账面 ${checkResult.before} → 实盘 ${checkResult.realNum}，差异 ${checkResult.diff}`"
          />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { stockApi, warehouseApi, workOrderApi } from '@/api/stockwork';

  const activeTab = ref('inout');
  const submitting = ref(false);

  const form = reactive({
    billType: 1,
    materialId: undefined as number | undefined,
    operNum: 1,
    batchNo: '',
    batchId: undefined as number | undefined,
    workOrderId: undefined as number | undefined,
    remark: '',
  });

  const materialOptions = ref<{ label: string; value: number }[]>([]);
  const orderOptions = ref<{ label: string; value: number }[]>([]);
  const fifoList = ref<any[]>([]);
  const batchOptions = ref<{ label: string; value: number }[]>([]);

  const batchColumns = [
    { title: '批次号', dataIndex: 'batchNo', key: 'batchNo' },
    { title: '剩余库存', dataIndex: 'remainStock', key: 'remainStock', width: 100 },
    { title: '入库时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
    { title: '库龄(天)', dataIndex: 'days', key: 'days', width: 90 },
  ];

  const billLoading = ref(false);
  const bills = ref<any[]>([]);
  const billQuery = reactive({
    billType: undefined as number | undefined,
    pageNum: 1,
    pageSize: 10,
  });
  const billPagination = reactive({
    current: 1,
    pageSize: 10,
    total: 0,
    showTotal: (t: number) => `共 ${t} 条`,
  });
  const billColumns = [
    { title: '单据号', dataIndex: 'billNo', key: 'billNo', width: 180 },
    { title: '类型', key: 'billType', width: 80 },
    { title: '物料', dataIndex: 'materialName', key: 'materialName' },
    { title: '批次', dataIndex: 'batchNo', key: 'batchNo', width: 150 },
    { title: '数量', dataIndex: 'operNum', key: 'operNum', width: 90 },
    { title: '操作人', dataIndex: 'operUser', key: 'operUser', width: 100 },
    { title: '时间', dataIndex: 'billTime', key: 'billTime', width: 170 },
    { title: '状态', key: 'billStatus', width: 90 },
    { title: '操作', key: 'action', width: 70 },
  ];

  const checkForm = reactive({
    materialId: undefined as number | undefined,
    realNum: 0,
  });
  const checking = ref(false);
  const checkResult = ref<any>(null);

  const loadOptions = async () => {
    const [materials, workOrders] = await Promise.all([
      warehouseApi.options(),
      workOrderApi.list({ pageNum: 1, pageSize: 200 }),
    ]);
    materialOptions.value = materials.map((m) => ({
      label: `[${m.baseSite}] ${m.materialCode} ${m.materialName}（库存 ${m.totalStock}）`,
      value: m.id!,
    }));
    orderOptions.value = workOrders.list.map((o) => ({
      label: `${o.workOrderNo} · ${o.materialName}`,
      value: o.id,
    }));
  };

  const handleMaterialChange = async (materialId: number) => {
    form.batchId = undefined;
    fifoList.value = [];
    batchOptions.value = [];
    if (!materialId) return;
    fifoList.value = await stockApi.fifo(materialId);
    batchOptions.value = fifoList.value.map((b) => ({
      label: `${b.batchNo}（剩余 ${b.remainStock}）`,
      value: b.id,
    }));
  };

  const resetForm = () => {
    Object.assign(form, {
      billType: 1,
      materialId: undefined,
      operNum: 1,
      batchNo: '',
      batchId: undefined,
      workOrderId: undefined,
      remark: '',
    });
    fifoList.value = [];
    batchOptions.value = [];
  };

  const submitInOut = async () => {
    if (!form.materialId) return message.warning('请选择物料');
    if (!form.operNum || form.operNum <= 0) return message.warning('数量必须大于 0');
    if (form.billType === 1 && !form.batchNo.trim()) return message.warning('入库必须填写批次号');
    submitting.value = true;
    try {
      const payload = {
        materialId: form.materialId,
        operNum: form.operNum,
        batchNo: form.billType === 1 ? form.batchNo.trim() : undefined,
        batchId: form.billType === 2 ? form.batchId : undefined,
        workOrderId: form.workOrderId,
        remark: form.remark,
      };
      if (form.billType === 1) {
        await stockApi.inBound(payload);
      } else {
        await stockApi.outBound(payload);
      }
      message.success('提交成功');
      await loadOptions();
      handleMaterialChange(form.materialId);
      loadBills(1);
      clearFormAfterSubmit();
    } finally {
      submitting.value = false;
    }
  };

  const clearFormAfterSubmit = () => {
    form.operNum = 1;
    form.batchNo = '';
    form.batchId = undefined;
    form.remark = '';
  };

  const loadBills = async (page = billQuery.pageNum) => {
    billQuery.pageNum = page;
    billLoading.value = true;
    try {
      const data = await stockApi.bills({ ...billQuery });
      bills.value = data.list;
      billPagination.total = data.total;
      billPagination.current = page;
      billPagination.pageSize = billQuery.pageSize;
    } finally {
      billLoading.value = false;
    }
  };

  const handleBillTableChange = (page: any) => {
    billQuery.pageSize = page.pageSize;
    loadBills(page.current);
  };

  const resetBills = () => {
    billQuery.billType = undefined;
    loadBills(1);
  };

  const voidBill = async (id: number) => {
    await stockApi.voidBill(id);
    message.success('单据已作废');
    loadBills();
    loadOptions();
  };

  const submitCheck = async () => {
    if (!checkForm.materialId) return message.warning('请选择盘点物料');
    checking.value = true;
    try {
      checkResult.value = await stockApi.check(checkForm.materialId, checkForm.realNum);
      message.success('盘点完成');
      loadOptions();
    } finally {
      checking.value = false;
    }
  };

  onMounted(async () => {
    await loadOptions();
    loadBills(1);
  });
</script>

<style lang="less" scoped>
  .filter-bar {
    margin-bottom: 12px;
  }

  .danger-text {
    color: #d4380d;
  }

  .mt-3 {
    margin-top: 12px;
  }
</style>
