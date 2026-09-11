<script setup lang="ts">
import { computed, ref } from 'vue'

type Type = { id: number; name: string; subtype: string; enabled: boolean }
type Party = { id: string; name: string; relationType: string }

const props = defineProps<{ path: 'pending' | 'signed'; api: string; role: string; userId: string; orgId: string; departmentId: string; types: Type[]; parties: Party[] }>()
const emit = defineEmits<{ back: []; saved: [id: number] }>()
const fileInput = ref<HTMLInputElement | null>(null)
const selectedFile = ref<File | null>(null)
const contractId = ref<number | null>(null)
const attachmentId = ref<number | null>(null)
const parsing = ref(false)
const saving = ref(false)
const error = ref('')
const parsed = ref(false)
const fieldSource = ref<Record<string, 'AI' | 'MANUAL'>>({})
const form = ref({ contractName: '', typeId: 0, partyId: '', totalAmount: '', currency: 'CNY', paymentDirection: '', signDate: '', effectiveDate: '', expireDate: '', remark: '' })
const isSigned = computed(() => props.path === 'signed')
const title = computed(() => isSigned.value ? '登记已签合同' : '上传待审合同')
const readyToConfirm = computed(() => parsed.value && form.value.contractName.trim() && form.value.typeId && form.value.partyId && (!isSigned.value || (form.value.signDate && form.value.effectiveDate)))

function headers(json = false) { return { ...(json ? { 'Content-Type': 'application/json' } : {}), 'X-Role': props.role, 'X-User-Id': props.userId, 'X-Org-Id': props.orgId, 'X-Department-Id': props.departmentId } }
function chooseFile(event: Event) { setFile((event.target as HTMLInputElement).files?.[0] || null) }
function dropFile(event: DragEvent) { setFile(event.dataTransfer?.files?.[0] || null) }
function setFile(file: File | null) { selectedFile.value = file; parsed.value = false; error.value = ''; contractId.value = null; attachmentId.value = null }
function change(field: string) { if (parsed.value) fieldSource.value[field] = 'MANUAL' }
function source(field: string) { return fieldSource.value[field] === 'MANUAL' ? '人工修改' : 'AI 识别' }
function partyCandidate(basic: any) { return basic.counterparty_name || basic.party_name || basic.counterparty || basic.party || '' }
function matchParty(name: string) { return props.parties.find(item => item.name === name || item.name.includes(name) || name.includes(item.name))?.id || '' }
function typeCandidate(basic: any) { return basic.contract_type || basic.type_name || '' }
function matchType(name: string) { return props.types.find(item => item.name === name || item.subtype === name || item.name.includes(name) || item.subtype.includes(name))?.id || 0 }

async function parseFile() {
  if (!selectedFile.value || parsing.value) return
  parsing.value = true; error.value = ''
  try {
    const intake = await fetch(`${props.api}/api/contracts/intake?path=${encodeURIComponent(props.path)}`, { method: 'POST', credentials: 'include', headers: headers() })
    if (!intake.ok) throw new Error((await intake.text()) || '创建解析草稿失败')
    const contract = await intake.json(); contractId.value = contract.id
    const data = new FormData(); data.append('file', selectedFile.value)
    const upload = await fetch(`${props.api}/api/contracts/${contract.id}/attachments?bizAttachType=1`, { method: 'POST', credentials: 'include', headers: headers(), body: data })
    if (!upload.ok) throw new Error((await upload.text()) || '文件上传失败')
    const attachment = await upload.json(); attachmentId.value = attachment.id
    const extract = await fetch(`${props.api}/api/contracts/${contract.id}/ai/extract-file?attachmentId=${attachment.id}`, { method: 'POST', credentials: 'include', headers: headers() })
    if (!extract.ok) throw new Error((await extract.text()) || '文件解析失败')
    const latest = await fetch(`${props.api}/api/contracts/${contract.id}/ai/extract/latest`, { credentials: 'include', headers: headers() })
    if (!latest.ok) throw new Error((await latest.text()) || '未取得 AI 解析结果')
    const result = await latest.json(); const basic = result.basic_info || {}
    const partyName = partyCandidate(basic); const typeName = typeCandidate(basic)
    form.value = { ...form.value, contractName: basic.contract_name || contract.name || '', typeId: matchType(typeName), partyId: matchParty(partyName), totalAmount: basic.total_amount == null ? '' : String(basic.total_amount), currency: basic.currency === '人民币' ? 'CNY' : (basic.currency || 'CNY'), paymentDirection: basic.payment_direction || '', signDate: basic.sign_date || '', effectiveDate: basic.effective_date || '', expireDate: basic.expire_date || '' }
    fieldSource.value = { contractName: 'AI', typeId: 'AI', partyId: 'AI', totalAmount: 'AI', currency: 'AI', paymentDirection: 'AI', signDate: 'AI', effectiveDate: 'AI', expireDate: 'AI' }
    parsed.value = true
  } catch (e) { error.value = (e as Error).message }
  finally { parsing.value = false }
}

