<template>
  <div>
    <a-card :bordered="false">
      <a-form layout="inline" class="filter-bar">
        <a-form-item label="字典类型">
          <a-select
            v-model:value="dictType"
            allow-clear
            show-search
            :options="typeOptions"
            placeholder="全部类型"
            style="width: 200px"
            @change="loadData"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="openModal()">
            <template #icon><Icon icon="ant-design:plus-outlined" /></template>
            新增字典
          </a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="list"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dictType'">
            <a-tag color="blue">{{ record.dictType }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openModal(record)">编辑</a>
              <a-popconfirm title="确认删除该字典项？" @confirm="removeItem(record.id)">
                <a class="danger-text">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="form.id ? '编辑字典' : '新增字典'"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item label="字典类型" required>
          <a-select
            v-model:value="form.dictType"
            show-search
            :options="typeOptions"
            placeholder="可选择已有类型或输入新类型"
            mode="tags"
            :max-count="1"
          />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="显示名称" required>
              <a-input v-model:value="form.dictLabel" placeholder="如：设备问题" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="字典值" required>
              <a-input v-model:value="form.dictValue" placeholder="如：1" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="排序">
          <a-input-number v-model:value="form.sort" :min="0" style="width: 160px" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, reactive, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { Icon } from '@/components/basic/icon';
  import { systemApi } from '@/api/stockwork';

  const loading = ref(false);
  const saving = ref(false);
  const list = ref<any[]>([]);
  const allList = ref<any[]>([]);
  const dictType = ref<string | undefined>();
  const typeOptions = ref<{ label: string; value: string }[]>([]);

  const columns = [
    { title: '字典类型', key: 'dictType', width: 200 },
    { title: '显示名称', dataIndex: 'dictLabel', key: 'dictLabel' },
    { title: '字典值', dataIndex: 'dictValue', key: 'dictValue', width: 140 },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 90 },
    { title: '操作', key: 'action', width: 120 },
  ];

  const form = reactive({
    id: undefined as number | undefined,
    dictType: '',
    dictLabel: '',
    dictValue: '',
    sort: 0,
  });
  const modalOpen = ref(false);

  const loadData = async () => {
    loading.value = true;
    try {
      allList.value = await systemApi.dictList();
      list.value = dictType.value
        ? allList.value.filter((d) => d.dictType === dictType.value)
        : allList.value;
      typeOptions.value = [...new Set(allList.value.map((d) => d.dictType))].map((t) => ({
        label: t!,
        value: t!,
      }));
    } finally {
      loading.value = false;
    }
  };

  const openModal = (record?: any) => {
    Object.assign(form, {
      id: undefined,
      dictType: dictType.value || '',
      dictLabel: '',
      dictValue: '',
      sort: 0,
      ...(record
        ? {
            id: record.id,
            dictType: record.dictType,
            dictLabel: record.dictLabel,
            dictValue: record.dictValue,
            sort: record.sort,
          }
        : {}),
    });
    modalOpen.value = true;
  };

  const handleSave = async () => {
    const type = Array.isArray(form.dictType) ? form.dictType[0] : form.dictType;
    if (!type || !form.dictLabel || form.dictValue === '') {
      return message.warning('请完整填写字典类型、名称与值');
    }
    saving.value = true;
    try {
      await systemApi.saveDict({ ...form, dictType: type });
      message.success('保存成功');
      modalOpen.value = false;
      loadData();
    } finally {
      saving.value = false;
    }
  };

  const removeItem = async (id: number) => {
    await systemApi.deleteDict(id);
    message.success('删除成功');
    loadData();
  };

  onMounted(loadData);
</script>

<style lang="less" scoped>
  .filter-bar {
    margin-bottom: 12px;
  }

  .danger-text {
    color: #d4380d;
  }
</style>
