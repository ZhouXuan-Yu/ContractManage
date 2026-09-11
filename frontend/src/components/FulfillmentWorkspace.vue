<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

type Contract = { id: number; name: string; contractNo: string; partyName: string; status: string }
type Payment = { id: number; itemName: string; direction: string; amount: number; dueDate: string; status: string; paidAt?: string }
type PaymentRecord = { id: number; paymentPlanId: number; amount: number; paymentDate: string; status: string; remark?: string }
type Attachment = { id: number; fileName: string; bizAttachType?: number }
type Milestone = { id: number; name: string; dueDate: string; status: string }
type ContractWork = Contract & { payments: Payment[]; milestones: Milestone[]; attachments: Attachment[] }

const props = defineProps<{ api: string; role: string; userId?: string; orgId?: string; departmentId?: string }>()
const rows = ref<ContractWork[]>([])
const selectedId = ref<number | null>(null)
const loading = ref(false)
const error = ref('')
const paymentForm = reactive({ itemName: '', direction: 'PAY', amount: '', dueDate: '' })
const milestoneForm = reactive({ name: '', dueDate: '' })
const recordForm = reactive({ paymentId: null as number | null, amount: '', paymentDate: '', voucherAttachmentId: null as number | null, remark: '' })
const paymentRecords = ref<PaymentRecord[]>([])
const headers = () => ({ 'Content-Type': 'application/json', 'X-Role': props.role, 'X-User-Id': props.userId || '10001', 'X-Org-Id': props.orgId || '100', 'X-Department-Id': props.departmentId || '101' })

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${props.api}${path}`, { ...options, headers: { ...headers(), ...(options.headers || {}) } })
  if (!response.ok) throw new Error((await response.text()) || '请求失败')
  return response.json()
}
async function load() {
  loading.value = true; error.value = ''
  try {
    const contracts = (await request<Contract[]>('/api/contracts')).filter(item => item.status === '履行中')
    rows.value = await Promise.all(contracts.map(async item => ({
      ...item,
      payments: await request<Payment[]>(`/api/contracts/${item.id}/payments`),
      milestones: await request<Milestone[]>(`/api/contracts/${item.id}/milestones`),
      attachments: (await request<{ attachments: Attachment[] }>(`/api/contracts/${item.id}`)).attachments
    })))
    if (!selectedId.value || !rows.value.some(item => item.id === selectedId.value)) selectedId.value = rows.value[0]?.id || null
  } catch (e) { error.value = (e as Error).message } finally { loading.value = false }
}
function selected() { return rows.value.find(item => item.id === selectedId.value) }
async function addPayment() {
  const row = selected(); if (!row) return
  if (!paymentForm.itemName || !paymentForm.amount || !paymentForm.dueDate) { error.value = '请完整填写收付款计划'; return }
  try { await request(`/api/contracts/${row.id}/payments`, { method: 'POST', body: JSON.stringify({ ...paymentForm, amount: Number(paymentForm.amount) }) }); Object.assign(paymentForm, { itemName: '', direction: 'PAY', amount: '', dueDate: '' }); await load() } catch (e) { error.value = (e as Error).message }
}
async function addMilestone() {
  const row = selected(); if (!row) return
  if (!milestoneForm.name || !milestoneForm.dueDate) { error.value = '请完整填写履行节点'; return }
  try { await request(`/api/contracts/${row.id}/milestones`, { method: 'POST', body: JSON.stringify({ ...milestoneForm }) }); Object.assign(milestoneForm, { name: '', dueDate: '' }); await load() } catch (e) { error.value = (e as Error).message }
}
async function paymentStatus(payment: Payment, status: string) { const row = selected(); if (!row) return; try { await request(`/api/contracts/${row.id}/payments/${payment.id}/status?status=${status}`, { method: 'POST' }); await load() } catch (e) { error.value = (e as Error).message } }
async function completeMilestone(item: Milestone) { const row = selected(); if (!row) return; try { await request(`/api/contracts/${row.id}/milestones/${item.id}/complete`, { method: 'POST' }); await load() } catch (e) { error.value = (e as Error).message } }
async function milestoneStatus(item: Milestone, status: string) { const row = selected(); if (!row) return; const remark = (status === 'DELAYED' || status === 'EXCEPTION') ? window.prompt(status === 'DELAYED' ? '请填写延期原因' : '请填写异常原因') : ''; if ((status === 'DELAYED' || status === 'EXCEPTION') && !remark?.trim()) return; try { await request(`/api/contracts/${row.id}/milestones/${item.id}/status`, { method: 'POST', body: JSON.stringify({ status, remark }) }); await load() } catch (e) { error.value = (e as Error).message } }
async function openRecords(payment: Payment) { const row = selected(); if (!row) return; recordForm.paymentId = payment.id; recordForm.amount = ''; recordForm.paymentDate = ''; recordForm.voucherAttachmentId = null; recordForm.remark = ''; try { paymentRecords.value = await request<PaymentRecord[]>(`/api/contracts/${row.id}/payments/${payment.id}/records`) } catch (e) { error.value = (e as Error).message } }
async function addRecord() { const row = selected(); if (!row || !recordForm.paymentId || !recordForm.amount || !recordForm.paymentDate) { error.value = '请填写实际金额和日期'; return }; try { await request(`/api/contracts/${row.id}/payments/${recordForm.paymentId}/records`, { method: 'POST', body: JSON.stringify({ amount: Number(recordForm.amount), paymentDate: recordForm.paymentDate, voucherAttachmentId: recordForm.voucherAttachmentId, remark: recordForm.remark }) }); await openRecords({ id: recordForm.paymentId } as Payment); await load() } catch (e) { error.value = (e as Error).message } }
function overdue(date: string, status: string) { return status === 'PENDING' && date < new Date().toISOString().slice(0, 10) }
onMounted(load)
</script>

<template>
  <section class="fulfillment-workspace page-section">
    <div class="page-header"><div><p class="eyebrow">合同中心 / 履行管理</p><h1>履行与收付款</h1><p class="page-desc">集中跟进履行节点、收付款计划和到期风险，所有操作都会记录到合同审计。</p></div><span class="fulfillment-count">{{ rows.length }} 份履行中合同</span></div>
    <div v-if="error" class="fulfillment-error">{{ error }}<button @click="error=''">关闭</button></div>
    <div v-if="loading" class="fulfillment-empty">正在加载履行数据...</div>
    <div v-else-if="!rows.length" class="panel fulfillment-empty"><strong>暂无履行中的合同</strong><span>合同确认生效后会出现在这里。</span></div>
    <div v-else class="fulfillment-layout">
      <aside class="panel fulfillment-contracts"><div class="section-heading"><h2>履行中合同</h2><span class="muted">{{ rows.length }}</span></div><button v-for="item in rows" :key="item.id" class="fulfillment-contract" :class="{ active: item.id === selectedId }" @click="selectedId=item.id"><b>{{ item.name }}</b><small>{{ item.contractNo }} · {{ item.partyName }}</small></button></aside>
      <div v-if="selected()" class="fulfillment-detail"><section class="panel fulfillment-summary"><div><p class="eyebrow">当前合同</p><h2>{{ selected()!.name }}</h2><span>{{ selected()!.contractNo }} · {{ selected()!.partyName }}</span></div><div class="fulfillment-summary-stats"><b>{{ selected()!.milestones.length }}<small>履行节点</small></b><b>{{ selected()!.payments.length }}<small>收付款计划</small></b></div></section>
        <section class="panel fulfillment-section"><div class="section-heading"><div><h2>履行节点</h2><p class="section-caption">维护交付、验收和里程碑事项</p></div></div><div class="fulfillment-add"><input v-model="milestoneForm.name" placeholder="节点名称，如：阶段验收" /><input v-model="milestoneForm.dueDate" type="date" /><button class="secondary-action" @click="addMilestone">新增节点</button></div><div v-if="selected()!.milestones.length" class="fulfillment-list"><div v-for="item in selected()!.milestones" :key="item.id" class="fulfillment-item"><span class="item-marker" :class="{ late: overdue(item.dueDate, item.status), done: item.status === 'COMPLETED' }"></span><span><b>{{ item.name }}</b><small>{{ item.dueDate }} · {{ overdue(item.dueDate, item.status) ? '已逾期' : item.status }}</small></span><button v-if="item.status !== 'COMPLETED'" class="link-button" @click="completeMilestone(item)">完成</button><button v-if="item.status !== 'COMPLETED'" class="link-button" @click="milestoneStatus(item, 'IN_PROGRESS')">处理中</button><button v-if="item.status !== 'COMPLETED'" class="link-button" @click="milestoneStatus(item, 'DELAYED')">延期</button><button v-if="item.status !== 'COMPLETED'" class="link-button" @click="milestoneStatus(item, 'EXCEPTION')">异常</button></div></div><div v-else class="inline-empty">暂无履行节点，请先新增计划。</div></section>
        <section class="panel fulfillment-section"><div class="section-heading"><div><h2>收付款计划</h2><p class="section-caption">计划金额与实际收付款分开记录</p></div></div><div class="fulfillment-add payment-add"><input v-model="paymentForm.itemName" placeholder="计划名称，如：首付款" /><select v-model="paymentForm.direction"><option value="PAY">我方付款</option><option value="RECEIVE">我方收款</option></select><input v-model="paymentForm.amount" type="number" min="0" placeholder="计划金额" /><input v-model="paymentForm.dueDate" type="date" /><button class="secondary-action" @click="addPayment">新增计划</button></div><div v-if="selected()!.payments.length" class="fulfillment-list"><div v-for="item in selected()!.payments" :key="item.id" class="fulfillment-item payment-item"><span><b>{{ item.itemName }}</b><small>{{ item.direction === 'PAY' ? '付款' : '收款' }} · {{ item.dueDate }} · 计划金额 {{ item.amount }}</small></span><i class="status-tag" :class="{ overdue: overdue(item.dueDate, item.status) }">{{ overdue(item.dueDate, item.status) ? '已逾期' : item.status }}</i><button class="link-button" @click="openRecords(item)">登记实际</button></div></div><div v-else class="inline-empty">暂无收付款计划，请先新增计划。</div><div v-if="recordForm.paymentId" class="fulfillment-add payment-record-form"><strong>实际收付款</strong><input v-model="recordForm.amount" type="number" min="0" placeholder="实际金额" /><input v-model="recordForm.paymentDate" type="date" /><select v-model.number="recordForm.voucherAttachmentId"><option :value="null">不关联凭证</option><option v-for="file in selected()!.attachments" :key="file.id" :value="file.id">{{ file.fileName }}</option></select><input v-model="recordForm.remark" placeholder="说明（可选）" /><button class="secondary-action" @click="addRecord">保存实际记录</button><button class="link-button" @click="recordForm.paymentId=null">关闭</button></div><div v-if="recordForm.paymentId && paymentRecords.length" class="fulfillment-list"><div v-for="record in paymentRecords" :key="record.id" class="fulfillment-item payment-item"><span><b>实际记录 {{ record.amount }}</b><small>{{ record.paymentDate }} · {{ record.remark || '无说明' }}</small></span><i class="status-tag">{{ record.status }}</i></div></div></section>
      </div>
    </div>
  </section>
</template>
