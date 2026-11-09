<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showConfirmDialog, showImagePreview, showSuccessToast } from 'vant'
import { finishOrder, getOrderDetail } from '@/api'
import type { ProcessItem, WorkOrderDetail } from '@/types'
import { formatNum, statusText } from '@/utils/display'
import ReportPopup from '@/components/ReportPopup.vue'

const route = useRoute()
const router = useRouter()

const orderId = computed(() => Number(route.params.id))
const detail = ref<WorkOrderDetail | null>(null)
const loading = ref(true)
const finishing = ref(false)
const reportVisible = ref(false)
const currentProcess = ref<ProcessItem | null>(null)

const progressPercent = computed(() => {
  const summary = detail.value?.summary
  if (!summary || !summary.totalProcess) return 0
  return Math.round((summary.doneProcess / summary.totalProcess) * 100)
})

const showFinish = computed(() => {
  const data = detail.value
  if (!data) return false
  if (data.workStatus === 2 || data.workStatus === 3) return false
  return data.summary.totalProcess > 0 && data.summary.doneProcess === data.summary.totalProcess
})

async function loadDetail() {
  try {
    detail.value = await getOrderDetail(orderId.value)
  } catch {
    // 错误提示由请求层统一处理
  } finally {
    loading.value = false
  }
}

function prevDone(index: number) {
  const processes = detail.value?.processes ?? []
  return processes.slice(0, index).every((item) => item.processStatus === 2)
}

function canReport(index: number) {
  const data = detail.value
  const process = data?.processes[index]
  if (!data || !process) return false
  if (data.workStatus === 2 || data.workStatus === 3) return false
  return prevDone(index)
}

function openReport(process: ProcessItem) {
  currentProcess.value = process
  reportVisible.value = true
}

function previewPhoto(url: string) {
  if (url) showImagePreview([url])
}

function onFinish() {
  showConfirmDialog({
    title: '提交完工申请',
    message: '提交后工单将进入待质检状态，报工记录会被锁定，确认提交吗？',
    confirmButtonText: '确认提交',
    cancelButtonText: '再想想'
  })
    .then(async () => {
      if (!detail.value) return
      finishing.value = true
      try {
        await finishOrder(detail.value.id)
        showSuccessToast('已提交，等待质检')
        await loadDetail()
      } catch {
        // 错误提示由请求层统一处理
      } finally {
        finishing.value = false
      }
    })
    .catch(() => {})
}

onMounted(loadDetail)
</script>

<template>
  <div class="page detail-page">
    <header class="brand-hero detail-hero">
      <div class="hero-bar">
        <van-icon name="arrow-left" class="back" @click="router.back()" />
        <span class="hero-title">工单详情</span>
        <span class="hero-placeholder" />
      </div>

      <template v-if="detail">
        <div class="hero-head">
          <div class="hero-info">
            <div class="hero-no">{{ detail.workOrderNo }}</div>
            <div class="hero-product">{{ detail.materialName }}</div>
          </div>
          <van-tag round class="hero-status">{{ statusText(detail.workStatus) }}</van-tag>
        </div>

        <div class="hero-stats">
          <div class="hs">
            <b>{{ detail.summary.doneProcess }}/{{ detail.summary.totalProcess }}</b>
            <span>工序进度</span>
          </div>
          <div class="hs">
            <b>{{ formatNum(detail.summary.goodTotal) }}</b>
            <span>良品</span>
          </div>
          <div class="hs">
            <b>{{ formatNum(detail.summary.badTotal) }}</b>
            <span>不良</span>
          </div>
        </div>

        <div class="hero-progress">
          <van-progress
            :percentage="progressPercent"
            :show-pivot="false"
            stroke-width="4"
            color="#ffffff"
            track-color="rgba(255,255,255,0.28)"
          />
        </div>
      </template>
    </header>

    <div v-if="loading" class="page-loading">
      <van-loading size="24" />
      正在加载工单详情...
    </div>

    <template v-else-if="detail">
      <div class="detail-body" :class="{ 'has-bottom': showFinish }">
        <div class="panel info-panel">
          <div class="info-grid">
            <div class="info-item">
              <span>生产基地</span>
              <b>{{ detail.baseSite }}</b>
            </div>
            <div class="info-item">
              <span>物料编码</span>
              <b>{{ detail.materialCode }}</b>
            </div>
            <div class="info-item">
              <span>计划数量</span>
              <b>{{ formatNum(detail.planNum) }}</b>
            </div>
            <div class="info-item">
              <span>产品批次</span>
              <b>{{ detail.productBatch }}</b>
            </div>
            <div class="info-item">
              <span>操作工</span>
              <b>{{ detail.operatorName }}</b>
            </div>
            <div class="info-item">
              <span>创建时间</span>
              <b>{{ detail.createTime }}</b>
            </div>
          </div>
        </div>

        <div class="panel process-panel">
          <div class="panel-title">工序流程</div>
          <div class="process-list">
            <div
              v-for="(process, index) in detail.processes"
              :key="process.id"
              class="process-item"
              :class="{ done: process.processStatus === 2 }"
            >
              <div class="step-index">{{ process.processSeq }}</div>
              <div class="process-main">
                <div class="process-head">
                  <span class="process-name">{{ process.processName }}</span>
                  <van-tag
                    plain
                    round
                    :type="process.processStatus === 2 ? 'success' : 'warning'"
                  >
                    {{ process.processStatus === 2 ? '已报工' : '未报工' }}
                  </van-tag>
                </div>
                <div class="process-req">
                  工艺要求：{{ process.requirement || '—' }}
                </div>

                <div v-if="process.report" class="report-box">
                  <div class="report-line">
                    <span>
                      良品 <b>{{ formatNum(process.report.goodNum) }}</b>
                    </span>
                    <span>
                      不良 <b class="bad">{{ formatNum(process.report.badNum) }}</b>
                    </span>
                    <span v-if="process.report.badReason" class="reason">
                      原因：{{ process.report.badReason }}
                    </span>
                  </div>
                  <div v-if="process.report.reportTime" class="report-time">
                    报工时间：{{ process.report.reportTime }}
                  </div>
                  <div
                    v-if="process.report.photoUrl"
                    class="photo-thumb"
                    @click="previewPhoto(process.report.photoUrl)"
                  >
                    <img :src="process.report.photoUrl" alt="现场照片" />
                  </div>
                </div>
                <div
                  v-else-if="process.processStatus === 2"
                  class="report-box report-missing"
                >
                  该工序已报工，报工明细暂无
                </div>

                <div v-if="canReport(index)" class="process-actions">
                  <van-button
                    size="small"
                    round
                    type="primary"
                    :plain="process.processStatus !== 2"
                    @click="openReport(process)"
                  >
                    {{ process.processStatus === 2 ? '修改报工' : '报工' }}
                  </van-button>
                </div>
                <div
                  v-else-if="process.processStatus === 1 && !prevDone(index)"
                  class="process-lock"
                >
                  需先完成前序工序
                </div>
              </div>
            </div>
          </div>

          <div
            v-if="detail.workStatus === 2 || detail.workStatus === 3"
            class="process-tip"
          >
            工单已{{ statusText(detail.workStatus) }}，报工记录已锁定
          </div>
        </div>
      </div>

      <div v-if="showFinish" class="bottom-bar">
        <van-button
          block
          round
          type="primary"
          :loading="finishing"
          loading-text="提交中..."
          @click="onFinish"
        >
          提交完工申请
        </van-button>
      </div>

      <ReportPopup
        v-model:show="reportVisible"
        :order="detail"
        :process="currentProcess"
        @success="loadDetail"
      />
    </template>

    <van-empty v-else description="工单加载失败">
      <van-button size="small" round type="primary" plain @click="loadDetail">
        重新加载
      </van-button>
    </van-empty>
  </div>
