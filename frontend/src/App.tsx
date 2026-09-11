import { useMemo, useState } from 'react'
import { AppLayout } from '@hero-local/app-layout'
import { Navbar } from '@hero-local/navbar'
import { Sidebar } from '@hero-local/sidebar'
import { Button, Chip } from '@heroui/react'
import {
  Activity, AlertTriangle, ArrowLeft, Bell, Bot, BriefcaseBusiness, Building2,
  CalendarClock, Check, ChevronRight, CircleDollarSign, CircleHelp, ClipboardCheck,
  FileCheck2, FileClock, FilePenLine, Files, FileSearch, FileText, FolderCog,
  LayoutDashboard, Library, Menu, Plus, Search, Settings, ShieldCheck, Sparkles,
  UserRound, Users, WalletCards, WandSparkles, X,
} from 'lucide-react'

type Page = 'dashboard' | 'ledger' | 'create' | 'review' | 'detail' | 'fulfillment' | 'templates' | 'settings'
type Status = '草稿' | '待审查' | '审批中' | '待签署' | '履行中' | '已完成'
type Risk = '高风险' | '中风险' | '低风险' | '未审查'
type Contract = {
  id: number
  no: string
  name: string
  type: string
  party: string
  status: Status
  risk: Risk
  updated: string
  amount: number
  version: number
}

const initialContracts: Contract[] = [
  { id: 1, no: 'DEMO-CG-2026-001', name: '演示：办公设备采购合同', type: '采购合同', party: '演示供应商 A', status: '待审查', risk: '高风险', updated: '今天 10:32', amount: 286000, version: 3 },
  { id: 2, no: 'DEMO-FW-2026-004', name: '演示：年度技术服务合同', type: '服务合同', party: '演示服务商 B', status: '审批中', risk: '中风险', updated: '今天 09:18', amount: 480000, version: 2 },
  { id: 3, no: 'DEMO-LD-2026-008', name: '演示：员工劳动合同', type: '劳动合同', party: '演示员工', status: '待签署', risk: '低风险', updated: '昨天 16:40', amount: 0, version: 1 },
  { id: 4, no: 'DEMO-XS-2026-013', name: '演示：产品销售合同', type: '销售合同', party: '演示客户 C', status: '履行中', risk: '低风险', updated: '昨天 14:05', amount: 720000, version: 4 },
  { id: 5, no: 'DEMO-ZL-2026-002', name: '演示：办公场地租赁合同', type: '租赁合同', party: '演示出租方 D', status: '草稿', risk: '未审查', updated: '9 月 9 日', amount: 360000, version: 1 },
]

const navGroups = [
  { label: '合同中心', items: [
    { id: 'dashboard' as Page, label: '合同工作台', icon: LayoutDashboard },
    { id: 'ledger' as Page, label: '合同台账', icon: Files },
    { id: 'review' as Page, label: '审查与审批', icon: ShieldCheck },
    { id: 'fulfillment' as Page, label: '履约与收付款', icon: WalletCards },
  ]},
  { label: '资源中心', items: [
    { id: 'templates' as Page, label: '模板与资源', icon: Library },
  ]},
  { label: '系统管理', items: [
    { id: 'settings' as Page, label: '系统配置', icon: Settings },
  ]},
]

const money = (value: number) => value ? `¥${value.toLocaleString('zh-CN')}` : '—'

function StatusPill({ children, tone = 'neutral' }: { children: React.ReactNode; tone?: string }) {
  return <span className={`status-pill ${tone}`}>{children}</span>
}

