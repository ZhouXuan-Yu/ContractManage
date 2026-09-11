<script setup lang="ts">
type Detail = { totalAmount: number | null; currency: string | null; paymentDirection: string | null; signDate: string | null; effectiveDate: string | null; expireDate: string | null; sourceTypeCode: string | null; archived?: boolean }
const props = defineProps<{ detail: Detail }>()
const direction = () => props.detail.paymentDirection === '1' ? '收款' : props.detail.paymentDirection === '2' ? '付款' : '未设置'
const source = () => props.detail.sourceTypeCode === '1' ? '模板创建' : props.detail.sourceTypeCode === '2' ? '文件导入' : props.detail.sourceTypeCode === '3' ? '手工录入' : '未设置'
const date = (value: string | null) => value || '未设置'
</script>
<template>
  <section class="panel formal-contract-panel">
    <div v-if="detail.archived" class="readonly-note"><strong>该合同已归档</strong><span>正文、附件、履约、收付款和变更均为只读，历史记录仍可查询。</span></div>
    <div class="section-heading"><div><h2>合同正式字段</h2><p class="section-caption">字段口径与合同主表保持一致，未录入的数据明确显示为未设置。</p></div><span class="formal-source-label">DOCX 主表口径</span></div>
    <dl class="formal-field-grid"><div><dt>合同总金额</dt><dd>{{ detail.totalAmount === null ? '未设置' : detail.totalAmount.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</dd></div><div><dt>币种</dt><dd>{{ detail.currency || 'CNY' }}</dd></div><div><dt>收付方向</dt><dd>{{ direction() }}</dd></div><div><dt>来源类型</dt><dd>{{ source() }}</dd></div><div><dt>签署日期</dt><dd>{{ date(detail.signDate) }}</dd></div><div><dt>生效日期</dt><dd>{{ date(detail.effectiveDate) }}</dd></div><div><dt>到期日期</dt><dd>{{ date(detail.expireDate) }}</dd></div></dl>
  </section>
</template>
