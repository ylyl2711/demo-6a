<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { showSuccessToast, showToast, type UploaderFileListItem } from 'vant'
import { submitReport, uploadFile } from '@/api'
import type { ProcessItem, WorkOrderDetail } from '@/types'

const props = defineProps<{
  show: boolean
  order: WorkOrderDetail | null
  process: ProcessItem | null
}>()

const emit = defineEmits<{
  'update:show': [value: boolean]
  success: []
}>()

const visible = computed({
  get: () => props.show,
  set: (value: boolean) => emit('update:show', value)
})

const goodNum = ref('')
const badNum = ref('0')
const badReasonId = ref<number | null>(null)
const fileList = ref<UploaderFileListItem[]>([])
const submitting = ref(false)

const badCount = computed(() => {
  const num = Number(badNum.value)
  return Number.isFinite(num) && num > 0 ? num : 0
})

watch(
  () => props.show,
  (show) => {
    if (show) resetForm()
  }
)

function resetForm() {
  const report = props.process?.report
  goodNum.value = report ? String(report.goodNum) : String(props.order?.planNum ?? '')
  badNum.value = report ? String(report.badNum) : '0'
  badReasonId.value = report?.badReasonDictId ?? null
  fileList.value = report?.photoUrl ? [{ url: report.photoUrl, isImage: true }] : []
  submitting.value = false
}

async function afterRead(item: UploaderFileListItem | UploaderFileListItem[]) {
  const single = Array.isArray(item) ? item[0] : item
  if (!single?.file) return
  single.status = 'uploading'
  single.message = '上传中'
  try {
    const result = await uploadFile(single.file)
    single.status = 'done'
    single.message = ''
    single.url = result.url
  } catch {
    single.status = 'failed'
    single.message = '上传失败'
    fileList.value = []
  }
}

function resolvePhotoUrl() {
  const url = fileList.value[0]?.url ?? ''
  return url.startsWith('/uploads') ? url : ''
}

async function onSubmit() {
  if (!props.order || !props.process) return
  const good = Number(goodNum.value)
  const bad = Number(badNum.value)
  if (!Number.isFinite(good) || good < 0) {
    showToast('请输入正确的良品数')
    return
  }
  if (!Number.isFinite(bad) || bad < 0) {
    showToast('请输入正确的不良数')
    return
  }
  if (good === 0 && bad === 0) {
    showToast('良品数和不良数不能同时为 0')
    return
  }
  if (bad > 0 && badReasonId.value === null) {
    showToast('不良数大于 0 时必须选择不良原因')
    return
  }

  submitting.value = true
  try {
    await submitReport({
      workOrderId: props.order.id,
      processId: props.process.id,
      goodNum: good,
      badNum: bad,
      badReasonId: bad > 0 ? badReasonId.value : null,
      photoUrl: resolvePhotoUrl()
    })
    showSuccessToast('报工提交成功')
    visible.value = false
    emit('success')
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <van-popup
    v-model:show="visible"
    position="bottom"
    round
    :style="{ maxHeight: '88vh' }"
    class="report-popup"
  >
    <div class="popup-header">
      <div>
        <div class="popup-title">工序报工</div>
        <div class="popup-sub">
          {{ process?.processName }} · {{ order?.workOrderNo }}
        </div>
      </div>
      <van-icon name="cross" class="popup-close" @click="visible = false" />
    </div>

    <van-form class="popup-form" @submit="onSubmit">
      <van-cell-group inset>
        <van-field
          v-model="goodNum"
          type="number"
          label="良品数"
          placeholder="请输入良品数量"
        />
        <van-field
          v-model="badNum"
          type="number"
          label="不良数"
          placeholder="请输入不良数量"
        />
      </van-cell-group>

      <div v-if="badCount > 0" class="reason-block">
        <div class="block-title">不良原因<span class="required">*</span></div>
        <van-radio-group v-model="badReasonId" class="reason-list">
          <van-radio
            v-for="reason in order?.badReasons ?? []"
            :key="reason.id"
            :name="reason.id"
            class="reason-item"
          >
            {{ reason.label }}
          </van-radio>
        </van-radio-group>
        <div v-if="!(order?.badReasons ?? []).length" class="reason-empty">
          暂无不良原因选项，请联系管理员配置
        </div>
      </div>

      <div class="upload-block">
        <div class="block-title">
          现场照片<span class="optional">（选填，最多 1 张）</span>
        </div>
        <van-uploader
          v-model="fileList"
          :max-count="1"
          :preview-image="true"
          accept="image/*"
          :after-read="afterRead"
        />
      </div>

      <div class="popup-footer">
        <van-button
          block
          round
          type="primary"
          native-type="submit"
          :loading="submitting"
          loading-text="提交中..."
        >
          提交报工
        </van-button>
      </div>
    </van-form>
  </van-popup>
</template>

<style scoped>
.report-popup {
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
}

.popup-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 18px 20px 12px;
}

.popup-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d2333;
}

.popup-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #9aa3b5;
}

.popup-close {
  font-size: 18px;
  color: #b3bbcb;
  padding: 4px;
}

.popup-form {
  padding: 0 4px;
}

.popup-form :deep(.van-cell-group--inset) {
  margin: 0 12px;
  border-radius: 12px;
  background: #f4f6fb;
  overflow: hidden;
}

.popup-form :deep(.van-field__label) {
  width: 62px;
  color: #5a6478;
}

.block-title {
  padding: 16px 20px 8px;
  font-size: 13px;
  font-weight: 600;
  color: #333c4e;
}

.block-title .required {
  color: #e35d6a;
  margin-left: 2px;
}

.block-title .optional {
  font-weight: 400;
  font-size: 11px;
  color: #a9b2c4;
  margin-left: 2px;
}

.reason-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  padding: 2px 20px 6px;
}

.reason-item {
  margin-bottom: 6px;
  font-size: 13px;
}

.reason-empty {
  padding: 0 20px 8px;
  font-size: 12px;
  color: #8a93a6;
}

.upload-block {
  padding-bottom: 8px;
}

.upload-block :deep(.van-uploader) {
  padding: 0 20px;
}

.upload-block :deep(.van-uploader__upload) {
  border-radius: 12px;
  background: #f4f6fb;
  border: 1px dashed #d7e1f6;
}

.upload-block :deep(.van-uploader__preview-image) {
  border-radius: 12px;
}

.popup-footer {
  padding: 16px 20px 12px;
}

.popup-footer :deep(.van-button) {
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
}
</style>