function App() {
  const [page, setPage] = useState<Page>('dashboard')
  const [contracts, setContracts] = useState(initialContracts)
  const [selectedId, setSelectedId] = useState(1)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('全部状态')
  const [createPath, setCreatePath] = useState<'choose' | 'signed' | 'review' | 'draft'>('choose')
  const [createStep, setCreateStep] = useState(1)
  const [reviewRisk, setReviewRisk] = useState(0)
  const [reviewConfirmed, setReviewConfirmed] = useState(false)
  const [detailTab, setDetailTab] = useState('概览')

  const selected = contracts.find(item => item.id === selectedId) ?? contracts[0]
  const go = (next: Page) => {
    setPage(next)
    if (next === 'create') { setCreatePath('choose'); setCreateStep(1) }
    window.location.hash = next
  }
  const openContract = (id: number) => { setSelectedId(id); setDetailTab('概览'); go('detail') }

  const sidebar = (
    <>
      <Sidebar className="glass-sidebar">
        <Sidebar.Header>
          <div className="brand-mark"><span>H</span><div data-sidebar="label"><b>合同管理中心</b><small>HR Contract</small></div></div>
        </Sidebar.Header>
        <Sidebar.Content>
          {navGroups.map(group => (
            <Sidebar.Group key={group.label}>
              <p className="nav-label" data-sidebar="label">{group.label}</p>
              <Sidebar.Menu aria-label={group.label}>
                {group.items.map(item => (
                  <Sidebar.MenuItem key={item.id} id={item.id} href={`#${item.id}`} textValue={item.label} isCurrent={page === item.id}>
                    <Sidebar.MenuIcon><item.icon size={18} /></Sidebar.MenuIcon>
                    <Sidebar.MenuLabel>{item.label}</Sidebar.MenuLabel>
                  </Sidebar.MenuItem>
                ))}
              </Sidebar.Menu>
            </Sidebar.Group>
          ))}
        </Sidebar.Content>
        <Sidebar.Footer>
          <div className="user-card"><span className="avatar">管</span><span data-sidebar="label"><b>本地管理员</b><small>前端演示模式</small></span></div>
        </Sidebar.Footer>
        <Sidebar.Rail />
      </Sidebar>
      <Sidebar.Mobile>
        <Sidebar.Header><div className="brand-mark"><span>H</span><div><b>合同管理中心</b><small>HR Contract</small></div></div></Sidebar.Header>
        <Sidebar.Content>
          {navGroups.map(group => <Sidebar.Group key={group.label}><p className="nav-label">{group.label}</p><Sidebar.Menu aria-label={`${group.label}移动导航`}>{group.items.map(item => <Sidebar.MenuItem key={item.id} id={`mobile-${item.id}`} href={`#${item.id}`} textValue={item.label} isCurrent={page === item.id}><Sidebar.MenuIcon><item.icon size={18}/></Sidebar.MenuIcon><Sidebar.MenuLabel>{item.label}</Sidebar.MenuLabel></Sidebar.MenuItem>)}</Sidebar.Menu></Sidebar.Group>)}
        </Sidebar.Content>
      </Sidebar.Mobile>
    </>
  )

  const navbar = (
    <Navbar maxWidth="full" className="glass-navbar">
      <Navbar.Header>
        <AppLayout.MenuToggle><Menu size={18} /></AppLayout.MenuToggle>
        <Sidebar.Trigger />
        <div className="page-context"><small>合同管理</small><b>{pageTitle(page)}</b></div>
        <Navbar.Spacer />
        <Navbar.Content>
          <span className="demo-chip"><span />前端演示数据</span>
          <button className="icon-button" aria-label="帮助"><CircleHelp size={18} /></button>
          <button className="icon-button notification-button" aria-label="通知"><Bell size={18} /><i /></button>
          <Button onPress={() => go('create')}><Plus size={17} />新建合同</Button>
        </Navbar.Content>
      </Navbar.Header>
    </Navbar>
  )

  const aside = page === 'review' ? <EvidenceRail contract={selected} riskIndex={reviewRisk} /> : undefined

  return (
    <div className="aurora-stage" onClick={(event) => {
      const anchor = (event.target as HTMLElement).closest('a[href^="#"]') as HTMLAnchorElement | null
      if (anchor) { const id = anchor.hash.slice(1) as Page; if (navGroups.flatMap(g => g.items).some(i => i.id === id)) setPage(id) }
    }}>
      <div className="aurora-orb orb-one" /><div className="aurora-orb orb-two" /><div className="aurora-orb orb-three" />
      <AppLayout sidebar={sidebar} navbar={navbar} aside={aside} sidebarCollapsible="icon">
        <main className="app-main">
          {page === 'dashboard' && <Dashboard contracts={contracts} onNavigate={go} onOpen={openContract} />}
          {page === 'ledger' && <Ledger contracts={contracts} search={search} setSearch={setSearch} status={status} setStatus={setStatus} onOpen={openContract} onCreate={() => go('create')} />}
          {page === 'create' && <CreateFlow path={createPath} setPath={setCreatePath} step={createStep} setStep={setCreateStep} onCancel={() => go('ledger')} onCreated={(draft) => { setContracts(items => [draft, ...items]); setSelectedId(draft.id); go('detail') }} />}
          {page === 'review' && <ReviewWorkspace contract={selected} contracts={contracts} onSelect={setSelectedId} riskIndex={reviewRisk} setRiskIndex={setReviewRisk} confirmed={reviewConfirmed} setConfirmed={setReviewConfirmed} />}
          {page === 'detail' && <ContractDetail contract={selected} tab={detailTab} setTab={setDetailTab} onBack={() => go('ledger')} onReview={() => go('review')} />}
          {page === 'fulfillment' && <Fulfillment contracts={contracts} onOpen={openContract} />}
          {page === 'templates' && <Templates onCreate={() => { setCreatePath('draft'); setCreateStep(2); setPage('create') }} />}
          {page === 'settings' && <SystemSettings />}
        </main>
      </AppLayout>
    </div>
  )
}