</template>

<style scoped>
.detail-page {
  background: #f2f5fb;
}

.detail-hero {
  padding: calc(12px + env(safe-area-inset-top)) 18px 18px;
  border-radius: 0 0 22px 22px;
}

.hero-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hero-bar .back {
  font-size: 20px;
  color: #fff;
  padding: 2px 0;
}

.hero-title {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.hero-placeholder {
  width: 20px;
}

.hero-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-top: 18px;
}

.hero-info {
  min-width: 0;
}

.hero-no {
  font-size: 12px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.72);
  letter-spacing: 0.4px;
}

.hero-product {
  margin-top: 6px;
  font-size: 19px;
  font-weight: 600;
  color: #fff;
  line-height: 1.3;
}

.hero-status {
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.18);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
}

.hero-stats {
  display: flex;
  margin-top: 16px;
  padding: 12px 0;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.13);
}

.hero-stats .hs {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.hero-stats .hs + .hs {
  border-left: 1px solid rgba(255, 255, 255, 0.16);
}

.hero-stats .hs b {
  font-size: 17px;
  font-weight: 600;
  color: #fff;
}

.hero-stats .hs span {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.75);
}

.hero-progress {
  margin-top: 14px;
}

.detail-body {
  padding: 12px 0 24px;
}

.detail-body.has-bottom {
  padding-bottom: calc(96px + env(safe-area-inset-bottom));
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 8px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.info-item span {
  font-size: 11px;
  color: #9aa3b5;
}

.info-item b {
  font-size: 13px;
  font-weight: 500;
  color: #333c4e;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2333;
  margin-bottom: 16px;
}

.process-list {
  padding-left: 2px;
}

.process-item {
  position: relative;
  padding: 0 0 20px 42px;
}

.process-item::before {
  content: '';
  position: absolute;
  left: 14px;
  top: 32px;
  bottom: 2px;
  width: 2px;
  background: #e8eefb;
}

.process-item:last-child {
  padding-bottom: 4px;
}

.process-item:last-child::before {
  display: none;
}

.step-index {
  position: absolute;
  left: 0;
  top: 0;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #eef2fa;
  color: #9aa3b5;
  font-size: 13px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.process-item.done .step-index {
  background: rgba(34, 88, 232, 0.12);
  color: #2258e8;
}

.process-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.process-name {
  font-size: 15px;
  font-weight: 600;
  color: #1d2333;
}

.process-req {
  margin-top: 5px;
  font-size: 12px;
  color: #8a93a6;
  line-height: 1.5;
}

.report-box {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f4f6fb;
}

.report-line {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px 16px;
  font-size: 12px;
  color: #8a93a6;
}

.report-line b {
  font-size: 14px;
  color: #333c4e;
}

.report-line b.bad {
  color: #e35d6a;
}

.report-line .reason {
  color: #7b869c;
}

.report-time {
  margin-top: 6px;
  font-size: 11px;
  color: #a9b2c4;
}

.report-missing {
  font-size: 12px;
  color: #adb6c7;
}

.photo-thumb {
  margin-top: 10px;
  width: 72px;
  height: 72px;
  border-radius: 10px;
  overflow: hidden;
  background: #e8eefb;
}

.photo-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.process-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.process-lock {
  margin-top: 8px;
  font-size: 11px;
  color: #adb6c7;
}

.process-tip {
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px dashed #e8eefb;
  font-size: 12px;
  color: #9aa3b5;
  text-align: center;
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  max-width: 480px;
  margin: 0 auto;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -6px 20px rgba(23, 54, 200, 0.1);
  z-index: 10;
}
</style>
