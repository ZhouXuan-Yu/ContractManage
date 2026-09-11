<script setup lang="ts">
import { computed, ref } from 'vue'
type Type = { id: number; name: string; subtype: string; category: string; enabled: boolean }
type Party = { id: string; name: string; relationType: string }
type Template = { id: number; name: string; typeId: number; status: string; versionNo: number }
type Form = { typeId: number; partyId: string; name: string; remark: string; templateId: number | null; totalAmount: number | null; currency: string; paymentDirection: number | null; expireDate: string }
const props = defineProps<{ api: string; role: string; form: Form; types: Type[]; parties: Party[]; templates: Template[]; editing: boolean; loading: boolean }>()
const emit = defineEmits<{ back: []; save: [content: string] }>()
const content = ref('')
const aiLoading = ref(false)
const aiError = ref('')
const publishedTemplates = computed(() => props.templates.filter(item => item.typeId === props.form.typeId && item.status === 'PUBLISHED'))
const amountInvolved = computed({ get: () => props.form.totalAmount !== null || props.form.paymentDirection !== null, set: (value: boolean) => { if (!value) { props.form.totalAmount = null; props.form.paymentDirection = null } } })
const valid = computed(() => Boolean(props.form.name.trim() && props.form.typeId && props.form.partyId && (props.editing || props.form.templateId || content.value.trim()) && (!amountInvolved.value || (props.form.totalAmount !== null && props.form.currency && props.form.paymentDirection))))
async function aiDraft() {
  if (!props.form.name.trim() || !props.form.typeId || !props.form.partyId) { aiError.value = '请先填写合同名称、合同类型和对方主体'; return }
  aiLoading.value = true; aiError.value = ''
  try {
    const type = props.types.find(item => item.id === props.form.typeId)
    const party = props.parties.find(item => item.id === props.form.partyId)
    const response = await fetch(`${props.api}/api/ai/draft`, { method: 'POST', credentials: 'include', headers: { 'Content-Type': 'application/json', 'X-Role': props.role }, body: JSON.stringify({ contractName: props.form.name, contractType: type ? `${type.category}/${type.name}/${type.subtype}` : '', partyName: party?.name || '', ourPartyName: '当前登录组织', amount: props.form.totalAmount?.toString() || '', currency: props.form.currency, endDate: props.form.expireDate, requirements: props.form.remark }) })
    if (!response.ok) throw new Error((await response.text()) || 'AI 起草失败')
    const result = await response.json(); content.value = result.content || result.data?.content || ''; if (!content.value) throw new Error('AI did not return contract content')
  } catch (e) { aiError.value = (e as Error).message } finally { aiLoading.value = false }
}
</script>
<template>
  <section class="create-workspace page-section create-single-page">
    <div class="page-header"><div><button class="back-button" @click="emit('back')">← 返回{{ editing ? '合同详情' : '合同台账' }}</button><p class="eyebrow">合同管理 / {{ editing ? '编辑草稿' : '模板起草' }}</p><h1>{{ editing ? '编辑合同草稿' : '模板起草' }}</h1><p class="page-desc">带 * 的字段必须填写；带“系统带出”的字段由登录组织和权限决定，不在合同页面维护主数据。</p></div><span class="status-tag">{{ editing ? '草稿编辑' : '签前起草' }}</span></div>
    <section class="panel create-form-panel create-form-single">
      <div class="form-section-heading"><div><h2>合同主体信息</h2><p>主体是合同的核心主数据。下拉选择只读取“系统管理 → 基础配置”的已启用数据。</p></div><span class="form-section-index">01</span></div>
      <div class="form-grid">
        <label class="full-width">合同名称 <em>*</em><input v-model="form.name" placeholder="例如：2026 年度技术服务合同"><small class="field-help">用于台账检索、正文标题和审批单标题。</small></label>
        <label>合同大类 / 类型 <em>*</em><select v-model.number="form.typeId"><option v-for="type in types.filter(item => item.enabled)" :key="type.id" :value="type.id">{{ type.category }} / {{ type.name }} · {{ type.subtype }}</option></select><small class="field-help">从基础配置中的合同分类选择，不能在这里临时创建。</small></label>
        <label>我方签约主体 <em>*</em><select disabled><option>合同管理示例组织</option></select><small class="field-help">系统带出当前登录组织；有“切换我方主体”权限时才可切换。</small></label>
        <label>对方签约主体 <em>*</em><select v-model="form.partyId"><option value="">请选择已维护主体</option><option v-for="party in parties" :key="party.id" :value="party.id">{{ party.name }} · {{ party.relationType }}</option></select><small class="field-help">客户、供应商、员工等主体统一在基础配置维护；AI 识别结果也必须确认后才能成为正式主体。</small></label>
        <label>经办部门 <span class="system-label">系统带出</span><input value="当前登录部门" disabled></label>
        <label>经办人 <span class="system-label">系统带出</span><input value="当前登录人" disabled></label>
      </div>
      <div class="form-section-heading compact"><div><h2>商务信息</h2><p>只有涉及金额的合同才填写金额、币种和收付款方向。</p></div><span class="form-section-index">02</span></div>
      <div class="form-grid">
        <label>是否涉及金额 <em>*</em><select v-model="amountInvolved"><option :value="false">否，非金额合同</option><option :value="true">是，金额合同</option></select><small class="field-help">选择“是”后，以下三个字段变为必填。</small></label>
        <label v-if="amountInvolved">合同金额 <em>*</em><input v-model.number="form.totalAmount" min="0" type="number" placeholder="填写含税或不含税金额"><small class="field-help">金额口径请在备注中说明。</small></label>
        <label v-if="amountInvolved">币种 <em>*</em><select v-model="form.currency"><option>CNY</option><option>USD</option><option>EUR</option></select><small class="field-help">币种属于受控字典，后续在基础配置维护。</small></label>
        <label v-if="amountInvolved">收付款方向 <em>*</em><select v-model.number="form.paymentDirection"><option :value="null">请选择</option><option :value="1">我方收款</option><option :value="2">我方付款</option></select><small class="field-help">决定履约阶段生成收款或付款计划。</small></label>
        <label>计划到期日期<input v-model="form.expireDate" type="date"><small class="field-help">用于到期提醒，可在后续履约中调整。</small></label>
      </div>
      <div class="form-section-heading compact"><div><h2>模板与说明</h2><p>模板版本在保存时固定，后续模板升级不会覆盖本合同正文。</p></div><span class="form-section-index">03</span></div>
      <div class="form-grid"><label class="full-width">标准模板 <span v-if="!publishedTemplates.length" class="system-label">无已发布模板时可用 AI 起草</span><select v-model="form.templateId"><option :value="null">{{ publishedTemplates.length ? '请选择已发布模板' : '暂无已发布模板' }}</option><option v-for="template in publishedTemplates" :key="template.id" :value="template.id">{{ template.name }} · v{{ template.versionNo }}</option></select><small class="field-help">模板维护位置：系统管理 → 基础配置 → 合同模板；AI 起草不替代模板发布。</small></label><label class="full-width">补充说明<textarea v-model="form.remark" rows="4" placeholder="填写业务背景、金额口径、特殊约定或需要法务关注的事项"></textarea></label></div>
      <div class="ai-draft-editor"><div class="section-heading"><div><h2>合同正文草稿</h2><p class="section-caption">可由已发布模板生成，也可调用已配置的 AI 起草；AI 结果必须人工修改、确认后保存。</p></div><button class="secondary-action" :disabled="aiLoading" @click="aiDraft">{{ aiLoading ? 'AI 起草中...' : 'AI 辅助起草' }}</button></div><textarea v-model="content" rows="14" placeholder="选择模板后保存会自动生成正文；也可以先点击 AI 辅助起草"></textarea><p v-if="aiError" class="fulfillment-error">{{ aiError }}</p><span class="muted">当前正文仅作为草稿，后续仍需发起法律审查和审批。</span></div>
      <div class="create-info-band"><strong>保存后的业务结果</strong><span>合同会进入草稿状态；模板版本固定，正文可继续编辑。未配置真实 AI 或 AI 调用失败时，页面会明确提示，不会伪造成功。</span></div>
      <div class="create-footer"><span class="muted">* 为必填项，选择项均来自基础配置。</span><button class="secondary-action" @click="emit('back')">取消</button><button class="primary-action" :disabled="loading || !valid" @click="emit('save', content)">{{ loading ? '保存中...' : (editing ? '保存合同草稿' : '生成合同初稿') }}</button></div>
    </section>
  </section>
</template>