function pageTitle(page: Page) {
  return ({ dashboard: '合同工作台', ledger: '合同台账', create: '新建合同', review: '审查与审批', detail: '合同详情', fulfillment: '履约与收付款', templates: '模板与资源', settings: '系统配置' })[page]
}

function PageHeader({ eyebrow, title, description, action }: { eyebrow: string; title: string; description: string; action?: React.ReactNode }) {
  return <header className="page-header"><div><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>{action}</header>
}

function Dashboard({ contracts, onNavigate, onOpen }: { contracts: Contract[]; onNavigate: (p: Page) => void; onOpen: (id: number) => void }) {
  const draft = contracts.filter(c => c.status === '草稿').length
  const pending = contracts.filter(c => ['待审查', '审批中', '待签署'].includes(c.status)).length
  const active = contracts.filter(c => c.status === '履行中').length
  return <>
    <PageHeader eyebrow="今日工作概览" title="合同工作台" description="聚焦待处理事项、风险和即将到期的合同。" action={<Button onPress={() => onNavigate('create')}><Plus size={17}/>新建合同</Button>} />
    <section className="metric-grid">
      <Metric icon={<Files />} label="合同总数" value={contracts.length} detail="当前权限范围" tone="violet" />
      <Metric icon={<FilePenLine />} label="草稿" value={draft} detail="等待继续编辑" tone="blue" />
      <Metric icon={<ClipboardCheck />} label="处理中" value={pending} detail="审查、审批或签署" tone="pink" />
      <Metric icon={<Activity />} label="履行中" value={active} detail="正在执行的合同" tone="cyan" />
    </section>
    <section className="dashboard-grid">
      <article className="glass-panel attention-panel">
        <div className="section-title"><div><h2>优先处理</h2><p>按风险和时效自动排序</p></div><button onClick={() => onNavigate('ledger')}>查看全部 <ChevronRight size={16}/></button></div>
        <div className="attention-list">
          <Attention icon={<AlertTriangle />} tone="danger" title="高风险条款待确认" detail="演示：办公设备采购合同 · 违约责任" onClick={() => onOpen(1)} />
          <Attention icon={<Bot />} tone="violet" title="AI 提取结果待确认" detail="演示：年度技术服务合同 · 12 个字段" onClick={() => onNavigate('review')} />
          <Attention icon={<CalendarClock />} tone="warning" title="付款节点即将到期" detail="演示：产品销售合同 · 3 天后到期" onClick={() => onNavigate('fulfillment')} />
        </div>
      </article>
      <article className="glass-panel pulse-panel">
        <div className="section-title"><div><h2>审查态势</h2><p>需要人工决策的风险</p></div><StatusPill tone="danger">1 项高风险</StatusPill></div>
        <div className="risk-orbit"><div className="orbit-core"><strong>3</strong><span>待处理</span></div><i className="orbit-ring one"/><i className="orbit-ring two"/></div>
        <div className="risk-legend"><span><i className="danger"/>高风险 1</span><span><i className="warning"/>中风险 1</span><span><i className="success"/>低风险 1</span></div>
      </article>
    </section>
    <article className="glass-panel recent-panel">
      <div className="section-title"><div><h2>最近更新</h2><p>继续处理最近发生变化的合同</p></div><button onClick={() => onNavigate('ledger')}>进入台账 <ChevronRight size={16}/></button></div>
      <ContractTable contracts={contracts.slice(0, 4)} onOpen={onOpen} />
    </article>
  </>
}

function Metric({ icon, label, value, detail, tone }: { icon: React.ReactNode; label: string; value: number; detail: string; tone: string }) {
  return <article className={`glass-panel metric-card ${tone}`}><div className="metric-icon">{icon}</div><span>{label}</span><strong>{value}</strong><small>{detail}</small></article>
}

function Attention({ icon, tone, title, detail, onClick }: { icon: React.ReactNode; tone: string; title: string; detail: string; onClick: () => void }) {
  return <button className="attention-item" onClick={onClick}><span className={`attention-icon ${tone}`}>{icon}</span><span><b>{title}</b><small>{detail}</small></span><ChevronRight size={17}/></button>
}