async function confirm() {
  if (!contractId.value || !attachmentId.value || !readyToConfirm.value) return
  saving.value = true; error.value = ''
  try {
    const body = { ...form.value, totalAmount: form.value.totalAmount || null, confirm: true }
    const save = await fetch(`${props.api}/api/contracts/${contractId.value}/ai/extract/confirm`, { method: 'POST', credentials: 'include', headers: headers(true), body: JSON.stringify(body) })
    if (!save.ok) throw new Error((await save.text()) || '确认解析结果失败')
    if (isSigned.value) {
      const query = new URLSearchParams({ signedFileId: String(attachmentId.value), signDate: form.value.signDate, effectiveDate: form.value.effectiveDate })
      const registered = await fetch(`${props.api}/api/contracts/${contractId.value}/register-signed?${query}`, { method: 'POST', credentials: 'include', headers: headers() })
      if (!registered.ok) throw new Error((await registered.text()) || '登记已签合同失败')
    }
    emit('saved', contractId.value)
  } catch (e) { error.value = (e as Error).message }
  finally { saving.value = false }
}
</script>

<template>
  <section class="contract-intake page-section">
    <div class="page-header"><div><button class="back-button" @click="emit('back')">← 返回合同台账</button><p class="eyebrow">CONTRACT INTAKE</p><h1>{{ title }}</h1><p class="page-desc">上传合同文件后，系统提取文本并由 AI 识别合同字段；请核对结果后再保存。</p></div></div>
    <section class="panel signed-registration intake-review-page">
      <div class="section-heading"><div><h2>{{ isSigned ? '已签原件与信息核验' : '待审文件与信息核验' }}</h2><p class="section-caption">AI 结果仅作为候选信息；人工确认后才会写入合同台账和履约数据。</p></div><span class="status-tag">{{ isSigned ? '已签登记' : '待审合同' }}</span></div>
      <div class="registration-grid intake-review-grid">
        <div class="intake-file-column">
          <div class="file-drop" @dragover.prevent @drop.prevent="dropFile" @click="fileInput?.click()"><input ref="fileInput" type="file" accept=".doc,.docx,.pdf,.png,.jpg,.jpeg" @change="chooseFile" /><strong>{{ selectedFile ? selectedFile.name : '拖拽合同文件到这里，或点击选择文件' }}</strong><small>支持 Word、PDF、JPG、PNG。图片会先归档；OCR 接入前不能自动读取扫描件文字。</small></div>
          <div v-if="selectedFile" class="uploaded-file-card"><span><b>{{ selectedFile.name }}</b><small>{{ Math.ceil(selectedFile.size / 1024) }} KB · {{ parsed ? '已完成解析' : '等待解析' }}</small></span><button class="link-button" @click="setFile(null)">移除</button></div>
          <button class="primary-action intake-parse-button" :disabled="!selectedFile || parsing" @click="parseFile">{{ parsing ? '正在提取并解析...' : '提取文本并 AI 解析' }}</button>
        </div>
        <div class="intake-review-form">
          <div v-if="!parsed" class="intake-review-empty"><strong>等待解析文件</strong><span>文件上传并解析后，合同名称、主体、金额、日期等信息会显示在这里供你核对。</span></div>
          <div v-else class="form-grid">
            <label class="full-width required">合同名称 <em>{{ source('contractName') }}</em><input v-model="form.contractName" @input="change('contractName')" /></label>
            <label class="required">合同类型 <em>{{ source('typeId') }}</em><select v-model.number="form.typeId" @change="change('typeId')"><option :value="0">请确认合同类型</option><option v-for="item in types.filter(type => type.enabled)" :key="item.id" :value="item.id">{{ item.name }} · {{ item.subtype }}</option></select></label>
            <label class="required">对方主体 <em>{{ source('partyId') }}</em><select v-model="form.partyId" @change="change('partyId')"><option value="">请确认或选择主体</option><option v-for="item in parties" :key="item.id" :value="item.id">{{ item.name }} · {{ item.relationType }}</option></select><small class="field-help">仅确认匹配的主数据主体；未匹配主体不会自动创建为正式客户。</small></label>
            <label>合同金额 <em>{{ source('totalAmount') }}</em><input v-model="form.totalAmount" inputmode="decimal" @input="change('totalAmount')" /></label>
            <label>币种 <em>{{ source('currency') }}</em><input v-model="form.currency" maxlength="8" @input="change('currency')" /></label>
            <label>收付款方向 <em>{{ source('paymentDirection') }}</em><select v-model="form.paymentDirection" @change="change('paymentDirection')"><option value="">待确认</option><option value="RECEIVE">我方收款</option><option value="PAY">我方付款</option></select></label>
            <label>到期日期 <em>{{ source('expireDate') }}</em><input v-model="form.expireDate" type="date" @input="change('expireDate')" /></label>
            <template v-if="isSigned"><label class="required">签署日期 <em>{{ source('signDate') }}</em><input v-model="form.signDate" type="date" @input="change('signDate')" /></label><label class="required">生效日期 <em>{{ source('effectiveDate') }}</em><input v-model="form.effectiveDate" type="date" @input="change('effectiveDate')" /></label></template>
            <label class="full-width">备注<textarea v-model="form.remark" rows="3" placeholder="填写核验说明或需要关注的事项" /></label>
          </div>
        </div>
      </div>
      <p v-if="error" class="registration-error">{{ error }}</p>
      <div class="form-footer"><span class="muted">{{ isSigned ? '确认后直接登记为履行中，不生成签前审批记录。' : '确认后保存为待审合同，后续可发起法律审查和审批。' }}</span><button class="primary-action" :disabled="saving || !readyToConfirm" @click="confirm">{{ saving ? '保存中...' : (isSigned ? '确认登记并进入履约' : '确认解析结果并保存') }}</button></div>
    </section>
  </section>
</template>
