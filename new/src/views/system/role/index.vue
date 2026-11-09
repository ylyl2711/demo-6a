<template>
  <div>
    <a-card :bordered="false">
      <a-table
        :columns="columns"
        :data-source="roles"
        :loading="loading"
        :pagination="false"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'roleCode'">
            <a-tag :color="record.roleCode === 'ADMIN' ? 'purple' : record.roleCode === 'BUSINESS' ? 'blue' : 'green'">
              {{ record.roleCode }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'menuPerms'">
            <a-space wrap>
              <a-tag v-for="p in splitPerms(record.menuPerms)" :key="p">{{ permLabel(p) }}</a-tag>
            </a-space>
          </template>
          <template v-else-if="column.key === 'action'">
            <a @click="openModal(record)">配置权限</a>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="modalOpen" title="配置角色菜单权限" :confirm-loading="saving" @ok="handleSave">
      <a-alert type="info" show-icon :message="`当前角色：${current?.roleName}`" class="mb-3" />
      <a-checkbox-group v-model:value="checked" style="width: 100%">
        <a-row :gutter="[0, 12]">
          <a-col v-for="item in permOptions" :key="item.value" :span="12">
            <a-checkbox :value="item.value">{{ item.label }}</a-checkbox>
          </a-col>
        </a-row>
      </a-checkbox-group>
      <a-divider />
      <a-space>
        <a-button size="small" @click="checked = permOptions.map((p) => p.value)">全选</a-button>
        <a-button size="small" @click="checked = []">清空</a-button>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { systemApi } from '@/api/stockwork';
  import { useUserStore } from '@/store/modules/user';

  const userStore = useUserStore();
  const loading = ref(false);
  const saving = ref(false);
  const roles = ref<any[]>([]);
  const modalOpen = ref(false);
  const current = ref<any>(null);
  const checked = ref<string[]>([]);

  const permOptions = [
    { label: '数据概览', value: 'dashboard' },
    { label: '物料档案', value: 'warehouse' },
    { label: '出入库与台账 / 呆滞预警', value: 'stock' },
    { label: '批次追溯', value: 'batch' },
    { label: '生产工单 / BOM 与工序模板', value: 'workorder' },
    { label: '工单质检', value: 'reportCheck' },
    { label: '不良品统计', value: 'quality' },
    { label: '用户管理', value: 'system:user' },
    { label: '角色权限', value: 'system:role' },
    { label: '数据字典', value: 'system:dict' },
    { label: '操作日志', value: 'system:log' },
  ];

  const columns = [
    { title: '角色编码', key: 'roleCode', width: 130 },
    { title: '角色名称', dataIndex: 'roleName', key: 'roleName', width: 140 },
    { title: '说明', dataIndex: 'remark', key: 'remark', width: 200 },
    { title: '菜单权限', key: 'menuPerms' },
    { title: '操作', key: 'action', width: 100 },
  ];

  const splitPerms = (perms?: string) => (perms || '').split(',').filter(Boolean);
  const permLabel = (value: string) => permOptions.find((p) => p.value === value)?.label || value;

  const loadData = async () => {
    loading.value = true;
    try {
      roles.value = await systemApi.roleList();
    } finally {
      loading.value = false;
    }
  };

  const openModal = (record: any) => {
    current.value = record;
    checked.value = splitPerms(record.menuPerms);
    modalOpen.value = true;
  };

  const handleSave = async () => {
    saving.value = true;
    try {
      await systemApi.savePerm(current.value.id, checked.value.join(','));
      message.success('权限已更新');
      modalOpen.value = false;
      await loadData();
      await userStore.fetchPermsAndMenus();
    } finally {
      saving.value = false;
    }
  };

  onMounted(loadData);
</script>

<style lang="less" scoped>
  .mb-3 {
    margin-bottom: 12px;
  }
</style>
