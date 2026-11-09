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
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item label="物料类型">
          <a-select
            v-model:value="query.materialType"
            :options="materialTypes"
            allow-clear
            placeholder="全部类型"
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input
            v-model:value="query.keyword"
            allow-clear
            placeholder="物料编码 / 名称"
            style="width: 200px"
            @press-enter="loadData(1)"
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
          新增物料
        </a-button>
        <a-button @click="stockApi.exportLedger(query.baseSite)">
          <template #icon><Icon icon="ant-design:download-outlined" /></template>
          导出台账
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
          <template v-if="column.key === 'materialType'">
            <a-tag :color="typeColor(record.materialType)">{{ typeName(record.materialType) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'totalStock'">
            <span :class="{ 'danger-text': record.totalStock < record.stockMin }">
              {{ record.totalStock }}
            </span>
          </template>
          <template v-else-if="column.key === 'limit'">
            {{ record.stockMin }} / {{ record.stockMax }}
          </template>
          <template v-else-if="column.key === 'stagnantDay'">
            {{ record.stagnantDay || 90 }} 天
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openModal(record)">编辑</a>
              <a-popconfirm title="确认删除该物料档案？" @confirm="handleDelete(record.id)">
                <a class="danger-text">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="form.id ? '编辑物料档案' : '新增物料档案'"
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
            <a-form-item label="仓库名称" required>
              <a-input v-model:value="form.warehouseName" placeholder="如：宁波原材料库" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="物料编码" required>
              <a-input v-model:value="form.materialCode" placeholder="如：MAT-NB-001" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="物料名称" required>
              <a-input v-model:value="form.materialName" placeholder="请输入物料名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="物料类型">
              <a-select v-model:value="form.materialType" :options="materialTypes" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="呆滞阈值（天）">
              <a-input-number v-model:value="form.stagnantDay" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="库存下限">
              <a-input-number v-model:value="form.stockMin" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="库存上限">
              <a-input-number v-model:value="form.stockMax" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { Icon } from '@/components/basic/icon';
  import { commonApi, stockApi, warehouseApi, type Material } from '@/api/stockwork';

  const loading = ref(false);
  const saving = ref(false);
  const list = ref<Material[]>([]);
  const total = ref(0);
  const baseSites = ref<{ label: string; value: string }[]>([]);
  const materialTypes = ref([
    { label: '原材料', value: 1 },
    { label: '半成品', value: 2 },
    { label: '成品', value: 3 },
  ]);

  const query = reactive({
    baseSite: undefined as string | undefined,
    materialType: undefined as number | undefined,
    keyword: '',
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
    { title: '物料编码', dataIndex: 'materialCode', key: 'materialCode', width: 140 },
    { title: '物料名称', dataIndex: 'materialName', key: 'materialName' },
    { title: '生产基地', dataIndex: 'baseSite', key: 'baseSite', width: 100 },
    { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 150 },
    { title: '类型', key: 'materialType', width: 90 },
    { title: '当前库存', key: 'totalStock', width: 100 },
    { title: '下限 / 上限', key: 'limit', width: 130 },
    { title: '呆滞阈值', key: 'stagnantDay', width: 100 },
    { title: '操作', key: 'action', width: 110 },
  ];

  const form = reactive<Material>({
    id: undefined,
    warehouseName: '',
    baseSite: '宁波',
    materialCode: '',
    materialName: '',
    materialType: 1,
    stockMin: 0,
    stockMax: 0,
    stagnantDay: 90,
  });

  const modalOpen = ref(false);

  const typeName = (type?: number) =>
    ({ 1: '原材料', 2: '半成品', 3: '成品' })[type || 1] || '原材料';
  const typeColor = (type?: number) =>
    ({ 1: 'blue', 2: 'orange', 3: 'green' })[type || 1] || 'blue';

  const loadData = async (page = query.pageNum) => {
    query.pageNum = page;
    loading.value = true;
    try {
      const data = await warehouseApi.list({ ...query });
      list.value = data.list;
      total.value = data.total;
      pagination.total = data.total;
      pagination.current = query.pageNum;
      pagination.pageSize = query.pageSize;
    } finally {
      loading.value = false;
    }
  };

  const handleTableChange = (page: any) => {
    query.pageNum = page.current;
    query.pageSize = page.pageSize;
    loadData(page.current);
  };

  const resetQuery = () => {
    query.baseSite = undefined;
    query.materialType = undefined;
    query.keyword = '';
    loadData(1);
  };

  const openModal = (record?: Material) => {
    Object.assign(form, {
      id: undefined,
      warehouseName: '',
      baseSite: '宁波',
      materialCode: '',
      materialName: '',
      materialType: 1,
      stockMin: 0,
      stockMax: 0,
      stagnantDay: 90,
      ...(record || {}),
    });
    modalOpen.value = true;
  };

  const handleSave = async () => {
    if (!form.warehouseName || !form.baseSite || !form.materialCode || !form.materialName) {
      return message.warning('请完整填写仓库、基地、物料编码与名称');
    }
    saving.value = true;
    try {
      await warehouseApi.save({ ...form });
      message.success('保存成功');
      modalOpen.value = false;
      loadData();
    } finally {
      saving.value = false;
    }
  };

  const handleDelete = async (id: number) => {
    await warehouseApi.remove(id);
    message.success('删除成功');
    loadData();
  };

  onMounted(async () => {
    baseSites.value = await commonApi.baseSite();
    loadData(1);
  });
</script>

<style lang="less" scoped>
  .filter-bar {
    margin-bottom: 12px;
  }

  .toolbar {
    display: flex;
    gap: 10px;
    margin-bottom: 12px;
  }

  .danger-text {
    color: #d4380d;
  }
</style>