function Ledger({ contracts, search, setSearch, status, setStatus, onOpen, onCreate }: { contracts: Contract[]; search: string; setSearch: (v: string) => void; status: string; setStatus: (v: string) => void; onOpen: (id: number) => void; onCreate: () => void }) {
  const statuses = ['全部状态', '草稿', '待审查', '审批中', '待签署', '履行中', '已完成']
  const filtered = contracts.filter(c => (status === '全部状态' || c.status === status) && [c.name, c.no, c.party, c.type].join(' ').toLowerCase().includes(search.toLowerCase()))
  return <>
    <PageHeader eyebrow="统一合同记录" title="合同台账" description="草稿、待审、审批、签署与履行记录在同一条版本链中管理。" action={<Button onPress={onCreate}><Plus size={17}/>新建合同</Button>} />
    <article className="glass-panel ledger-panel">
      <div className="ledger-toolbar">
        <label className="search-box"><Search size={18}/><input aria-label="搜索合同" value={search} onChange={e => setSearch(e.target.value)} placeholder="搜索合同名称、编号或交易对方" />{search && <button onClick={() => setSearch('')} aria-label="清空搜索"><X size={15}/></button>}</label>
        <button className="filter-button"><FolderCog size={17}/>更多筛选</button>
      </div>
      <div className="status-tabs">{statuses.map(item => <button key={item} className={status === item ? 'active' : ''} onClick={() => setStatus(item)}>{item}<small>{item === '全部状态' ? contracts.length : contracts.filter(c => c.status === item).length}</small></button>)}</div>
      <div className="table-summary"><span>共 {filtered.length} 份合同</span><span>金额合计 {money(filtered.reduce((sum, c) => sum + c.amount, 0))}</span></div>
      {filtered.length ? <ContractTable contracts={filtered} onOpen={onOpen} /> : <EmptyState title="没有匹配的合同" detail="调整搜索关键词或筛选条件后重试。" />}
    </article>
  </>
}

function ContractTable({ contracts, onOpen }: { contracts: Contract[]; onOpen: (id: number) => void }) {
  return <div className="contract-table"><div className="table-head"><span>合同</span><span>类型 / 对方</span><span>业务状态</span><span>AI 风险</span><span>金额</span><span>更新时间</span></div>{contracts.map(c => <button className="table-row" key={c.id} onClick={() => onOpen(c.id)}><span><b>{c.name}</b><small>{c.no} · v{c.version}</small></span><span><b>{c.type}</b><small>{c.party}</small></span><span><StatusPill tone={statusTone(c.status)}>{c.status}</StatusPill></span><span><StatusPill tone={riskTone(c.risk)}>{c.risk}</StatusPill></span><span className="money">{money(c.amount)}</span><span>{c.updated}<ChevronRight size={16}/></span></button>)}</div>
}

