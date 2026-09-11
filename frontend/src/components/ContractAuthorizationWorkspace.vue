<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
type Auth = { id: number; targetUserId: string; view: boolean; edit: boolean; export: boolean; legalConfirm: boolean; status: number; expireTime: string | null; remark: string | null }
const props = defineProps<{ api: string; role: string; userId: string; orgId: string; departmentId: string; contractId: number; contractName: string }>()
const emit = defineEmits<{ back: [] }>()
const rows = ref<Auth[]>([]); const error = ref(''); const saving = ref(false); const loading = ref(false)
const form = ref({ targetUserId: '10002', view: true, edit: false, export: false, legalConfirm: false, expireTime: '', remark: '' })
const headers = () => ({ 'Content-Type': 'application/json', 'X-Role': props.role, 'X-User-Id': props.userId, 'X-Org-Id': props.orgId, 'X-Department-Id': props.departmentId })
async function load() {
  loading.value = true
  try { const r = await fetch(`${props.api}/api/contract-authorizations?contractId=${props.contractId}`, { headers: headers() }); if (!r.ok) throw new Error(await r.text()); rows.value = await r.json() }
  catch (e) { error.value = (e as Error).message } finally { loading.value = false }
}
async function save() {
  saving.value = true; error.value = ''
  try { const r = await fetch(`${props.api}/api/contract-authorizations`, { method: 'POST', headers: headers(), body: JSON.stringify({ contractId: props.contractId, ...form.value, expireTime: form.value.expireTime || null }) }); if (!r.ok) throw new Error(await r.text()); await load() }
  catch (e) { error.value = (e as Error).message } finally { saving.value = false }
}
async function revoke(id: number) {
  try { await fetch(`${props.api}/api/contract-authorizations/${id}/revoke`, { method: 'POST', headers: headers() }); await load() }
  catch (e) { error.value = (e as Error).message }
}
watch(() => props.contractId, () => load().catch(e => error.value = (e as Error).message))
onMounted(() => load().catch(e => error.value = (e as Error).message))
</script>

<template>
  <section class="permission-workspace page-section">
    <div class="page-header">
      <div>
        <button class="back-button" @click="emit('back')">← 返回合同详情</button>
        <p class="eyebrow">合同详情 / 合同级授权</p>
        <h1>合同级授权</h1>
        <p class="page-desc">{{ contractName }} — 为指定人员临时授予单份合同访问权限；角色范围之外的授权同样受有效期和撤销状态控制。</p>
      </div>
      <span class="formal-source-label">CONTRACT-SCOPED ACCESS</span>
    </div>
    <div v-if="error" class="fulfillment-error">{{ error }}<button @click="error = ''">关闭</button></div>
    <div class="permission-layout">
      <section class="panel permission-role-list">
        <div class="section-heading"><div><h2>已授权记录</h2><p class="section-caption">当前合同的临时授权清单。</p></div></div>
        <div v-if="loading" class="inline-empty">正在加载授权记录...</div>
        <div v-else-if="!rows.length" class="empty-state"><strong>暂无临时授权</strong><span>授权后，被授权用户可在角色范围外访问该合同。</span></div>
        <div v-for="item in rows" :key="item.id" class="permission-role-row">
          <span class="role-mark">{{ item.targetUserId.slice(0, 1) }}</span>
          <span><b>用户 {{ item.targetUserId }}</b><small>{{ item.status === 1 ? '有效' : '已撤销' }} · {{ item.expireTime || '长期有效' }}</small></span>
          <i :class="['status-tag', { disabled: item.status !== 1 }]">{{ item.status === 1 ? '有效' : '已撤销' }}</i>
          <button v-if="item.status === 1" class="link-button" @click="revoke(item.id)">撤销</button>
        </div>
      </section>
      <section class="panel permission-detail">
        <div class="permission-detail-head">
          <div><p class="eyebrow">临时授权</p><h2>新增授权</h2><span>至少选择一项操作权限。</span></div>
          <button class="primary-action" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存授权' }}</button>
        </div>
        <div class="form-grid">
          <label>被授权用户<input v-model="form.targetUserId" placeholder="请输入用户 ID" /></label>
          <label>到期时间<input v-model="form.expireTime" type="datetime-local" /></label>
          <label class="full-width">授权说明<textarea v-model="form.remark" rows="2" /></label>
        </div>
        <div class="permission-checks">
          <label><input v-model="form.view" type="checkbox" />查看</label>
          <label><input v-model="form.edit" type="checkbox" />编辑</label>
          <label><input v-model="form.export" type="checkbox" />导出</label>
          <label><input v-model="form.legalConfirm" type="checkbox" />法务确认</label>
        </div>
      </section>
    </div>
  </section>
</template>
