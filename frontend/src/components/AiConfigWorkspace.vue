<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

type Config = {
  name: string; provider: string; baseUrl: string; model: string; secretConfigured: boolean
  draftWorkflowId: string; extractWorkflowId: string; reviewWorkflowId: string; fulfillmentWorkflowId: string
  enabled: boolean; draftEnabled: boolean; extractEnabled: boolean; reviewEnabled: boolean; fulfillmentEnabled: boolean
  runtimeProvider: string; runtimeMode: string; lastTestAt: string | null; lastError: string | null
}
type Capability = { key: 'draft'|'extract'|'review'|'fulfillment'; title: string; description: string; entry: string; workflow: keyof Form; enabled: keyof Form }
type Form = { provider: string; baseUrl: string; model: string; secret: string; draftWorkflowId: string; extractWorkflowId: string; reviewWorkflowId: string; fulfillmentWorkflowId: string; enabled: boolean; draftEnabled: boolean; extractEnabled: boolean; reviewEnabled: boolean; fulfillmentEnabled: boolean }
type TestResult = { capability: string; provider: string; success: boolean; message: string; testedAt: string }

const props = defineProps<{ api: string; role: string }>()
const config = ref<Config | null>(null)
const loading = ref(true); const saving = ref(false); const testing = ref(false); const error = ref(''); const notice = ref('')
const secret = ref(''); const capabilityTests = ref<Record<string, TestResult>>({}); const testingCapability = ref('')
const form = ref<Form>({ provider: 'mock', baseUrl: '', model: '', secret: '', draftWorkflowId: '', extractWorkflowId: '', reviewWorkflowId: '', fulfillmentWorkflowId: '', enabled: false, draftEnabled: true, extractEnabled: true, reviewEnabled: true, fulfillmentEnabled: true })
const providers = [{ value: 'mock', label: '本地 Mock', description: '仅用于本地页面和流程验证' }, { value: 'dify', label: 'Dify', description: '按能力调用对应 Dify 工作流' }, { value: 'deepseek', label: 'DeepSeek', description: '兼容 OpenAI Chat Completions 接口' }, { value: 'company_ai', label: '公司 AI 网关', description: '调用公司内部兼容接口' }]
const capabilities: Capability[] = [
  { key: 'draft', title: 'AI 辅助起草', description: '根据合同信息生成可编辑的合同草稿', entry: '合同起草页', workflow: 'draftWorkflowId', enabled: 'draftEnabled' },
  { key: 'extract', title: '合同信息解析', description: '从 Word、PDF 合同中提取主体、金额和期限', entry: '合同文件导入', workflow: 'extractWorkflowId', enabled: 'extractEnabled' },
  { key: 'review', title: '法律风险审查', description: '识别风险条款并生成待人工确认的审查结果', entry: '合同详情页', workflow: 'reviewWorkflowId', enabled: 'reviewEnabled' },
  { key: 'fulfillment', title: '履约信息提取', description: '从合同正文提取付款计划和履约节点', entry: '合同详情页', workflow: 'fulfillmentWorkflowId', enabled: 'fulfillmentEnabled' },
]
const providerLabel = computed(() => providers.find(item => item.value === form.value.provider)?.label || form.value.provider)