function CreateFlow({ path, setPath, step, setStep, onCancel, onCreated }: { path: 'choose'|'signed'|'review'|'draft'; setPath: (p: 'choose'|'signed'|'review'|'draft') => void; step: number; setStep: (n: number) => void; onCancel: () => void; onCreated: (c: Contract) => void }) {
  const [fileName, setFileName] = useState('')
  const [name, setName] = useState('')
  const [party, setParty] = useState('')
  if (path === 'choose') return <>
    <PageHeader eyebrow="选择合同当前阶段" title="新建合同" description="三条路径最终进入同一合同台账，并保留完整版本与处理记录。" action={<button className="ghost-action" onClick={onCancel}><ArrowLeft size={17}/>返回台账</button>} />
    <section className="path-grid">
      <PathCard icon={<FileCheck2/>} title="登记已签合同" detail="历史或线下已签合同，上传原件后进行信息提取、人工确认与履约登记。" steps="上传原件 → AI 提取 → 人工确认 → 登记" onClick={() => setPath('signed')} tone="cyan" />
      <PathCard icon={<FileSearch/>} title="上传待审合同" detail="对方提供的未签文件，完成 AI 提取、法律审查和外部审批后再签署。" steps="上传文件 → 提取确认 → 法律审查 → 审批" onClick={() => setPath('review')} tone="violet" featured />
      <PathCard icon={<WandSparkles/>} title="从模板起草" detail="选择标准模板或多个参考模板，通过 AI 助手形成合同初稿。" steps="选择模板 → 填写信息 → AI 辅助起草 → 保存" onClick={() => setPath('draft')} tone="pink" />
    </section>
  </>
  const titles = { signed: '登记已签合同', review: '上传待审合同', draft: '从模板起草' }
  const stepLabels = path === 'draft' ? ['选择模板', '填写信息', '编辑正文', '保存草稿'] : ['上传文件', 'AI 提取', '人工确认', path === 'signed' ? '完成登记' : '进入审查']
  const finish = () => onCreated({ id: Date.now(), no: `DEMO-${path.toUpperCase()}-${String(Date.now()).slice(-4)}`, name: name || `演示：${titles[path]}`, type: path === 'draft' ? '服务合同' : '采购合同', party: party || '待补充交易对方', status: path === 'review' ? '待审查' : path === 'signed' ? '履行中' : '草稿', risk: path === 'review' ? '未审查' : '低风险', updated: '刚刚', amount: 0, version: 1 })
  return <>
    <PageHeader eyebrow="新建合同" title={titles[path]} description="当前操作仅生成前端演示记录，AI 结果仍需人工确认。" action={<button className="ghost-action" onClick={() => setPath('choose')}><ArrowLeft size={17}/>重新选择</button>} />
    <div className="flow-steps">{stepLabels.map((label, index) => <div key={label} className={`${step === index + 1 ? 'active' : ''} ${step > index + 1 ? 'done' : ''}`}><span>{step > index + 1 ? <Check size={15}/> : index + 1}</span><b>{label}</b></div>)}</div>
    <article className="glass-panel create-panel">
      {step === 1 && path !== 'draft' && <label className="drop-zone"><input type="file" onChange={e => setFileName(e.target.files?.[0]?.name ?? '')}/><span className="drop-icon"><FileText size={28}/></span><b>{fileName || '拖放合同文件到这里'}</b><small>{fileName ? '文件已加入前端演示流程' : '支持 Word、PDF 或扫描件，单个文件不超过 50 MB'}</small><em>选择文件</em></label>}
      {step === 1 && path === 'draft' && <div className="template-selector"><button className="template-choice selected"><span><FileText/><b>通用服务合同模板</b></span><small>已发布 · v4 · 适用于服务合同</small><Check size={18}/></button><button className="template-choice"><span><Files/><b>选择多个参考模板</b></span><small>AI 仅将所选模板作为起草参考</small><Plus size={18}/></button></div>}
      {step === 2 && <div className="extract-layout"><div className="document-skeleton"><span/><span/><span/><span/><span/></div><div className="extract-progress"><span className="ai-orb"><Sparkles/></span><h2>{path === 'draft' ? '填写合同基础信息' : 'AI 正在提取合同信息'}</h2><p>{path === 'draft' ? '这些字段将作为模板变量写入初稿。' : '演示流程已识别 12 个字段，下一步由你逐项确认。'}</p></div></div>}
      {step === 3 && <div className="form-layout"><div className="form-fields"><label>合同名称<input value={name} onChange={e => setName(e.target.value)} placeholder="请输入合同名称"/></label><label>交易对方<input value={party} onChange={e => setParty(e.target.value)} placeholder="搜索或填写交易对方"/></label><label>合同类型<select defaultValue="采购合同"><option>采购合同</option><option>服务合同</option><option>销售合同</option><option>劳动合同</option></select></label><label>合同金额<input placeholder="0.00" inputMode="decimal"/></label></div><aside className="source-card"><Sparkles size={18}/><b>{path === 'draft' ? 'AI 起草提示' : '字段来源证据'}</b><p>{path === 'draft' ? 'AI 建议不会自动写入正式合同，保存前可继续编辑。' : '点击字段后可在原文件中查看对应页码与文字位置。'}</p></aside></div>}
      {step === 4 && <div className="complete-state"><span><Check size={30}/></span><h2>信息已准备完成</h2><p>{path === 'review' ? '保存后进入待审查状态。' : path === 'signed' ? '保存后进入履约管理。' : '保存为草稿，可继续编辑正文并发起审查。'}</p></div>}
      <footer className="flow-actions"><button className="ghost-action" disabled={step === 1} onClick={() => setStep(Math.max(1, step - 1))}>上一步</button><div><span>第 {step} / 4 步</span><Button onPress={() => step < 4 ? setStep(step + 1) : finish()}>{step < 4 ? '下一步' : '保存并查看'}<ChevronRight size={17}/></Button></div></footer>
    </article>
  </>
}

function PathCard({ icon, title, detail, steps, onClick, tone, featured }: { icon: React.ReactNode; title: string; detail: string; steps: string; onClick: () => void; tone: string; featured?: boolean }) {
  return <button className={`path-card ${tone} ${featured ? 'featured' : ''}`} onClick={onClick}>{featured && <em>推荐流程</em>}<span className="path-icon">{icon}</span><h2>{title}</h2><p>{detail}</p><small>{steps}</small><b>开始办理 <ChevronRight size={17}/></b></button>
}

