<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

type Connection = { id: number; connectionType: string; name: string; provider: string; baseUrl: string; status: string; secretConfigured: boolean; lastTestAt: string | null; lastError: string | null; updatedAt: string }
type Log = { id: number; action: string; result: string; message: string; operatedAt: string }
type Integration = { capability: string; provider: string; mode: string; available: boolean; note: string }
const props = defineProps<{ api: string; role: string }>()
const connections = ref<Connection[]>([])
const selectedId = ref<number | null>(null)
const logs = ref<Log[]>([])
const integrations = ref<Integration[]>([])
const loading = ref(false); const saving = ref(false); const testing = ref(false); const error = ref(''); const showCreate = ref(false)
const editing = ref(false)
const form = ref({ name: '', provider: '', baseUrl: '', secret: '' })
const selected = computed(() => connections.value.find(item => item.id === selectedId.value) || null)
const labels: Record<string, string> = { HR_ORG: 'HR / 组织系统', TRADE_PARTY: '交易对方主数据', LEGAL_ENTITY: '我方主体系统', FILE_SERVICE: '文件服务', APPROVAL: '外部审批系统', AI_PROVIDER: 'AI Provider' }
const statusLabels: Record<string, string> = { ENABLED: '已启用', DISABLED: '已停用', FAILED: '测试失败', NOT_CONFIGURED: '未配置' }
async function request<T>(path: string, options: RequestInit = {}) { const response = await fetch(`${props.api}${path}`, { ...options, headers: { 'Content-Type': 'application/json', 'X-Role': props.role, ...(options.headers || {}) } }); if (!response.ok) throw new Error((await response.text()) || '请求失败'); return response.json() as Promise<T> }
async function load() { loading.value = true; try { [connections.value, integrations.value] = await Promise.all([request<Connection[]>('/api/connections'), request<Integration[]>('/api/integrations/status')]); if (!selectedId.value) selectedId.value = connections.value[0]?.id || null; await loadLogs() } catch (e) { error.value = (e as Error).message } finally { loading.value = false } }
async function loadLogs() { if (selectedId.value) logs.value = await request<Log[]>(`/api/connections/${selectedId.value}/logs`) }
function select(item: Connection) { selectedId.value = item.id; logs.value = []; loadLogs().catch(e => error.value = (e as Error).message) }
function edit() { if (!selected.value) return; editing.value = true; form.value = { name: selected.value.name, provider: selected.value.provider || '', baseUrl: selected.value.baseUrl || '', secret: '' }; showCreate.value = true }
function createForm() { editing.value = false; form.value = { name: '', provider: '', baseUrl: '', secret: '' }; showCreate.value = true }
async function save() { if (!form.value.name.trim() || !form.value.provider.trim() || !form.value.baseUrl.trim()) { error.value = '请填写名称、Provider 和服务地址'; return }; saving.value = true; try { if (editing.value && selected.value) await request(`/api/connections/${selected.value.id}`, { method: 'PUT', body: JSON.stringify(form.value) }); else { const item = await request<Connection>('/api/connections', { method: 'POST', body: JSON.stringify({ ...form.value, connectionType: `CUSTOM_${Date.now()}` }) }); selectedId.value = item.id }; showCreate.value = false; await load() } catch (e) { error.value = (e as Error).message } finally { saving.value = false } }
async function action(path: string) { try { const item = await request<Connection>(path, { method: 'POST' }); selectedId.value = item.id; await load(); await loadLogs() } catch (e) { error.value = (e as Error).message } }
async function test() { if (!selected.value) return; testing.value = true; try { await action(`/api/connections/${selected.value.id}/test`) } finally { testing.value = false } }
function status(item: Connection) { return statusLabels[item.status] || item.status }
onMounted(load)
</script>

