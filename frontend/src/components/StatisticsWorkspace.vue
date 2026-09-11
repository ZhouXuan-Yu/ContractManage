<script setup lang="ts">
import { onMounted, ref } from 'vue'
type Bucket = { key: string; label: string; count: number; amount: number | null }
type Overview = { total: number; valid: number; draft: number; pendingSign: number; fulfilling: number; completed: number; terminated: number; voided: number; archivedCount: number; amountAvailable: boolean; contractAmountTotal: number; paymentTotal: number; paidTotal: number; paymentPending: number; paymentOverdue: number; milestonePending: number; milestoneOverdue: number; aiPending: number; aiFailed: number; highRisk: number; changeCount: number; lifecycle: Bucket[]; types: Bucket[]; aiRisks: Bucket[] }
const props = defineProps<{ api: string; role: string; userId?: string; orgId?: string; departmentId?: string; embedded?: boolean }>()
const emit = defineEmits<{ drill: [target: 'ledger' | 'fulfillment', status?: string] }>()
const data = ref<Overview | null>(null); const loading = ref(true); const error = ref('')
async function load() {
  loading.value = true; error.value = ''
  try { const response = await fetch(props.api + '/api/statistics/overview', { headers: { 'X-Role': props.role, 'X-User-Id': props.userId || '10001', 'X-Org-Id': props.orgId || '100', 'X-Department-Id': props.departmentId || '101' } }); if (!response.ok) throw new Error('统计数据加载失败'); data.value = await response.json() }
  catch (e) { error.value = (e as Error).message } finally { loading.value = false }
}
function formatAmount(value: number) { return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
onMounted(load)
</script>

<template>
  <section class="statistics-workspace" :class="{ 'page-section': !embedded, 'statistics-embedded': embedded }">
    <div v-if="!embedded" class="page-header"><div><p class="eyebrow">合同统计 / 统计分析</p><h1>统计分析</h1><p class="page-desc">按当前用户可见范围查看合同生命周期、履行收付款与 AI 风险分布。</p></div><div class="header-actions"><span class="sync-note">统计口径：作废合同不计入有效汇总</span><button class="secondary-action" @click="load">刷新数据</button></div></div>
    <div v-if="loading" class="panel loading-state">正在加载统计数据...</div>
    <div v-else-if="error" class="panel error-state"><strong>{{ error }}</strong><button class="secondary-action" @click="load">重新加载</button></div>
    <template v-else-if="data">
      <div v-if="embedded" class="section-heading embedded-heading"><div><h2>经营概览</h2><p class="section-caption">按当前用户可见范围汇总合同生命周期、履行收付款与 AI 风险，点击状态可跳转台账。</p></div><button class="secondary-action" @click="load">刷新数据</button></div>
      <div v-if="!embedded" class="metric-grid statistics-metrics"><button class="metric-card metric-total" @click="emit('drill', 'ledger')"><span>有效合同</span><strong>{{ data.valid }}</strong><small>可见合同总数 {{ data.total }}</small></button><button class="metric-card metric-draft" @click="emit('drill', 'ledger', '草稿')"><span>草稿</span><strong>{{ data.draft }}</strong><small>进入台账继续处理</small></button><button class="metric-card metric-sign" @click="emit('drill', 'ledger', '待签署')"><span>待签署</span><strong>{{ data.pendingSign }}</strong><small>等待签署生效</small></button><button class="metric-card metric-active" @click="emit('drill', 'fulfillment')"><span>履行中</span><strong>{{ data.fulfilling }}</strong><small>进入履行台账查看</small></button></div>
      <div class="statistics-grid"><section class="panel statistics-panel"><div class="section-heading"><div><h2>生命周期分布</h2><p class="section-caption">点击状态可回到合同台账</p></div></div><button v-for="item in data.lifecycle" :key="item.key" class="stat-row" @click="emit('drill', 'ledger', item.label)"><span>{{ item.label }}</span><strong>{{ item.count }}</strong><i class="stat-bar"><b :style="{ width: (data.valid ? Math.min(100, item.count / data.valid * 100) : 0) + '%' }"></b></i><small>查看</small></button></section><section class="panel statistics-panel"><div class="section-heading"><div><h2>履行与收付款</h2><p class="section-caption">来自履行台账的计划数据</p></div></div><div class="stat-summary"><div><span>计划总额</span><strong>{{ formatAmount(data.paymentTotal) }}</strong><small>已完成 {{ formatAmount(data.paidTotal) }}</small></div><div><span>待处理付款</span><strong>{{ data.paymentPending }}</strong><small>逾期 {{ data.paymentOverdue }} 笔</small></div><div><span>履行节点</span><strong>{{ data.milestonePending }}</strong><small>逾期 {{ data.milestoneOverdue }} 项</small></div></div><button class="section-link" @click="emit('drill', 'fulfillment')">进入履行与收付款</button></section></div>
      <div class="statistics-grid"><section class="panel statistics-panel"><div class="section-heading"><div><h2>合同类型分布</h2><p class="section-caption">按当前合同类型数据汇总</p></div></div><div v-if="data.types.length" class="compact-bars"><div v-for="item in data.types" :key="item.key"><span>{{ item.label }}</span><b>{{ item.count }}</b></div></div><div v-else class="inline-empty">暂无合同类型数据</div></section><section class="panel statistics-panel"><div class="section-heading"><div><h2>AI 风险概览</h2><p class="section-caption">AI 结果仅用于提示，不改变合同状态</p></div></div><div class="risk-summary"><div><span>待处理</span><strong>{{ data.aiPending }}</strong></div><div><span>失败</span><strong>{{ data.aiFailed }}</strong></div><div><span>高风险</span><strong>{{ data.highRisk }}</strong></div></div><div v-if="data.aiRisks.length" class="compact-bars"><div v-for="item in data.aiRisks" :key="item.key"><span>{{ item.label }}</span><b>{{ item.count }}</b></div></div><div v-else class="inline-empty">暂无 AI 审查结果</div></section></div>
      <section class="statistics-note"><span class="status-dot"></span><span>金额字段尚未从正式合同主表接入，本页不展示合同金额汇总；收付款金额仅统计已有付款计划。</span></section>
    </template>
  </section>
  <section v-if="data" class="panel statistics-panel"><div class="section-heading"><div><h2>合同金额与变更</h2><p class="section-caption">仅汇总当前权限范围内未归档合同</p></div></div><div class="stat-summary"><div><span>合同金额</span><strong>{{ formatAmount(data.contractAmountTotal) }}</strong></div><div><span>变更记录</span><strong>{{ data.changeCount }}</strong></div><div><span>已归档合同</span><strong>{{ data.archivedCount }}</strong></div><div><span>实际收付款</span><strong>{{ formatAmount(data.paidTotal) }}</strong></div></div></section>
</template>
