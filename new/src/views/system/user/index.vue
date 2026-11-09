<template>
  <div>
    <a-card :bordered="false">
      <a-form layout="inline" class="filter-bar">
        <a-form-item label="关键词">
          <a-input
            v-model:value="query.keyword"
            allow-clear
            placeholder="用户名 / 姓名"
            style="width: 200px"
            @press-enter="loadData(1)"
          />
        </a-form-item>
        <a-form-item label="角色">
          <a-select
            v-model:value="query.roleId"
            :options="roleOptions"
            allow-clear
            placeholder="全部角色"
            style="width: 150px"
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
          <template #icon><Icon icon="ant-design:user-add-outlined" /></template>
          新增用户
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
          <template v-if="column.key === 'roleName'">
            <a-tag :color="record.roleCode === 'ADMIN' ? 'purple' : record.roleCode === 'BUSINESS' ? 'blue' : 'green'">
              {{ record.roleName }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'default'" :text="record.status === 1 ? '启用' : '停用'" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openModal(record)">编辑</a>
              <a @click="toggleStatus(record)">{{ record.status === 1 ? '停用' : '启用' }}</a>
              <a-popconfirm v-if="record.roleCode !== 'ADMIN'" title="确认删除该用户？" @confirm="removeUser(record.id)">
                <a class="danger-text">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="form.id ? '编辑用户' : '新增用户'"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form layout="vertical" :model="form">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="用户名" required>
              <a-input v-model:value="form.username" :disabled="!!form.id" placeholder="登录账号" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="姓名" required>
              <a-input v-model:value="form.realName" placeholder="真实姓名" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="角色" required>
              <a-select v-model:value="form.roleId" :options="roleOptions" placeholder="请选择角色" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item :label="form.id ? '密码（留空不修改）' : '初始密码'">
              <a-input-password v-model:value="form.password" placeholder="默认 123456" />
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
  import { systemApi } from '@/api/stockwork';

  const loading = ref(false);
  const saving = ref(false);
  const list = ref<any[]>([]);
  const roleOptions = ref<{ label: string; value: number }[]>([]);
  const query = reactive({
    keyword: '',
    roleId: undefined as number | undefined,
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
    { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
    { title: '用户名', dataIndex: 'username', key: 'username', width: 130 },
    { title: '姓名', dataIndex: 'realName', key: 'realName', width: 110 },
    { title: '角色', key: 'roleName', width: 140 },
    { title: '状态', key: 'status', width: 100 },
    { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
    { title: '操作', key: 'action', width: 180 },
  ];

  const form = reactive({
    id: undefined as number | undefined,
    username: '',
    realName: '',
    roleId: undefined as number | undefined,
    password: '',
  });
  const modalOpen = ref(false);

  const loadData = async (page = query.pageNum) => {
    query.pageNum = page;
    loading.value = true;
    try {
      const data = await systemApi.userList({ ...query });
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
    query.keyword = '';
    query.roleId = undefined;
    loadData(1);
  };

  const openModal = (record?: any) => {
    Object.assign(form, {
      id: undefined,
      username: '',
      realName: '',
      roleId: undefined,
      password: '',
      ...(record
        ? { id: record.id, username: record.username, realName: record.realName, roleId: record.roleId }
        : {}),
    });
    modalOpen.value = true;
  };

  const handleSave = async () => {
    if (!form.username.trim() || !form.realName.trim() || !form.roleId) {
      return message.warning('请完整填写用户名、姓名与角色');
    }
    saving.value = true;
    try {
      await systemApi.saveUser({ ...form });
      message.success('保存成功');
      modalOpen.value = false;
      loadData();
    } finally {
      saving.value = false;
    }
  };

  const toggleStatus = async (record: any) => {
    await systemApi.userStatus(record.id, record.status === 1 ? 0 : 1);
    message.success('状态已更新');
    loadData();
  };

  const removeUser = async (id: number) => {
    await systemApi.deleteUser(id);
    message.success('删除成功');
    loadData();
  };

  onMounted(async () => {
    const roles = await systemApi.roleList();
    roleOptions.value = roles.map((r: any) => ({ label: r.roleName, value: r.id }));
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

  .danger-text {
    color: #d4380d;
  }
</style>
