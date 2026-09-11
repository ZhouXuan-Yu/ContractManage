<script setup lang="ts">
import { ref, watch } from 'vue'

type Detail = { id: number; name: string; totalAmount: number | null; currency: string | null; paymentDirection: string | null; signDate: string | null; effectiveDate: string | null; expireDate: string | null }
const props = defineProps<{ api: string; detail: Detail; role: string; userId: string; orgId: string; departmentId: string }>()
const emit = defineEmits<{ updated: [] }>()
const extraction = ref<any | null>(null)
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const paymentPlans = ref<any[]>([])
const fulfillmentItems = ref<any[]>([])
const form = ref({ contractName: '', totalAmount: '', currency: 'CNY', paymentDirection: '', signDate: '', effectiveDate: '', expireDate: '' })

function headers() { return { 'Content-Type': 'application/json', 'X-Role': props.role, 'X-User-Id': props.userId, 'X-Org-Id': props.orgId, 'X-Department-Id': props.departmentId } }
async function load() {
  loading.value = true; message.value = ''
  try {
    const response = await fetch(`${props.api}/api/contracts/${props.detail.id}/ai/extract/latest`, { headers: headers() })
    if (!response.ok) throw new Error((await response.text()) || '暂无解析结果')
    extraction.value = await response.json()
    const recommendations = await fetch(`${props.api}/api/contracts/${props.detail.id}/ai/extract/fulfillment-recommendations`, { headers: headers() })
    if (recommendations.ok) {
      const data = await recommendations.json()
      paymentPlans.value = data.paymentPlans || []
      fulfillmentItems.value = data.fulfillmentItems || []
    }
    const basic = extraction.value?.basic_info || {}
    form.value = { contractName: basic.contract_name || props.detail.name || '', totalAmount: basic.total_amount == null ? '' : String(basic.total_amount), currency: basic.currency === '人民币' ? 'CNY' : (basic.currency || props.detail.currency || 'CNY'), paymentDirection: basic.payment_direction || '', signDate: basic.sign_date || '', effectiveDate: basic.effective_date || '', expireDate: basic.expire_date || '' }
  } catch (error) { extraction.value = null; message.value = (error as Error).message }
  finally { loading.value = false }
}
async function confirm() {
  saving.value = true; message.value = ''
  try {
    const response = await fetch(`${props.api}/api/contracts/${props.detail.id}/ai/extract/confirm`, { method: 'POST', headers: headers(), body: JSON.stringify({ ...form.value, confirm: true, paymentPlans: paymentPlans.value, fulfillmentItems: fulfillmentItems.value }) })
    if (!response.ok) throw new Error((await response.text()) || '确认失败')
    message.value = 'AI 解析字段已确认并写入合同'
    emit('updated')
  } catch (error) { message.value = (error as Error).message }
  finally { saving.value = false }
}
async function revoke() {
  saving.value = true; message.value = ''
  try {
    const response = await fetch(`${props.api}/api/contracts/${props.detail.id}/ai/extract/fulfillment-revoke`, { method: 'POST', headers: headers() })
    if (!response.ok) throw new Error((await response.text()) || '撤销失败')
    message.value = '本次 AI 写入的待处理履行项已撤销，历史记录已保留'
    emit('updated')
  } catch (error) { message.value = (error as Error).message }
  finally { saving.value = false }
}
watch(() => props.detail.id, load, { immediate: true })
</script>

<template>
  <section class="ai-extraction-panel">
    <div class="ai-extraction-head">
      <div><p class="ai-kicker">AI DATA REVIEW / EXTRACT-V1</p><h2>解析结果确认</h2><p>先核对 AI 识别值，再写入合同正式字段。无法识别为标准日期的内容会保留为待确认信息。</p></div>
      <div class="ai-signal"><span></span><b>{{ extraction ? 'READY FOR REVIEW' : 'WAITING FOR RESULT' }}</b></div>
    </div>
    <div v-if="loading" class="ai-loading"><span class="ai-loader"></span>正在读取解析结果</div>
    <div v-else-if="extraction" class="ai-extraction-body">
      <div class="ai-field-matrix">
        <label>合同名称<input v-model="form.contractName" /></label>
        <label>合同金额<input v-model="form.totalAmount" inputmode="decimal" /></label>
        <label>币种<select v-model="form.currency"><option value="CNY">CNY / 人民币</option><option value="USD">USD / 美元</option><option value="EUR">EUR / 欧元</option></select></label>
        <label>收付方向<input v-model="form.paymentDirection" placeholder="AI 识别结果" /></label>
        <label>签署日期<input v-model="form.signDate" type="date" /></label>
        <label>生效日期<input v-model="form.effectiveDate" type="date" /></label>
        <label>到期日期<input v-model="form.expireDate" type="date" /></label>
      </div>
      <div class="ai-source-strip"><span class="ai-source-icon">AI</span><span><b>识别摘要</b><small>置信度 {{ extraction.overall_confidence ?? '未提供' }} · 条款 {{ extraction.stats?.clause_count ?? 0 }} · 付款计划 {{ extraction.stats?.payment_plan_count ?? 0 }}</small></span></div>
      <div v-if="paymentPlans.length || fulfillmentItems.length" class="ai-confirm-grid">
        <div v-if="paymentPlans.length" class="ai-confirm-list"><div class="ai-list-heading"><b>付款计划确认</b><small>勾选后写入履行与收付款</small></div><label v-for="plan in paymentPlans" :key="`payment-${plan.phaseNo}`" class="ai-confirm-row"><input v-model="plan.selected" type="checkbox" /><span><input v-model="plan.planName" aria-label="付款计划名称" /><small>{{ plan.triggerCondition || '无触发条件' }}</small></span><input v-model="plan.amount" class="ai-mini-input" inputmode="decimal" aria-label="计划金额" placeholder="金额" /><input v-model="plan.dueDate" class="ai-date-input" type="date" aria-label="计划日期" /></label></div>
        <div v-if="fulfillmentItems.length" class="ai-confirm-list"><div class="ai-list-heading"><b>履行节点确认</b><small>勾选后写入履行计划</small></div><label v-for="item in fulfillmentItems" :key="`item-${item.phaseNo}`" class="ai-confirm-row"><input v-model="item.selected" type="checkbox" /><span><input v-model="item.name" aria-label="履行节点名称" /><small>{{ item.description || '无补充说明' }}</small></span><input v-model="item.dueDate" class="ai-date-input" type="date" aria-label="节点日期" /></label></div>
      </div>
      <div class="ai-extraction-footer"><span>{{ message || '确认后将写入合同主表，并记录操作日志' }}</span><div class="ai-footer-actions"><button class="ai-revoke-button" :disabled="saving" @click="revoke">撤销本次 AI 写入</button><button class="ai-confirm-button" :disabled="saving" @click="confirm">{{ saving ? '写入中...' : '确认并写入正式字段' }}</button></div></div>
    </div>
    <div v-else class="ai-empty"><strong>尚未生成解析结果</strong><span>{{ message || '请先在右侧发起合同解析' }}</span></div>
  </section>
</template>
