<template>
  <div>
    <a-card :bordered="false" class="mb-3">
      <a-form layout="inline">
        <a-form-item label="选择产品">
          <a-select
            v-model:value="productId"
            show-search
            option-filter-prop="label"
            :options="productOptions"
            placeholder="请选择成品物料"
            style="width: 340px"
            @change="loadConfig"
          />
        </a-form-item>
      </a-form>
    </a-card>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="12">
        <a-card :bordered="false" title="物料清单 BOM">
          <template #extra>
            <a-space>
              <a-button size="small" @click="addBom">添加行</a-button>
              <a-button size="small" type="primary" :disabled="!productId" @click="saveBom">
                保存 BOM
              </a-button>
            </a-space>
          </template>
          <a-table
            :columns="bomColumns"
            :data-source="bomItems"
            :pagination="false"
            row-key="_key"
            size="small"
            :locale="{ emptyText: '请选择产品后维护 BOM' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'component'">
                <a-select
                  v-model:value="record.componentMaterialId"
                  show-search
                  option-filter-prop="label"
                  :options="componentOptions"
                  placeholder="选择原材料/半成品"
                  size="small"
                  style="width: 100%"
                />
              </template>
              <template v-else-if="column.key === 'quantity'">
                <a-input-number v-model:value="record.quantity" :min="0.01" size="small" style="width: 100%" />
              </template>
              <template v-else-if="column.key === 'remark'">
                <a-input v-model:value="record.remark" size="small" placeholder="选填" />
              </template>
              <template v-else-if="column.key === 'action'">
                <a class="danger-text" @click="bomItems.splice(bomItems.indexOf(record), 1)">删除</a>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="12">
        <a-card :bordered="false" title="工序模板">
          <template #extra>
            <a-space>
              <a-button size="small" @click="addTpl">添加工序</a-button>
              <a-button size="small" type="primary" :disabled="!productId" @click="saveTpl">
                保存模板
              </a-button>
            </a-space>
          </template>
          <a-table
            :columns="tplColumns"
            :data-source="tplItems"
            :pagination="false"
            row-key="_key"
            size="small"
            :locale="{ emptyText: '请选择产品后维护工序模板' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'processSeq'">
                <a-input-number v-model:value="record.processSeq" :min="1" size="small" style="width: 100%" />
              </template>
              <template v-else-if="column.key === 'processName'">
                <a-input v-model:value="record.processName" size="small" placeholder="工序名称" />
              </template>
              <template v-else-if="column.key === 'requirement'">
                <a-input v-model:value="record.requirement" size="small" placeholder="工艺要求" />
              </template>
              <template v-else-if="column.key === 'action'">
                <a class="danger-text" @click="tplItems.splice(tplItems.indexOf(record), 1)">删除</a>
              </template>
            </template>
          </a-table>
          <a-alert
            class="mt-3"
            type="info"
            show-icon
            message="新建生产工单时会按此处工序顺序复制工序，操作工按顺序逐工序报工。"
          />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import { message } from 'ant-design-vue';
  import { commonApi, warehouseApi, workOrderApi } from '@/api/stockwork';

  let keySeed = 1;
  const nextKey = () => `row-${keySeed++}`;

  const productId = ref<number | undefined>();
  const productOptions = ref<{ label: string; value: number }[]>([]);
  const componentOptions = ref<{ label: string; value: number }[]>([]);

  const bomItems = ref<any[]>([]);
  const tplItems = ref<any[]>([]);

  const bomColumns = [
    { title: '组成物料', key: 'component' },
    { title: '单位用量', key: 'quantity', width: 110 },
    { title: '备注', key: 'remark', width: 150 },
    { title: '操作', key: 'action', width: 60 },
  ];

  const tplColumns = [
    { title: '序号', key: 'processSeq', width: 80 },
    { title: '工序名称', key: 'processName', width: 140 },
    { title: '工艺要求', key: 'requirement' },
    { title: '操作', key: 'action', width: 60 },
  ];

  const loadConfig = async () => {
    if (!productId.value) return;
    const [bom, tpl] = await Promise.all([
      workOrderApi.bom(productId.value),
      workOrderApi.template(productId.value),
    ]);
    bomItems.value = bom.map((b: any) => ({
      _key: nextKey(),
      componentMaterialId: b.componentMaterialId,
      quantity: b.quantity,
      remark: b.remark || '',
    }));
    tplItems.value = tpl.map((t: any) => ({
      _key: nextKey(),
      processSeq: t.processSeq,
      processName: t.processName,
      requirement: t.requirement || '',
    }));
  };

  const addBom = () =>
    bomItems.value.push({ _key: nextKey(), componentMaterialId: undefined, quantity: 1, remark: '' });

  const addTpl = () =>
    tplItems.value.push({
      _key: nextKey(),
      processSeq: tplItems.value.length + 1,
      processName: '',
      requirement: '',
    });

  const saveBom = async () => {
    const items = bomItems.value.filter((b) => b.componentMaterialId && b.quantity);
    await workOrderApi.saveBom({ productMaterialId: productId.value, items });
    message.success('BOM 已保存');
    loadConfig();
  };

  const saveTpl = async () => {
    const items = tplItems.value.filter((t) => t.processName?.trim());
    if (!items.length) return message.warning('请至少维护一道工序');
    await workOrderApi.saveTemplate({ materialId: productId.value, items });
    message.success('工序模板已保存');
    loadConfig();
  };

  onMounted(async () => {
    const [products, materials] = await Promise.all([
      commonApi.product(),
      warehouseApi.options(),
    ]);
    productOptions.value = products.map((p) => ({
      label: `[${p.baseSite}] ${p.materialCode} ${p.materialName}`,
      value: p.id,
    }));
    componentOptions.value = materials
      .filter((m) => (m.materialType || 1) !== 3)
      .map((m) => ({
        label: `${m.materialCode} ${m.materialName}`,
        value: m.id!,
      }));
  });
</script>

<style lang="less" scoped>
  .mb-3 {
    margin-bottom: 16px;
  }

  .mt-3 {
    margin-top: 12px;
  }

  .danger-text {
    color: #d4380d;
  }
</style>