async function request<T>(path: string, options: RequestInit = {}) {
  const response = await fetch(`${props.api}${path}`, { ...options, credentials: 'include', headers: { 'Content-Type': 'application/json', 'X-Role': props.role, ...(options.headers || {}) } })
  if (!response.ok) throw new Error((await response.text()) || '请求失败')
  return response.json() as Promise<T>
}
function copyConfig(item: Config) { form.value = { provider: item.provider, baseUrl: item.baseUrl || '', model: item.model || '', secret: '', draftWorkflowId: item.draftWorkflowId || '', extractWorkflowId: item.extractWorkflowId || '', reviewWorkflowId: item.reviewWorkflowId || '', fulfillmentWorkflowId: item.fulfillmentWorkflowId || '', enabled: item.enabled, draftEnabled: item.draftEnabled, extractEnabled: item.extractEnabled, reviewEnabled: item.reviewEnabled, fulfillmentEnabled: item.fulfillmentEnabled } }
async function load() { loading.value = true; try { config.value = await request<Config>('/api/ai/config'); if (config.value) copyConfig(config.value) } catch (e) { error.value = (e as Error).message } finally { loading.value = false } }
async function saveProvider() { saving.value = true; error.value = ''; try { config.value = await request<Config>('/api/ai/config', { method: 'PUT', body: JSON.stringify({ name: '合同 AI 服务', ...form.value, secret: secret.value }) }); secret.value = ''; notice.value = '提供方配置已保存'; setTimeout(() => notice.value = '', 2500) } catch (e) { error.value = (e as Error).message } finally { saving.value = false } }
async function testProvider() { testing.value = true; error.value = ''; try { config.value = await request<Config>('/api/ai/config/test', { method: 'POST' }); notice.value = config.value.lastError ? `基础连接失败：${config.value.lastError}` : '基础连接测试成功'; } catch (e) { error.value = (e as Error).message } finally { testing.value = false } }
async function saveCapability() { await saveProvider() }
async function testCapability(row: Capability) { testingCapability.value = row.key; error.value = ''; try { const result = await request<TestResult>(`/api/ai/config/test/${row.key}`, { method: 'POST' }); capabilityTests.value[row.key] = result } catch (e) { error.value = (e as Error).message } finally { testingCapability.value = '' } }
function status(row: Capability) { const result = capabilityTests.value[row.key]; if (!form.value.enabled || !form.value[row.enabled]) return '未启用'; if (!result) return '待测试'; return result.success ? '可用' : '失败' }
onMounted(load)
</script>

<template>
  <section class="ai-config-workspace page-section">
    <div class="page-header"><div><p class="eyebrow">系统管理 / 基础配置 / AI 能力</p><h1>AI 能力配置</h1><p class="page-desc">按提供方管理凭证，按合同能力分别配置、测试和启用。</p></div><span v-if="config" :class="['status-tag', { warning: !config.enabled }]">{{ config.enabled ? '合同 AI 已启用' : '合同 AI 未启用' }}</span></div>
    <div v-if="error" class="fulfillment-error">{{ error }}<button @click="error = ''">关闭</button></div><div v-if="notice" class="ai-config-notice">{{ notice }}</div><div v-if="loading" class="inline-empty">正在加载 AI 配置...</div>
    <template v-else>
      <section class="panel ai-config-panel provider-config-card"><div class="config-panel-heading"><div><span class="panel-kicker">PROVIDER CREDENTIALS</span><h2>AI 提供方凭证</h2><p>密钥只保存到服务端，不在页面回显。更换提供方后，合同能力会按下面清单分别调用。</p></div></div><div class="form-grid"><label>提供方<select v-model="form.provider"><option v-for="item in providers" :key="item.value" :value="item.value">{{ item.label }}</option></select><small class="field-help">{{ providers.find(item => item.value === form.provider)?.description }}</small></label><label>服务地址<input v-model="form.baseUrl" :placeholder="form.provider === 'dify' ? 'https://api.dify.ai/v1' : 'https://api.deepseek.com'"></label><label>模型<input v-model="form.model" placeholder="DeepSeek 使用 deepseek-chat"></label><label>API Key<input v-model="secret" type="password" :placeholder="config?.secretConfigured ? '已配置，留空表示不更换' : '输入密钥，仅提交到服务端'"></label><label class="switch-field"><input v-model="form.enabled" type="checkbox">启用合同 AI<span>总开关关闭后，所有合同 AI 能力均不可调用。</span></label></div><div class="config-action-row"><span class="muted">当前运行提供方：{{ providerLabel }} · {{ config?.secretConfigured ? '密钥已配置' : '密钥未配置' }}</span><span class="row-actions"><button class="secondary-action" :disabled="testing" @click="testProvider">{{ testing ? '测试中...' : '测试提供方连接' }}</button><button class="primary-action" :disabled="saving" @click="saveProvider">{{ saving ? '保存中...' : '保存提供方配置' }}</button></span></div></section>
      <section class="panel ai-capability-panel"><div class="config-panel-heading"><div><span class="panel-kicker">AI CAPABILITIES</span><h2>合同 AI 能力</h2><p>每项能力都有自己的配置和操作。没有使用的能力可以单独停用，不影响其他能力。</p></div></div><div class="capability-list"><article v-for="row in capabilities" :key="row.key" class="capability-row"><div class="capability-main"><div class="capability-title"><h3>{{ row.title }}</h3><span :class="['status-tag', status(row) === '可用' ? 'success' : status(row) === '失败' ? 'danger' : 'warning']">{{ status(row) }}</span></div><p>{{ row.description }}</p><small>业务入口：{{ row.entry }} · 提供方：{{ providerLabel }}</small></div><div class="capability-config"><label v-if="form.provider === 'dify'">工作流 ID<input v-model="form[row.workflow]" placeholder="填写该能力的 Dify 工作流 ID"></label><div v-else class="inherited-config"><span>调用模型</span><b>{{ form.model || '未配置模型' }}</b><small>地址和模型使用上方提供方配置</small></div><label class="capability-switch"><input v-model="form[row.enabled]" type="checkbox">启用此能力</label></div><div class="capability-actions"><button class="secondary-action" :disabled="saving" @click="saveCapability">保存此能力</button><button class="secondary-action" :disabled="testingCapability === row.key" @click="testCapability(row)">{{ testingCapability === row.key ? '测试中...' : '测试此能力' }}</button><div v-if="capabilityTests[row.key]" :class="['capability-test-result', capabilityTests[row.key].success ? 'ok' : 'fail']">{{ capabilityTests[row.key].message }}</div></div></article></div></section>
    </template>
  </section>