const risks = [
  { level: '高风险', title: '违约责任上限缺失', clause: '第十二条 违约责任', excerpt: '任何一方违反本合同约定，应赔偿对方因此遭受的全部损失。', advice: '建议明确直接损失范围，并约定累计赔偿责任上限。', basis: '采购合同风险规则 R-12 · v3.2' },
  { level: '中风险', title: '验收期限表述不明确', clause: '第六条 验收', excerpt: '甲方应在货物到达后及时完成验收。', advice: '将“及时”调整为明确工作日，并约定逾期反馈的处理方式。', basis: '履约期限规则 R-07 · v2.1' },
  { level: '低风险', title: '通知方式可以补充', clause: '第十五条 通知', excerpt: '双方通知应以书面形式送达。', advice: '可补充约定有效电子邮箱和送达时间认定。', basis: '合同完整性规则 R-21 · v1.4' },
]

function ReviewWorkspace({ contract, contracts, onSelect, riskIndex, setRiskIndex, confirmed, setConfirmed }: { contract: Contract; contracts: Contract[]; onSelect: (id: number) => void; riskIndex: number; setRiskIndex: (n: number) => void; confirmed: boolean; setConfirmed: (v: boolean) => void }) {
  return <>
    <PageHeader eyebrow="版本绑定的审查证据" title="AI 合同审查" description="AI 负责定位和建议，最终判断由经办人与法务完成。" action={<div className="review-actions"><button className="ghost-action"><Sparkles size={17}/>重新审查</button><Button onPress={() => setConfirmed(true)} isDisabled={confirmed}>{confirmed ? <><Check size={17}/>已确认结果</> : '确认审查结果'}</Button></div>} />
    <div className="review-contract-switcher"><FileSearch size={18}/><select value={contract.id} onChange={e => { onSelect(Number(e.target.value)); setRiskIndex(0); setConfirmed(false) }}>{contracts.filter(c => c.status !== '已完成').map(c => <option key={c.id} value={c.id}>{c.name} · v{c.version}</option>)}</select><StatusPill tone="violet">AI 结果仅供辅助</StatusPill></div>
    <section className="review-grid">
      <article className="document-view glass-panel"><div className="document-toolbar"><span><FileText size={17}/>{contract.no} · v{contract.version}</span><span>第 4 / 12 页</span></div><div className="paper"><h2>采购合同</h2><p>甲方（采购方）：演示采购方</p><p>乙方（供应方）：{contract.party}</p><h3>第六条 验收</h3><p className={riskIndex === 1 ? 'highlight warning' : ''}>甲方应在货物到达后及时完成验收。如发现质量问题，应通知乙方处理。</p><h3>第十二条 违约责任</h3><p className={riskIndex === 0 ? 'highlight danger' : ''}>任何一方违反本合同约定，应赔偿对方因此遭受的全部损失。</p><h3>第十五条 通知</h3><p className={riskIndex === 2 ? 'highlight success' : ''}>双方通知应以书面形式送达，送达地址以合同首页记载为准。</p></div></article>
      <article className="risk-list glass-panel"><div className="section-title"><div><h2>审查发现</h2><p>3 项需要人工判断</p></div><StatusPill tone="danger">整体高风险</StatusPill></div>{risks.map((risk, index) => <button key={risk.title} className={riskIndex === index ? 'risk-item active' : 'risk-item'} onClick={() => setRiskIndex(index)}><span className={`risk-dot ${riskTone(risk.level as Risk)}`}/><span><b>{risk.title}</b><small>{risk.clause}</small><p>{risk.advice}</p></span><ChevronRight size={17}/></button>)}</article>
    </section>
  </>
}

function EvidenceRail({ contract, riskIndex }: { contract: Contract; riskIndex: number }) {
  const risk = risks[riskIndex]
  return <aside className="evidence-rail"><div className="evidence-head"><span className="evidence-icon"><FileSearch/></span><div><small>证据玻璃轨</small><h2>{risk.title}</h2></div></div><div className="evidence-section"><span>原文位置</span><b>{risk.clause} · 第 4 页</b><blockquote>{risk.excerpt}</blockquote></div><div className="evidence-section"><span>判断依据</span><b>{risk.basis}</b><p>结果绑定 {contract.no} 的内容版本 v{contract.version}，合同版本变化后需要重新审查。</p></div><div className="evidence-section"><span>处理建议</span><p>{risk.advice}</p></div><div className="evidence-timeline"><span className="done"><Check size={13}/></span><div><b>AI 审查完成</b><small>演示任务 · 今天 10:28</small></div><span/><div><b>等待人工确认</b><small>当前处理节点</small></div></div></aside>
}