<template>
  <section class="connection-workspace page-section">
    <div class="page-header"><div><p class="eyebrow">系统管理 / 外部连接配置</p><h1>外部连接配置</h1><p class="page-desc">统一维护外部系统适配器。业务主数据仍以外部系统为准，合同系统只保存连接和审计信息。</p></div><button class="primary-action" @click="createForm">＋ 新建连接</button></div>
    <div v-if="error" class="fulfillment-error">{{ error }}<button @click="error=''">关闭</button></div>
    <section v-if="integrations.length" class="panel identity-source-panel"><div class="section-heading"><div><h2>身份目录接入状态</h2><p class="section-caption">当前使用本地目录降级；正式接入需替换 DirectoryProvider，不改变合同业务页面。</p></div><span class="formal-source-label">READ-ONLY CONTRACT</span></div><div class="identity-source-grid"><div v-for="item in integrations.filter(item => item.capability === 'HR_SSO_DIRECTORY')" :key="item.capability"><span>身份来源</span><b>{{ item.provider }}</b><i :class="['status-tag', { warning: item.mode === 'LOCAL_FALLBACK' }]">{{ item.mode === 'LOCAL_FALLBACK' ? '本地降级' : item.available ? '已连接' : '不可用' }}</i><small>{{ item.note }}</small></div><div><span>数据边界</span><b>只读用户、组织、部门</b><small>合同系统不创建或修改 HR/IAM 主数据</small></div><div><span>必需字段</span><b>用户 ID · 账号状态 · 组织 · 部门 · 角色</b><small>字段契约未提供前不标记为正式接入</small></div></div></section>
    <div class="connection-layout">
      <aside class="panel connection-list"><div class="section-heading"><div><h2>连接目录</h2><p class="section-caption">{{ connections.length }} 个适配器</p></div></div><div v-if="loading" class="inline-empty">正在加载...</div><div v-else-if="!connections.length" class="empty-state"><strong>暂无连接</strong><span>创建第一个外部系统连接。</span></div><button v-for="item in connections" :key="item.id" class="connection-row" :class="{ active: item.id === selectedId }" @click="select(item)"><span class="connection-icon">{{ item.connectionType === 'AI_PROVIDER' ? 'AI' : item.connectionType === 'FILE_SERVICE' ? '文' : '系' }}</span><span><b>{{ item.name }}</b><small>{{ item.provider }}</small></span><i class="status-tag">{{ status(item) }}</i></button></aside>
      <section class="panel connection-detail" v-if="selected"><div class="connection-detail-head"><div><p class="eyebrow">适配器详情</p><h2>{{ selected.name }}</h2><span>{{ labels[selected.connectionType] || selected.connectionType }}</span></div><div class="connection-actions"><i class="status-tag large">{{ status(selected) }}</i><button class="secondary-action" @click="edit">编辑配置</button><button class="secondary-action" @click="action(`/api/connections/${selected.id}/toggle`)">{{ selected.status === 'ENABLED' ? '停用连接' : '启用连接' }}</button><button class="primary-action" :disabled="testing" @click="test">{{ testing ? '测试中...' : '测试连接' }}</button></div></div>
        <div class="connection-meta"><div><span>Provider</span><b>{{ selected.provider || '未配置' }}</b></div><div><span>服务地址</span><b>{{ selected.baseUrl || '未配置' }}</b></div><div><span>访问密钥</span><b>{{ selected.secretConfigured ? '已配置（脱敏）' : '未配置' }}</b></div><div><span>最近测试</span><b>{{ selected.lastTestAt ? new Date(selected.lastTestAt).toLocaleString('zh-CN') : '尚未测试' }}</b></div></div>
        <div v-if="selected.lastError" class="connection-error"><b>最近一次错误</b><span>{{ selected.lastError }}</span></div>
        <div class="connection-log"><div class="section-heading"><div><h3>连接测试日志</h3><p class="section-caption">仅记录结果和摘要，不记录密钥内容</p></div></div><div v-if="logs.length" class="timeline"><div v-for="item in logs" :key="item.id" class="timeline-row"><span class="timeline-dot" :class="{ danger: item.result === 'FAILED' }"></span><span><b>{{ item.action }} · {{ item.result === 'SUCCESS' ? '成功' : '失败' }}</b><small>{{ item.message }} · {{ new Date(item.operatedAt).toLocaleString('zh-CN') }}</small></span></div></div><div v-else class="inline-empty">暂无测试记录</div></div>
      </section>
      <section v-else class="panel connection-detail empty-state"><strong>选择一个连接</strong><span>在左侧查看配置状态、测试结果和审计日志。</span></section>
    </div>
    <section v-if="showCreate" class="panel connection-form"><div class="section-heading"><div><h2>{{ editing ? '编辑连接' : '新建连接' }}</h2><p class="section-caption">密钥只用于服务端连接，不会明文展示。</p></div><button class="ghost-button" @click="showCreate=false">取消</button></div><div class="form-grid"><label>连接名称<input v-model="form.name" placeholder="例如：集团 HR 系统" /></label><label>Provider<input v-model="form.provider" placeholder="例如：HR Adapter" /></label><label class="full-width">服务地址<input v-model="form.baseUrl" placeholder="https://service.example.com/api" /></label><label class="full-width">访问密钥<input v-model="form.secret" type="password" placeholder="留空表示保留现有密钥" /></label></div><div class="form-footer"><span class="muted">连接配置变更会写入审计日志</span><button class="primary-action" :disabled="saving" @click="save">{{ saving ? '保存中...' : '保存连接' }}</button></div></section>
  </section>
</template>