</template>

<style scoped>
.ai-config-notice { margin: 12px 0; padding: 10px 14px; border: 1px solid #b7dfc5; background: #f0fbf4; color: #176b38; border-radius: 6px; }
.config-action-row, .capability-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-top: 18px; padding-top: 16px; border-top: 1px solid #e5e7eb; }
.row-actions { display: flex; gap: 10px; flex-wrap: wrap; }
.capability-list { display: grid; gap: 12px; }
.capability-row { display: grid; grid-template-columns: minmax(260px, 1.2fr) minmax(240px, 1fr) auto; gap: 20px; align-items: center; padding: 18px; border: 1px solid #e2e8f0; border-radius: 6px; background: #fff; }
.capability-title { display: flex; align-items: center; gap: 10px; }
.capability-title h3 { margin: 0; font-size: 15px; }
.capability-main p { margin: 7px 0; color: #526070; font-size: 13px; }
.capability-main small, .inherited-config small { color: #7b8794; font-size: 12px; }
.capability-config { display: grid; gap: 10px; }
.capability-config label { font-size: 12px; font-weight: 600; color: #344054; }
.capability-config input { display: block; width: 100%; margin-top: 6px; }
.capability-switch { display: flex !important; align-items: center; gap: 7px; font-weight: 500 !important; }
.capability-switch input { width: auto; margin: 0; }
.inherited-config { display: grid; gap: 4px; padding: 10px 12px; background: #f8fafc; border-radius: 4px; }
.inherited-config span { color: #667085; font-size: 12px; }
.inherited-config b { font-size: 13px; }
.capability-actions { border-top: 0; margin: 0; padding: 0; justify-content: flex-start; }
.capability-test-result { width: 100%; font-size: 12px; }
.capability-test-result.ok { color: #176b38; }
.capability-test-result.fail { color: #b42318; }
@media (max-width: 1000px) { .capability-row { grid-template-columns: 1fr; } }
</style>