function ContractDetail({ contract, tab, setTab, onBack, onReview }: { contract: Contract; tab: string; setTab: (v: string) => void; onBack: () => void; onReview: () => void }) {
  const tabs = ['概览', '正文与版本', '智能与审批', '履约与收付款', '附件与变更', '操作记录']
  return <>
    <button className="back-link" onClick={onBack}><ArrowLeft size={17}/>返回合同台账</button>
    <header className="contract-identity"><div><div className="identity-tags"><StatusPill tone={statusTone(contract.status)}>{contract.status}</StatusPill><StatusPill tone={riskTone(contract.risk)}>{contract.risk}</StatusPill><span>内容版本 v{contract.version}</span></div><h1>{contract.name}</h1><p>{contract.no} · {contract.type} · {contract.party}</p></div><div><button className="ghost-action" onClick={onReview}><ShieldCheck size={17}/>查看审查</button><Button onPress={() => setTab('正文与版本')}>继续处理</Button></div></header>
    <div className="detail-tabs">{tabs.map(item => <button key={item} className={tab === item ? 'active' : ''} onClick={() => setTab(item)}>{item}</button>)}</div>
    <DetailContent contract={contract} tab={tab} onReview={onReview} />
  </>
}

function DetailContent({ contract, tab, onReview }: { contract: Contract; tab: string; onReview: () => void }) {
  if (tab === '概览') return <section className="detail-overview"><article className="glass-panel decision-card"><span>建议下一步</span><div><ShieldCheck/><div><h2>确认高风险审查意见</h2><p>违约责任条款缺少责任上限，需要法务确认后再提交审批。</p></div></div><Button onPress={onReview}>进入审查工作台</Button></article><article className="glass-panel info-card"><h2>合同信息</h2><dl><div><dt>合同类型</dt><dd>{contract.type}</dd></div><div><dt>交易对方</dt><dd>{contract.party}</dd></div><div><dt>合同金额</dt><dd>{money(contract.amount)}</dd></div><div><dt>当前版本</dt><dd>v{contract.version}</dd></div><div><dt>最后更新</dt><dd>{contract.updated}</dd></div></dl></article><article className="glass-panel timeline-card"><h2>最近记录</h2><TimelineRows /></article></section>
  if (tab === '正文与版本') return <section className="two-column"><article className="glass-panel version-preview"><div className="section-title"><div><h2>当前合同正文</h2><p>内容版本 v{contract.version}</p></div><button>下载当前版本</button></div><div className="mini-paper"><h3>{contract.name}</h3><p>本区域用于展示当前生效的合同内容版本。所有编辑、上传和 AI 处理结果都会绑定独立版本，不覆盖历史内容。</p></div></article><article className="glass-panel version-list"><h2>版本记录</h2>{[contract.version, Math.max(1,contract.version-1),1].filter((v,i,a)=>a.indexOf(v)===i).map((v,i)=><div key={v}><span><FileText/><b>内容版本 v{v}</b><small>{i===0?'当前版本 · 今天 10:26':'历史版本 · 演示记录'}</small></span>{i===0?<StatusPill tone="success">当前</StatusPill>:<button>查看</button>}</div>)}</article></section>
  if (tab === '智能与审批') return <section className="detail-overview"><article className="glass-panel info-card wide"><div className="section-title"><div><h2>AI 与审批状态</h2><p>各类状态相互独立，不由 AI 自动改变合同状态。</p></div><Button onPress={onReview}>查看审查证据</Button></div><div className="state-flow"><span className="done"><Check/>内容版本</span><i/><span className="done"><Check/>AI 提取</span><i/><span className="current"><ShieldCheck/>法律审查</span><i/><span><ClipboardCheck/>外部审批</span><i/><span><FileCheck2/>签署生效</span></div></article></section>
  if (tab === '履约与收付款') return <Fulfillment contracts={[contract]} onOpen={() => undefined} embedded />
  return <section className="detail-overview"><article className="glass-panel info-card wide"><h2>{tab}</h2><EmptyState title={`${tab}暂无演示记录`} detail="后续真实操作产生的数据会按权限显示在这里。" /></article></section>
}

