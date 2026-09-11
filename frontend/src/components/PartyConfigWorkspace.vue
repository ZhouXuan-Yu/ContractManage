<script setup lang="ts">
import { onMounted, ref } from 'vue'
type Party = { id: string; name: string; relationType: string; sourceType: string; nature: string; creditCode?: string; enabled: boolean }
const props = defineProps<{ api: string; role: string }>()
const rows = ref<Party[]>([]); const keyword = ref(''); const error = ref(''); const showForm = ref(false)
const form = ref({ name: '', relationType: '客户', nature: '企业', creditCode: '', contactName: '', contactPhone: '', remark: '' })
async function request<T>(path: string, options: RequestInit = {}) { const response = await fetch(`${props.api}${path}`, { ...options, headers: { 'Content-Type': 'application/json', 'X-Role': props.role, ...(options.headers || {}) } }); if (!response.ok) throw new Error((await response.text()) || '请求失败'); return response.json() as Promise<T> }
async function load() { try { rows.value = await request<Party[]>(`/api/config/parties?keyword=${encodeURIComponent(keyword.value)}`) } catch (e) { error.value = (e as Error).message } }
async function create() { if (!form.value.name.trim()) { error.value = '请填写主体名称'; return }; try { await request('/api/config/parties', { method: 'POST', body: JSON.stringify({ ...form.value, sourceType: '本地配置', enabled: true }) }); showForm.value = false; await load() } catch (e) { error.value = (e as Error).message } }
async function toggle(row: Party) { try { await request(`/api/config/parties/${encodeURIComponent(row.id)}/toggle`, { method: 'POST' }); await load() } catch (e) { error.value = (e as Error).message } }
onMounted(load)
</script>
<template>
  <section class="party-config-workspace page-section">
    <div class="config-subhead"><div><h2>对方主体</h2><p>维护合同中可选择的客户、供应商、员工及其他签约主体。合同页面和 AI 解析共用这里的数据。</p></div><button class="primary-action" @click="showForm = true">＋ 新增主体</button></div>
    <div v-if="error" class="fulfillment-error">{{ error }}<button @click="error = ''">关闭</button></div>
    <div class="config-toolbar"><input v-model="keyword" placeholder="按名称、主体编码或统一社会信用代码搜索" @keyup.enter="load"><button class="secondary-action" @click="load">查询</button></div>
    <div class="config-table"><div class="config-table-head"><span>主体名称</span><span>主体类型</span><span>统一社会信用代码</span><span>来源</span><span>状态</span><span>操作</span></div><div v-for="row in rows" :key="row.id" class="config-table-row"><span><b>{{ row.name }}</b><small>{{ row.id }} · {{ row.nature }}</small></span><span>{{ row.relationType }}</span><span>{{ row.creditCode || '未填写' }}</span><span>{{ row.sourceType }}</span><span><i class="status-tag">{{ row.enabled ? '启用' : '停用' }}</i></span><span><button class="link-button" @click="toggle(row)">{{ row.enabled ? '停用' : '启用' }}</button></span></div><div v-if="!rows.length" class="inline-empty">暂无主体配置</div></div>
    <section v-if="showForm" class="panel config-inline-form"><div class="section-heading"><div><h3>新增对方主体</h3><p class="section-caption">新增后立即出现在合同起草和 AI 解析确认的选择面板中。</p></div><button class="ghost-button" @click="showForm = false">取消</button></div><div class="form-grid"><label>主体名称 *<input v-model="form.name"></label><label>主体类型 *<select v-model="form.relationType"><option>客户</option><option>供应商</option><option>员工</option><option>组织</option><option>其他</option></select></label><label>主体性质<select v-model="form.nature"><option>企业</option><option>个人</option><option>组织</option></select></label><label>统一社会信用代码/证件号<input v-model="form.creditCode"></label><label>联系人<input v-model="form.contactName"></label><label>联系电话<input v-model="form.contactPhone"></label><label class="full-width">备注<textarea v-model="form.remark" rows="2"></textarea></label></div><div class="form-footer"><span class="muted">来源固定为本地配置，后续可替换为公司主数据来源。</span><button class="primary-action" @click="create">保存主体</button></div></section>
  </section>
</template>