function Fulfillment({ contracts, onOpen, embedded = false }: { contracts: Contract[]; onOpen: (id:number)=>void; embedded?: boolean }) {
  const active = contracts.filter(c => c.status === '履行中' || embedded)
  return <>{!embedded && <PageHeader eyebrow="执行与资金计划" title="履约与收付款" description="跟踪交付、验收、收付款计划和异常节点。" />}<section className="fulfillment-grid"><article className="glass-panel fulfillment-summary"><div className="section-title"><div><h2>待办节点</h2><p>按到期时间排序</p></div><StatusPill tone="warning">1 项临近到期</StatusPill></div><div className="milestone"><span className="milestone-date"><b>14</b><small>SEP</small></span><div><b>演示：首期付款</b><small>演示：产品销售合同 · ¥216,000</small></div><StatusPill tone="warning">3 天后</StatusPill></div><div className="milestone"><span className="milestone-date"><b>20</b><small>SEP</small></span><div><b>演示：设备到货验收</b><small>演示：办公设备采购合同</small></div><StatusPill>9 天后</StatusPill></div></article><article className="glass-panel payment-health"><h2>资金计划</h2><div className="payment-value"><span>计划金额</span><strong>¥936,000</strong><small>演示数据 · 已执行 62%</small></div><div className="progress"><i style={{width:'62%'}}/></div><div className="payment-split"><span>已执行<b>¥580,320</b></span><span>待执行<b>¥355,680</b></span></div></article></section>{active.length > 0 && !embedded && <article className="glass-panel recent-panel"><div className="section-title"><div><h2>履行中合同</h2><p>点击进入合同详情</p></div></div><ContractTable contracts={active} onOpen={onOpen}/></article>}</>
}

function Templates({ onCreate }: { onCreate: () => void }) {
  const cards = [{name:'通用服务合同模板',type:'服务合同',version:'v4',tag:'常用'},{name:'标准采购合同模板',type:'采购合同',version:'v7',tag:'已发布'},{name:'产品销售合同模板',type:'销售合同',version:'v3',tag:'已发布'}]
  return <><PageHeader eyebrow="受控合同资源" title="模板与资源" description="模板、分类、知识和规则集中管理；已创建合同不受后续模板修改影响。" action={<Button onPress={onCreate}><Plus size={17}/>使用模板起草</Button>} /><div className="resource-tabs"><button className="active">合同模板</button><button>合同分类</button><button>知识库</button><button>规则库</button><button>交易对方</button></div><section className="template-grid">{cards.map((card,index)=><article className="glass-panel template-card" key={card.name}><div className={`template-cover cover-${index+1}`}><FileText/></div><div><span><StatusPill tone="success">{card.tag}</StatusPill><small>{card.type}</small></span><h2>{card.name}</h2><p>{card.version} · 演示模板 · 最近更新 2026-09</p><button onClick={onCreate}>使用此模板 <ChevronRight size={16}/></button></div></article>)}</section></>
}

function SystemSettings() {
  const sections = [{icon:<Bot/>,title:'AI 能力配置',desc:'解析、法律审查与起草 Agent 的服务状态。',meta:'3 个能力已配置'},{icon:<ShieldCheck/>,title:'权限与数据范围',desc:'按角色、组织、合同和字段控制访问。',meta:'5 个角色'},{icon:<Building2/>,title:'外部连接',desc:'主体、文件和审批服务的连接状态。',meta:'2 个待对接'},{icon:<Users/>,title:'账号管理',desc:'账号、角色和默认数据范围。',meta:'本地演示模式'},{icon:<ClipboardCheck/>,title:'操作日志',desc:'只读查看关键操作和数据访问记录。',meta:'保留审计证据'},{icon:<FolderCog/>,title:'合同基础配置',desc:'分类、编号规则和履约事项配置。',meta:'8 个合同类型'}]
  return <><PageHeader eyebrow="管理员可见" title="系统配置" description="业务参数与审计分区管理，密钥和服务地址不会暴露在前端。"/><section className="settings-grid">{sections.map(s=><button className="glass-panel settings-card" key={s.title}><span>{s.icon}</span><div><h2>{s.title}</h2><p>{s.desc}</p><small>{s.meta}</small></div><ChevronRight/></button>)}</section></>
}

function TimelineRows() { return <div className="timeline-rows"><div><span><Check/></span><p><b>AI 提取完成</b><small>12 个字段等待人工确认 · 今天 10:26</small></p></div><div><span><FileText/></span><p><b>生成内容版本 v3</b><small>由演示经办人上传 · 今天 10:24</small></p></div><div><span><BriefcaseBusiness/></span><p><b>合同草稿创建</b><small>前端演示记录 · 昨天 16:08</small></p></div></div> }

function EmptyState({ title, detail }: { title: string; detail: string }) { return <div className="empty-state"><span><FileClock/></span><b>{title}</b><p>{detail}</p></div> }
function statusTone(status: Status) { return ({草稿:'neutral',待审查:'violet',审批中:'warning',待签署:'blue',履行中:'success',已完成:'neutral'})[status] }
function riskTone(risk: Risk) { return ({高风险:'danger',中风险:'warning',低风险:'success',未审查:'neutral'})[risk] }

export default App
