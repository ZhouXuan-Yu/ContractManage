import { useEffect, useMemo, useState } from "react";
import { AppLayout } from "@hero-local/app-layout";
import { Sidebar } from "@hero-local/sidebar";
import { Navbar } from "@hero-local/navbar";
import { KPI } from "@hero-local/kpi";
import { KPIGroup } from "@hero-local/kpi-group";
import { DataGrid, type DataGridColumn } from "@hero-local/data-grid";
import { Stepper } from "@hero-local/stepper";
import { Resizable } from "@hero-local/resizable";
import { AreaChart } from "@hero-local/area-chart";
import { Button, Chip } from "@heroui/react";
import {
  AlertTriangle,
  ArrowLeft,
  BarChart3,
  Bell,
  BookOpen,
  Bot,
  Building2,
  CalendarClock,
  Check,
  ChevronRight,
  CircleDollarSign,
  ClipboardCheck,
  Database,
  FileCheck2,
  FileClock,
  FilePenLine,
  Files,
  FileSearch,
  FileText,
  FolderCog,
  Gauge,
  LayoutDashboard,
  Library,
  Menu,
  Plus,
  Search,
  Settings,
  ShieldCheck,
  SlidersHorizontal,
  Sparkles,
  Upload,
  UserRound,
  Users,
  WalletCards,
  X,
} from "lucide-react";

type Page =
  | "dashboard"
  | "ledger"
  | "create"
  | "detail"
  | "review"
  | "approval"
  | "fulfillment"
  | "cockpit"
  | "templates"
  | "parties"
  | "knowledge"
  | "rules"
  | "settings";
type ContractStatus =
  "草稿" | "待审查" | "审批中" | "待签署" | "履行中" | "已完成" | "已终止";
type ApprovalStatus = "未提交" | "审批中" | "已通过" | "已驳回";
type RiskLevel = "高" | "中" | "低" | "未审查";
type Contract = {
  id: number;
  no: string;
  name: string;
  type: string;
  category: string;
  party: string;
  owner: string;
  status: ContractStatus;
  approval: ApprovalStatus;
  risk: RiskLevel;
  amount: number;
  version: number;
  updated: string;
  expire: string;
};

const seedContracts: Contract[] = [
  {
    id: 1,
    no: "DEMO-CG-2026-001",
    name: "演示：办公设备采购合同",
    type: "采购合同",
    category: "采购",
    party: "演示供应商 A",
    owner: "采购部",
    status: "待审查",
    approval: "未提交",
    risk: "高",
    amount: 286000,
    version: 3,
    updated: "今天 10:32",
    expire: "2027-09-30",
  },
  {
    id: 2,
    no: "DEMO-FW-2026-004",
    name: "演示：年度技术服务合同",
    type: "服务合同",
    category: "服务",
    party: "演示服务商 B",
    owner: "信息部",
    status: "审批中",
    approval: "审批中",
    risk: "中",
    amount: 480000,
    version: 2,
    updated: "今天 09:18",
    expire: "2027-01-31",
  },
  {
    id: 3,
    no: "DEMO-LD-2026-008",
    name: "演示：员工劳动合同",
    type: "劳动合同",
    category: "人力",
    party: "演示员工",
    owner: "人力资源部",
    status: "待签署",
    approval: "已通过",
    risk: "低",
    amount: 0,
    version: 1,
    updated: "昨天 16:40",
    expire: "2029-09-10",
  },
  {
    id: 4,
    no: "DEMO-XS-2026-013",
    name: "演示：产品销售合同",
    type: "销售合同",
    category: "销售",
    party: "演示客户 C",
    owner: "销售部",
    status: "履行中",
    approval: "已通过",
    risk: "低",
    amount: 720000,
    version: 4,
    updated: "昨天 14:05",
    expire: "2027-03-18",
  },
  {
    id: 5,
    no: "DEMO-ZL-2026-002",
    name: "演示：办公场地租赁合同",
    type: "租赁合同",
    category: "行政",
    party: "演示出租方 D",
    owner: "行政部",
    status: "草稿",
    approval: "未提交",
    risk: "未审查",
    amount: 360000,
    version: 1,
    updated: "9 月 9 日",
    expire: "2028-08-31",
  },
  {
    id: 6,
    no: "DEMO-CG-2025-021",
    name: "演示：耗材框架采购合同",
    type: "采购合同",
    category: "采购",
    party: "演示供应商 E",
    owner: "采购部",
    status: "已完成",
    approval: "已通过",
    risk: "低",
    amount: 156000,
    version: 5,
    updated: "9 月 3 日",
    expire: "2026-08-31",
  },
];

const nav = [
  {
    group: "合同中心",
    items: [
      ["dashboard", "合同工作台", LayoutDashboard],
      ["ledger", "合同台账", Files],
      ["approval", "审批中心", ClipboardCheck],
      ["fulfillment", "履约与收付款", WalletCards],
      ["cockpit", "经营数据大屏", Gauge],
    ],
  },
  {
    group: "资源中心",
    items: [
      ["templates", "合同模板", Library],
      ["parties", "交易对方", Building2],
      ["knowledge", "知识库", BookOpen],
      ["rules", "规则库", ShieldCheck],
    ],
  },
  { group: "系统管理", items: [["settings", "系统配置", Settings]] },
] as const;

const labels: Record<Page, string> = {
  dashboard: "合同工作台",
  ledger: "合同台账",
  create: "新建合同",
  detail: "合同详情",
  review: "AI 审查",
  approval: "审批中心",
  fulfillment: "履约与收付款",
  cockpit: "经营数据大屏",
  templates: "合同模板",
  parties: "交易对方",
  knowledge: "知识库",
  rules: "规则库",
  settings: "系统配置",
};
const money = (n: number) => (n ? `¥${n.toLocaleString("zh-CN")}` : "—");

function Tag({
  children,
  tone = "gray",
}: {
  children: React.ReactNode;
  tone?: string;
}) {
  return <span className={`tag ${tone}`}>{children}</span>;
}
function Header({
  index,
  title,
  desc,
  action,
}: {
  index: string;
  title: string;
  desc: string;
  action?: React.ReactNode;
}) {
  return (
    <header className="page-head">
      <div className="page-index">{index}</div>
      <div>
        <h1>{title}</h1>
        <p>{desc}</p>
      </div>
      {action && <div className="page-action">{action}</div>}
    </header>
  );
}

export default function SwissApp() {
  const [page, setPage] = useState<Page>(() => {
    const id = window.location.hash.slice(1) as Page;
    return labels[id] ? id : "dashboard";
  });
  const [contracts, setContracts] = useState(seedContracts);
  const [selectedId, setSelectedId] = useState(1);
  const [createSource, setCreateSource] = useState<
    "signed" | "review" | "draft" | null
  >(null);
  const [toast, setToast] = useState("");
  const selected = contracts.find((x) => x.id === selectedId) ?? contracts[0];
  const go = (p: Page) => {
    setPage(p);
    window.location.hash = p;
    window.scrollTo({ top: 0 });
  };
  const open = (id: number) => {
    setSelectedId(id);
    go("detail");
  };
  const flash = (message: string) => {
    setToast(message);
    window.setTimeout(() => setToast(""), 2400);
  };
  useEffect(() => {
    const sync = () => {
      const id = window.location.hash.slice(1) as Page;
      if (labels[id]) {
        setPage(id);
        window.scrollTo({ top: 0 });
      }
    };
    window.addEventListener("hashchange", sync);
    return () => window.removeEventListener("hashchange", sync);
  }, []);

  const side = (
    <>
      <Sidebar className="swiss-side">
        <Sidebar.Header>
          <div className="brand">
            <span>HC</span>
            <div data-sidebar="label">
              <b>合同管理中心</b>
              <small>HR CONTRACT</small>
            </div>
          </div>
        </Sidebar.Header>
        <Sidebar.Content>
          {nav.map((group) => (
            <Sidebar.Group key={group.group}>
              <p className="group-label" data-sidebar="label">
                {group.group}
              </p>
              <Sidebar.Menu aria-label={group.group}>
                {group.items.map(([id, label, Icon]) => (
                  <Sidebar.MenuItem
                    key={id}
                    id={id}
                    href={`#${id}`}
                    textValue={label}
                    isCurrent={page === id}
                  >
                    <Sidebar.MenuIcon>
                      <Icon size={17} />
                    </Sidebar.MenuIcon>
                    <Sidebar.MenuLabel>{label}</Sidebar.MenuLabel>
                  </Sidebar.MenuItem>
                ))}
              </Sidebar.Menu>
            </Sidebar.Group>
          ))}
        </Sidebar.Content>
        <Sidebar.Footer>
          <div className="profile">
            <span>管</span>
            <div data-sidebar="label">
              <b>本地管理员</b>
              <small>前端演示环境</small>
            </div>
          </div>
        </Sidebar.Footer>
        <Sidebar.Rail />
      </Sidebar>
      <Sidebar.Mobile>
        <Sidebar.Header>
          <div className="brand">
            <span>HC</span>
            <div>
              <b>合同管理中心</b>
              <small>HR CONTRACT</small>
            </div>
          </div>
        </Sidebar.Header>
        <Sidebar.Content>
          {nav.map((group) => (
            <Sidebar.Group key={group.group}>
              <p className="group-label">{group.group}</p>
              <Sidebar.Menu aria-label={`${group.group}移动导航`}>
                {group.items.map(([id, label, Icon]) => (
                  <Sidebar.MenuItem
                    key={id}
                    id={`m-${id}`}
                    href={`#${id}`}
                    textValue={label}
                    isCurrent={page === id}
                  >
                    <Sidebar.MenuIcon>
                      <Icon size={17} />
                    </Sidebar.MenuIcon>
                    <Sidebar.MenuLabel>{label}</Sidebar.MenuLabel>
                  </Sidebar.MenuItem>
                ))}
              </Sidebar.Menu>
            </Sidebar.Group>
          ))}
        </Sidebar.Content>
      </Sidebar.Mobile>
    </>
  );

  const top = (
    <Navbar maxWidth="full" className="swiss-nav">
      <Navbar.Header>
        <AppLayout.MenuToggle>
          <Menu size={18} />
        </AppLayout.MenuToggle>
        <Sidebar.Trigger />
        <span className="crumb">
          合同管理 <i>/</i> <b>{labels[page]}</b>
        </span>
        <Navbar.Spacer />
        <Navbar.Content>
          <span className="env">
            <i />
            演示环境
          </span>
          <button className="icon-btn" aria-label="通知">
            <Bell size={18} />
            <em />
          </button>
          <Button
            onPress={() => {
              setCreateSource(null);
              go("create");
            }}
          >
            <Plus size={16} />
            新建合同
          </Button>
        </Navbar.Content>
      </Navbar.Header>
    </Navbar>
  );

  return (
    <div
      className="swiss-root"
      onClick={(e) => {
        const a = (e.target as HTMLElement).closest(
          'a[href^="#"]',
        ) as HTMLAnchorElement | null;
        if (a) {
          const id = a.hash.slice(1) as Page;
          if (labels[id]) setPage(id);
        }
      }}
    >
      <AppLayout
        sidebar={side}
        navbar={top}
        sidebarCollapsible="icon"
        navigate={(href) => {
          const id = href.replace(/^#/, "") as Page;
          if (labels[id]) setPage(id);
        }}
      >
        <main className={page === "cockpit" ? "main cockpit-main" : "main"}>
          {page === "dashboard" && (
            <Dashboard contracts={contracts} go={go} open={open} />
          )}{" "}
          {page === "ledger" && (
            <Ledger
              contracts={contracts}
              open={open}
              create={() => {
                setCreateSource(null);
                go("create");
              }}
            />
          )}
          {page === "create" && (
            <CreateContract
              source={createSource}
              setSource={setCreateSource}
              cancel={() => go("ledger")}
              complete={(c) => {
                setContracts((v) => [c, ...v]);
                setSelectedId(c.id);
                flash("合同草稿已建立");
                go("detail");
              }}
            />
          )}
          {page === "detail" && (
            <Detail
              contract={selected}
              back={() => go("ledger")}
              review={() => go("review")}
              flash={flash}
            />
          )}{" "}
          {page === "review" && (
            <Review
              contract={selected}
              back={() => go("detail")}
              flash={flash}
            />
          )}
          {page === "approval" && (
            <ApprovalCenter contracts={contracts} open={open} flash={flash} />
          )}{" "}
          {page === "fulfillment" && (
            <Fulfillment contracts={contracts} open={open} flash={flash} />
          )}{" "}
          {page === "cockpit" && <Cockpit contracts={contracts} />}
          {page === "templates" && (
            <Templates
              start={() => {
                setCreateSource("draft");
                go("create");
              }}
            />
          )}{" "}
          {page === "parties" && <Parties />}{" "}
          {page === "knowledge" && <Knowledge />}{" "}
          {page === "rules" && <Rules />}{" "}
          {page === "settings" && <SettingsPage />}
        </main>
      </AppLayout>
      {toast && (
        <div className="toast">
          <Check size={16} />
          {toast}
        </div>
      )}
    </div>
  );
}

function Dashboard({
  contracts,
  go,
  open,
}: {
  contracts: Contract[];
  go: (p: Page) => void;
  open: (id: number) => void;
}) {
  const total = contracts.length,
    draft = contracts.filter((x) => x.status === "草稿").length,
    sign = contracts.filter((x) => x.status === "待签署").length,
    active = contracts.filter((x) => x.status === "履行中").length;
  return (
    <>
      <Header
        index="01"
        title="合同工作台"
        desc="今天需要处理的合同、风险和到期事项。"
        action={
          <Button onPress={() => go("create")}>
            <Plus size={16} />
            新建合同
          </Button>
        }
      />
      <KPIGroup className="kpi-strip">
        <KPI>
          <KPI.Header>
            <KPI.Title>合同总数</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value value={total} />
          </KPI.Content>
          <KPI.Footer>当前权限范围</KPI.Footer>
        </KPI>
        <KPIGroup.Separator />
        <KPI>
          <KPI.Header>
            <KPI.Title>草稿</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value value={draft} />
          </KPI.Content>
          <KPI.Footer>等待继续编辑</KPI.Footer>
        </KPI>
        <KPIGroup.Separator />
        <KPI>
          <KPI.Header>
            <KPI.Title>待签署</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value value={sign} />
          </KPI.Content>
          <KPI.Footer>审批已经通过</KPI.Footer>
        </KPI>
        <KPIGroup.Separator />
        <KPI>
          <KPI.Header>
            <KPI.Title>履行中</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value value={active} />
          </KPI.Content>
          <KPI.Footer>执行中的合同</KPI.Footer>
        </KPI>
      </KPIGroup>
      <div className="work-grid">
        <section className="panel tasks">
          <SectionTitle
            no="A"
            title="我的待办"
            action="查看全部"
            onAction={() => go("ledger")}
          />
          <Task
            tone="red"
            icon={<AlertTriangle />}
            title="确认高风险审查意见"
            detail="演示：办公设备采购合同 · 违约责任"
            meta="今天"
            onClick={() => {
              open(1);
            }}
          />
          <Task
            tone="blue"
            icon={<Bot />}
            title="确认 AI 提取结果"
            detail="演示：年度技术服务合同 · 12 个字段"
            meta="今天"
            onClick={() => go("review")}
          />
          <Task
            tone="gray"
            icon={<FileCheck2 />}
            title="补充签署文件"
            detail="演示：员工劳动合同"
            meta="明天"
            onClick={() => open(3)}
          />
        </section>
        <section className="panel stage">
          <SectionTitle no="B" title="生命周期概览" />
          <div className="stage-bars">
            {[
              ["草稿", 1, 16],
              ["审查与审批", 2, 33],
              ["待签署", 1, 16],
              ["履行中", 1, 16],
              ["已完成", 1, 16],
            ].map(([n, c, w]) => (
              <button key={String(n)} onClick={() => go("ledger")}>
                <span>
                  <b>{c}</b>
                  <small>{n}</small>
                </span>
                <i style={{ width: `${w}%` }} />
              </button>
            ))}
          </div>
        </section>
      </div>
      <section className="panel recent">
        <SectionTitle
          no="C"
          title="最近更新合同"
          action="进入合同台账"
          onAction={() => go("ledger")}
        />
        <MiniTable contracts={contracts.slice(0, 5)} open={open} />
      </section>
    </>
  );
}

function SectionTitle({
  no,
  title,
  action,
  onAction,
}: {
  no: string;
  title: string;
  action?: string;
  onAction?: () => void;
}) {
  return (
    <div className="section-title">
      <span>{no}</span>
      <h2>{title}</h2>
      {action && (
        <button onClick={onAction}>
          {action}
          <ChevronRight size={15} />
        </button>
      )}
    </div>
  );
}
function Task({
  tone,
  icon,
  title,
  detail,
  meta,
  onClick,
}: {
  tone: string;
  icon: React.ReactNode;
  title: string;
  detail: string;
  meta: string;
  onClick: () => void;
}) {
  return (
    <button className="task" onClick={onClick}>
      <span className={tone}>{icon}</span>
      <div>
        <b>{title}</b>
        <small>{detail}</small>
      </div>
      <em>{meta}</em>
      <ChevronRight size={16} />
    </button>
  );
}

function Ledger({
  contracts,
  open,
  create,
}: {
  contracts: Contract[];
  open: (id: number) => void;
  create: () => void;
}) {
  const [query, setQuery] = useState(""),
    [state, setState] = useState("全部"),
    [category, setCategory] = useState("全部合同");
  const filtered = contracts.filter(
    (x) =>
      (state === "全部" || x.status === state) &&
      (category === "全部合同" || x.category === category) &&
      [x.name, x.no, x.party, x.type]
        .join(" ")
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  const columns: DataGridColumn<Contract>[] = [
    {
      id: "name",
      header: "合同名称",
      isRowHeader: true,
      minWidth: 250,
      allowsSorting: true,
      cell: (x) => (
        <span className="contract-cell">
          <b>{x.name}</b>
          <small>
            {x.no} · v{x.version}
          </small>
        </span>
      ),
    },
    {
      id: "party",
      header: "类型 / 交易对方",
      minWidth: 190,
      cell: (x) => (
        <span className="contract-cell">
          <b>{x.type}</b>
          <small>{x.party}</small>
        </span>
      ),
    },
    {
      id: "status",
      header: "业务状态",
      width: 105,
      cell: (x) => <Tag tone={statusTone(x.status)}>{x.status}</Tag>,
    },
    {
      id: "approval",
      header: "审批状态",
      width: 105,
      cell: (x) => <Tag tone={approvalTone(x.approval)}>{x.approval}</Tag>,
    },
    {
      id: "risk",
      header: "AI 风险",
      width: 90,
      cell: (x) => <Tag tone={riskTone(x.risk)}>{x.risk}</Tag>,
    },
    {
      id: "amount",
      header: "合同金额",
      align: "end",
      width: 130,
      allowsSorting: true,
      cell: (x) => <span className="money">{money(x.amount)}</span>,
    },
    {
      id: "updated",
      header: "最近更新",
      width: 115,
      cell: (x) => <span className="muted">{x.updated}</span>,
    },
  ];
  return (
    <>
      <Header
        index="02"
        title="合同台账"
        desc="所有合同从草稿开始进入同一条版本、审查、审批和履约链。"
        action={
          <Button onPress={create}>
            <Plus size={16} />
            新建合同
          </Button>
        }
      />
      <div className="category-tabs">
        {["全部合同", "采购", "销售", "人力", "服务", "行政"].map((x) => (
          <button
            className={category === x ? "active" : ""}
            onClick={() => setCategory(x)}
            key={x}
          >
            {x}
          </button>
        ))}
      </div>
      <section className="panel ledger">
        <div className="toolbar">
          <label>
            <Search size={17} />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="搜索合同名称、编号或交易对方"
            />
            {query && (
              <button onClick={() => setQuery("")}>
                <X size={14} />
              </button>
            )}
          </label>
          <select value={state} onChange={(e) => setState(e.target.value)}>
            {[
              "全部",
              "草稿",
              "待审查",
              "审批中",
              "待签署",
              "履行中",
              "已完成",
              "已终止",
            ].map((x) => (
              <option key={x}>{x}</option>
            ))}
          </select>
          <button className="outline">
            <SlidersHorizontal size={16} />
            组合筛选
          </button>
          <button className="outline">导出</button>
        </div>
        <div className="result-note">
          <b>{filtered.length}</b> 份合同
          <span />
          金额合计 <b>{money(filtered.reduce((s, x) => s + x.amount, 0))}</b>
        </div>
        <DataGrid
          aria-label="合同台账"
          columns={columns}
          data={filtered}
          getRowId={(x) => x.id}
          onRowAction={(key) => open(Number(key))}
          allowsColumnResize
          renderEmptyState={() => (
            <div className="empty">没有符合条件的合同</div>
          )}
          contentClassName="min-w-[1080px]"
          variant="primary"
        />
      </section>
    </>
  );
}

function MiniTable({
  contracts,
  open,
}: {
  contracts: Contract[];
  open: (id: number) => void;
}) {
  return (
    <div className="mini-table">
      <div>
        <span>合同</span>
        <span>状态</span>
        <span>风险</span>
        <span>金额</span>
        <span>更新</span>
      </div>
      {contracts.map((x) => (
        <button key={x.id} onClick={() => open(x.id)}>
          <span>
            <b>{x.name}</b>
            <small>{x.no}</small>
          </span>
          <Tag tone={statusTone(x.status)}>{x.status}</Tag>
          <Tag tone={riskTone(x.risk)}>{x.risk}</Tag>
          <span className="money">{money(x.amount)}</span>
          <span className="muted">{x.updated}</span>
        </button>
      ))}
    </div>
  );
}

function CreateContract({
  source,
  setSource,
  cancel,
  complete,
}: {
  source: "signed" | "review" | "draft" | null;
  setSource: (v: "signed" | "review" | "draft" | null) => void;
  cancel: () => void;
  complete: (c: Contract) => void;
}) {
  const [step, setStep] = useState(0),
    [file, setFile] = useState(""),
    [name, setName] = useState(""),
    [party, setParty] = useState(""),
    [type, setType] = useState("采购合同");
  if (!source)
    return (
      <>
        <Header
          index="03"
          title="新建合同"
          desc="先选择合同目前所处的真实阶段。"
          action={
            <button className="back" onClick={cancel}>
              <ArrowLeft size={16} />
              返回台账
            </button>
          }
        />
        <div className="source-grid">
          <Source
            no="A"
            icon={<FileCheck2 />}
            title="登记已签合同"
            desc="上传已签文件，完成提取与核验后进入履约台账。"
            flow="上传 → 提取确认 → 法律核验 → 登记"
            onClick={() => {
              setSource("signed");
              setStep(0);
            }}
          />
          <Source
            no="B"
            icon={<FileSearch />}
            title="上传待审合同"
            desc="对方提供未签文件，审查并审批通过后进入签署。"
            flow="上传 → 提取确认 → 法律审查 → 审批"
            onClick={() => {
              setSource("review");
              setStep(0);
            }}
          />
          <Source
            no="C"
            icon={<FilePenLine />}
            title="从模板起草"
            desc="选择标准模板或参考模板，编辑形成合同草稿。"
            flow="选模板 → 填写信息 → 编辑正文 → 保存"
            onClick={() => {
              setSource("draft");
              setStep(0);
            }}
          />
        </div>
      </>
    );
  const steps =
    source === "signed"
      ? ["上传已签文件", "确认提取信息", "法律核验", "完成登记"]
      : source === "review"
        ? ["上传待审文件", "确认提取信息", "法律审查", "建立审批"]
        : ["选择合同模板", "填写合同信息", "编辑合同正文", "保存草稿"];
  const finish = () =>
    complete({
      id: Date.now(),
      no: `DEMO-${Date.now().toString().slice(-6)}`,
      name:
        name || `演示：${source === "draft" ? "新建合同草稿" : "新上传合同"}`,
      type,
      category: type.slice(0, 2),
      party: party || "待补充交易对方",
      owner: "当前部门",
      status:
        source === "signed"
          ? "履行中"
          : source === "review"
            ? "待审查"
            : "草稿",
      approval: "未提交",
      risk: source === "draft" ? "未审查" : "低",
      amount: 0,
      version: 1,
      updated: "刚刚",
      expire: "—",
    });
  return (
    <>
      <Header
        index="03"
        title={
          source === "signed"
            ? "登记已签合同"
            : source === "review"
              ? "上传待审合同"
              : "从模板起草"
        }
        desc="每一步保存到同一份合同记录，后续从合同详情继续处理。"
        action={
          <button className="back" onClick={() => setSource(null)}>
            <ArrowLeft size={16} />
            重新选择
          </button>
        }
      />
      <section className="create-layout">
        <aside className="flow-aside">
          <Stepper
            currentStep={step}
            orientation="vertical"
            onStepChange={setStep}
          >
            {steps.map((s) => (
              <Stepper.Step key={s}>
                <Stepper.Indicator />
                <Stepper.Content>
                  <Stepper.Title>{s}</Stepper.Title>
                </Stepper.Content>
                <Stepper.Separator />
              </Stepper.Step>
            ))}
          </Stepper>
          <div className="flow-rule">
            <ShieldCheck size={16} />
            <p>AI 只负责提取、检测和建议，不能审批或改变合同状态。</p>
          </div>
        </aside>
        <article className="panel form-stage">
          <div className="form-title">
            <span>
              步骤 {step + 1} / {steps.length}
            </span>
            <h2>{steps[step]}</h2>
          </div>
          {step === 0 && source !== "draft" && (
            <label className="upload">
              <input
                type="file"
                onChange={(e) => setFile(e.target.files?.[0]?.name || "")}
              />
              <Upload size={28} />
              <b>{file || "上传合同文件"}</b>
              <small>支持 Word、PDF 或扫描件；文件将绑定内容版本</small>
              <em>选择文件</em>
            </label>
          )}
          {step === 0 && source === "draft" && (
            <div className="template-pick">
              <button className="selected">
                <FileText />
                <span>
                  <b>通用服务合同模板</b>
                  <small>已发布 · v4</small>
                </span>
                <Check />
              </button>
              <button>
                <Files />
                <span>
                  <b>选择多个参考模板</b>
                  <small>仅作为 AI 起草参考</small>
                </span>
                <Plus />
              </button>
            </div>
          )}
          {step === 1 && (
            <div className="fields">
              <label>
                合同名称
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="请输入合同名称"
                />
              </label>
              <label>
                合同类型
                <select value={type} onChange={(e) => setType(e.target.value)}>
                  {[
                    "采购合同",
                    "销售合同",
                    "服务合同",
                    "劳动合同",
                    "租赁合同",
                  ].map((x) => (
                    <option key={x}>{x}</option>
                  ))}
                </select>
              </label>
              <label>
                交易对方
                <input
                  value={party}
                  onChange={(e) => setParty(e.target.value)}
                  placeholder="搜索外部主体"
                />
              </label>
              <label>
                合同金额
                <input placeholder="0.00" />
              </label>
              <label>
                签约主体
                <select>
                  <option>演示签约主体</option>
                </select>
              </label>
              <label>
                经办部门
                <input value="当前部门" readOnly />
              </label>
            </div>
          )}
          {step === 2 && (
            <div className="editor">
              <div className="editor-tools">
                <button>正文</button>
                <button>引用模板</button>
                <button>插入变量</button>
                <button>
                  <Sparkles size={15} />
                  AI 建议
                </button>
              </div>
              <textarea
                defaultValue={
                  source === "draft"
                    ? "第一条 合同标的\n\n第二条 双方权利与义务\n\n第三条 交付与验收\n\n第四条 违约责任"
                    : "AI 已完成字段提取，请核对合同名称、交易对方、金额和期限。"
                }
              />
              <div className="evidence-note">
                <FileSearch />
                <span>
                  <b>
                    {source === "draft"
                      ? "正文保存后生成内容版本 v1"
                      : "已识别 12 个字段"}
                  </b>
                  <small>所有结果都需要人工确认</small>
                </span>
              </div>
            </div>
          )}
          {step === 3 && (
            <div className="final-check">
              <span>
                <Check />
              </span>
              <h2>
                {source === "signed"
                  ? "准备完成登记"
                  : source === "review"
                    ? "准备进入法律审查"
                    : "准备保存合同草稿"}
              </h2>
              <p>
                保存后可从合同详情查看当前版本、AI 结果、审批和后续办理入口。
              </p>
              <dl>
                <div>
                  <dt>合同名称</dt>
                  <dd>{name || "待补充"}</dd>
                </div>
                <div>
                  <dt>交易对方</dt>
                  <dd>{party || "待补充"}</dd>
                </div>
                <div>
                  <dt>合同类型</dt>
                  <dd>{type}</dd>
                </div>
              </dl>
            </div>
          )}
          <footer className="form-actions">
            <button
              className="outline"
              disabled={step === 0}
              onClick={() => setStep(Math.max(0, step - 1))}
            >
              上一步
            </button>
            <Button
              onPress={() =>
                step < steps.length - 1 ? setStep(step + 1) : finish()
              }
            >
              {step < steps.length - 1 ? "下一步" : "保存并查看"}
              <ChevronRight size={16} />
            </Button>
          </footer>
        </article>
      </section>
    </>
  );
}

function Source({
  no,
  icon,
  title,
  desc,
  flow,
  onClick,
}: {
  no: string;
  icon: React.ReactNode;
  title: string;
  desc: string;
  flow: string;
  onClick: () => void;
}) {
  return (
    <button className="source" onClick={onClick}>
      <span className="source-no">{no}</span>
      <span className="source-icon">{icon}</span>
      <h2>{title}</h2>
      <p>{desc}</p>
      <small>{flow}</small>
      <b>
        开始办理
        <ChevronRight size={16} />
      </b>
    </button>
  );
}

function Detail({
  contract,
  back,
  review,
  flash,
}: {
  contract: Contract;
  back: () => void;
  review: () => void;
  flash: (m: string) => void;
}) {
  const [tab, setTab] = useState("概览");
  const tabs = [
    "概览",
    "正文与版本",
    "条款与履行",
    "收付款",
    "智能与审批",
    "附件与变更",
    "操作记录",
  ];
  return (
    <>
      <button className="back" onClick={back}>
        <ArrowLeft size={16} />
        返回合同台账
      </button>
      <header className="identity">
        <div className="identity-no">{contract.no}</div>
        <div>
          <span className="tags">
            <Tag tone={statusTone(contract.status)}>{contract.status}</Tag>
            <Tag tone={approvalTone(contract.approval)}>
              {contract.approval}
            </Tag>
            <Tag tone={riskTone(contract.risk)}>AI 风险 {contract.risk}</Tag>
          </span>
          <h1>{contract.name}</h1>
          <p>
            {contract.type} · {contract.party} · 内容版本 v{contract.version}
          </p>
        </div>
        <div>
          <button className="outline" onClick={review}>
            <ShieldCheck size={16} />
            查看审查
          </button>
          <Button onPress={() => flash("已进入下一办理节点")}>继续办理</Button>
        </div>
      </header>
      <div className="detail-tabs">
        {tabs.map((x) => (
          <button
            key={x}
            className={tab === x ? "active" : ""}
            onClick={() => setTab(x)}
          >
            {x}
          </button>
        ))}
      </div>
      <DetailTab tab={tab} contract={contract} review={review} flash={flash} />
    </>
  );
}
function DetailTab({
  tab,
  contract,
  review,
  flash,
}: {
  tab: string;
  contract: Contract;
  review: () => void;
  flash: (m: string) => void;
}) {
  if (tab === "概览")
    return (
      <div className="detail-grid">
        <section className="panel next">
          <SectionTitle no="A" title="下一步" />
          <div>
            <AlertTriangle />
            <span>
              <b>确认高风险审查意见</b>
              <p>违约责任条款缺少责任上限，需要法务处理后再提交审批。</p>
            </span>
          </div>
          <Button onPress={review}>进入 AI 审查</Button>
        </section>
        <section className="panel facts">
          <SectionTitle no="B" title="合同信息" />
          <dl>
            <div>
              <dt>交易对方</dt>
              <dd>{contract.party}</dd>
            </div>
            <div>
              <dt>合同金额</dt>
              <dd>{money(contract.amount)}</dd>
            </div>
            <div>
              <dt>经办部门</dt>
              <dd>{contract.owner}</dd>
            </div>
            <div>
              <dt>到期日期</dt>
              <dd>{contract.expire}</dd>
            </div>
          </dl>
        </section>
        <section className="panel record">
          <SectionTitle no="C" title="最近记录" />
          <RecordRows />
        </section>
      </div>
    );
  if (tab === "正文与版本")
    return (
      <div className="split">
        <section className="panel document">
          <SectionTitle no="A" title="当前正文" action="下载当前版本" />
          <div className="paper">
            <h2>{contract.name}</h2>
            <p>
              本区域展示当前内容版本。合同正文、附件、AI
              提取和审查结果均绑定版本，不覆盖历史记录。
            </p>
            <h3>第一条 合同标的</h3>
            <p>双方依据合同约定履行各自权利与义务。</p>
          </div>
        </section>
        <section className="panel versions">
          <SectionTitle no="B" title="版本历史" />
          {[contract.version, Math.max(1, contract.version - 1), 1]
            .filter((x, i, a) => a.indexOf(x) === i)
            .map((v, i) => (
              <button key={v}>
                <FileText />
                <span>
                  <b>内容版本 v{v}</b>
                  <small>{i === 0 ? "当前版本" : "历史版本"}</small>
                </span>
                {i === 0 ? <Tag tone="blue">当前</Tag> : <ChevronRight />}
              </button>
            ))}
        </section>
      </div>
    );
  if (tab === "智能与审批")
    return (
      <section className="panel process">
        <SectionTitle no="A" title="合同办理链" />
        <div className="process-line">
          {[
            ["内容版本", "完成"],
            ["AI 提取", "待确认"],
            ["法律审查", "处理中"],
            ["外部审批", "未提交"],
            ["签署生效", "未开始"],
            ["履约归档", "未开始"],
          ].map(([a, b], i) => (
            <div key={a} className={i < 3 ? "active" : ""}>
              <span>{i < 1 ? <Check /> : i + 1}</span>
              <b>{a}</b>
              <small>{b}</small>
            </div>
          ))}
        </div>
        <div className="process-actions">
          <button className="outline" onClick={review}>
            查看 AI 证据
          </button>
          <Button onPress={() => flash("已生成审批提交检查清单")}>
            检查并提交审批
          </Button>
        </div>
      </section>
    );
  return (
    <section className="panel empty-panel">
      <SectionTitle no="A" title={tab} />
      <div className="empty">
        <FileClock />
        <b>暂无演示记录</b>
        <p>真实业务操作产生后，将按权限和版本展示在这里。</p>
      </div>
    </section>
  );
}

const reviewRisks = [
  {
    level: "高",
    title: "违约责任上限缺失",
    clause: "第十二条 违约责任",
    text: "任何一方违反本合同约定，应赔偿对方因此遭受的全部损失。",
    advice: "建议明确直接损失范围，并约定累计赔偿责任上限。",
    rule: "风险规则 R-12 · v3.2",
  },
  {
    level: "中",
    title: "验收期限表述不明确",
    clause: "第六条 验收",
    text: "甲方应在货物到达后及时完成验收。",
    advice: "将“及时”调整为明确工作日，并约定逾期反馈方式。",
    rule: "期限规则 R-07 · v2.1",
  },
  {
    level: "低",
    title: "通知方式可以补充",
    clause: "第十五条 通知",
    text: "双方通知应以书面形式送达。",
    advice: "补充有效电子邮箱和送达时间认定。",
    rule: "完整性规则 R-21 · v1.4",
  },
];
function Review({
  contract,
  back,
  flash,
}: {
  contract: Contract;
  back: () => void;
  flash: (m: string) => void;
}) {
  const [active, setActive] = useState(0),
    risk = reviewRisks[active];
  return (
    <>
      <Header
        index="04"
        title="AI 审查"
        desc={`${contract.no} · 内容版本 v${contract.version} · AI 结果只提供辅助判断`}
        action={
          <>
            <button className="outline" onClick={back}>
              <ArrowLeft size={16} />
              返回详情
            </button>
            <Button onPress={() => flash("审查结果已由人工确认")}>
              <Check size={16} />
              确认审查结果
            </Button>
          </>
        }
      />
      <section className="review-shell">
        <Resizable orientation="horizontal">
          <Resizable.Panel defaultSize="55%" minSize={38}>
            <div className="review-doc">
              <div className="doc-bar">
                <span>
                  <FileText size={16} />
                  {contract.name}
                </span>
                <span>第 4 / 12 页</span>
              </div>
              <div className="paper">
                <h2>采购合同</h2>
                <h3>第六条 验收</h3>
                <p className={active === 1 ? "mark" : ""}>
                  甲方应在货物到达后及时完成验收。
                </p>
                <h3>第十二条 违约责任</h3>
                <p className={active === 0 ? "mark danger" : ""}>
                  任何一方违反本合同约定，应赔偿对方因此遭受的全部损失。
                </p>
                <h3>第十五条 通知</h3>
                <p className={active === 2 ? "mark" : ""}>
                  双方通知应以书面形式送达。
                </p>
              </div>
            </div>
          </Resizable.Panel>
          <Resizable.Handle />
          <Resizable.Panel defaultSize="45%" minSize={30}>
            <div className="review-side">
              <div className="review-list">
                <SectionTitle no="A" title="审查发现" />
                {reviewRisks.map((r, i) => (
                  <button
                    key={r.title}
                    className={active === i ? "active" : ""}
                    onClick={() => setActive(i)}
                  >
                    <Tag tone={riskTone(r.level as RiskLevel)}>
                      {r.level}风险
                    </Tag>
                    <span>
                      <b>{r.title}</b>
                      <small>{r.clause}</small>
                    </span>
                    <ChevronRight />
                  </button>
                ))}
              </div>
              <div className="evidence">
                <span>证据定位</span>
                <h2>{risk.title}</h2>
                <dl>
                  <div>
                    <dt>原文</dt>
                    <dd>{risk.text}</dd>
                  </div>
                  <div>
                    <dt>判断依据</dt>
                    <dd>{risk.rule}</dd>
                  </div>
                  <div>
                    <dt>处理建议</dt>
                    <dd>{risk.advice}</dd>
                  </div>
                  <div>
                    <dt>版本绑定</dt>
                    <dd>
                      {contract.no} · v{contract.version}
                    </dd>
                  </div>
                </dl>
              </div>
            </div>
          </Resizable.Panel>
        </Resizable>
      </section>
    </>
  );
}

function ApprovalCenter({
  contracts,
  open,
  flash,
}: {
  contracts: Contract[];
  open: (id: number) => void;
  flash: (m: string) => void;
}) {
  const list = contracts.filter((x) => x.approval !== "未提交");
  return (
    <>
      <Header
        index="05"
        title="审批中心"
        desc="跟踪外部审批状态、待办人和回调记录；业务状态与审批状态分开显示。"
      />
      <div className="approval-tabs">
        <button className="active">待我处理 1</button>
        <button>我发起的 2</button>
        <button>全部审批 {list.length}</button>
      </div>
      <section className="panel approval-list">
        {list.map((x, i) => (
          <article key={x.id}>
            <span className="approval-index">
              {String(i + 1).padStart(2, "0")}
            </span>
            <div>
              <b>{x.name}</b>
              <small>
                {x.no} · {x.owner} · {x.party}
              </small>
            </div>
            <Tag tone={approvalTone(x.approval)}>{x.approval}</Tag>
            <span className="approval-owner">
              <small>当前节点</small>
              <b>{x.approval === "审批中" ? "法务负责人" : "流程结束"}</b>
            </span>
            <button className="outline" onClick={() => open(x.id)}>
              查看合同
            </button>
            {x.approval === "审批中" && (
              <Button onPress={() => flash("审批意见已记录，仅作前端演示")}>
                处理
              </Button>
            )}
          </article>
        ))}
      </section>
    </>
  );
}

function Fulfillment({
  contracts,
  open,
  flash,
}: {
  contracts: Contract[];
  open: (id: number) => void;
  flash: (m: string) => void;
}) {
  const list = contracts.filter((x) => x.status === "履行中");
  return (
    <>
      <Header
        index="06"
        title="履约与收付款"
        desc="跟踪交付、验收、付款计划和异常，所有凭证回到合同详情。"
      />
      <div className="fulfill-top">
        <KPI>
          <KPI.Header>
            <KPI.Title>计划金额</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value
              value={936000}
              currency="CNY"
              style="currency"
              maximumFractionDigits={0}
            />
          </KPI.Content>
        </KPI>
        <KPI>
          <KPI.Header>
            <KPI.Title>已执行金额</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value
              value={580320}
              currency="CNY"
              style="currency"
              maximumFractionDigits={0}
            />
          </KPI.Content>
          <KPI.Footer>执行进度 62%</KPI.Footer>
        </KPI>
        <KPI>
          <KPI.Header>
            <KPI.Title>逾期事项</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value value={1} />
          </KPI.Content>
          <KPI.Footer>1 项需要处理</KPI.Footer>
        </KPI>
      </div>
      <div className="fulfill-grid">
        <section className="panel">
          <SectionTitle no="A" title="临近节点" />
          {[
            ["14", "首期付款", "¥216,000", "3 天后"],
            ["20", "设备到货验收", "采购合同", "9 天后"],
            ["30", "月度服务验收", "服务合同", "19 天后"],
          ].map(([d, n, m, t]) => (
            <div className="milestone" key={n}>
              <span>
                <b>{d}</b>
                <small>SEP</small>
              </span>
              <div>
                <b>{n}</b>
                <small>{m}</small>
              </div>
              <Tag tone={t === "3 天后" ? "red" : "gray"}>{t}</Tag>
              <button onClick={() => flash("履约事项已打开")}>
                <ChevronRight />
              </button>
            </div>
          ))}
        </section>
        <section className="panel">
          <SectionTitle no="B" title="执行中合同" />
          {list.map((x) => (
            <button
              className="fulfill-contract"
              key={x.id}
              onClick={() => open(x.id)}
            >
              <span>
                <b>{x.name}</b>
                <small>{x.no}</small>
              </span>
              <strong>62%</strong>
              <i>
                <em style={{ width: "62%" }} />
              </i>
            </button>
          ))}
        </section>
      </div>
    </>
  );
}

const trend = [
  { month: "4月", count: 8, amount: 66 },
  { month: "5月", count: 11, amount: 82 },
  { month: "6月", count: 9, amount: 71 },
  { month: "7月", count: 14, amount: 116 },
  { month: "8月", count: 12, amount: 98 },
  { month: "9月", count: 16, amount: 132 },
];
function Cockpit({ contracts }: { contracts: Contract[] }) {
  return (
    <>
      <Header
        index="07"
        title="经营数据大屏"
        desc="只读汇总当前数据权限范围内的合同规模、生命周期、风险和履约情况。"
        action={
          <button className="outline">
            <BarChart3 size={16} />
            导出分析
          </button>
        }
      />
      <div className="cockpit-kpis">
        <KPI>
          <KPI.Header>
            <KPI.Title>合同总额</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value
              value={2002000}
              currency="CNY"
              style="currency"
              maximumFractionDigits={0}
            />
          </KPI.Content>
          <KPI.Footer>演示数据口径</KPI.Footer>
        </KPI>
        <KPI>
          <KPI.Header>
            <KPI.Title>有效合同</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value
              value={
                contracts.filter(
                  (x) => !["已完成", "已终止"].includes(x.status),
                ).length
              }
            />
          </KPI.Content>
        </KPI>
        <KPI>
          <KPI.Header>
            <KPI.Title>高风险待处理</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value value={1} />
          </KPI.Content>
          <KPI.Footer>需要法务确认</KPI.Footer>
        </KPI>
        <KPI>
          <KPI.Header>
            <KPI.Title>履约执行率</KPI.Title>
          </KPI.Header>
          <KPI.Content>
            <KPI.Value value={0.62} style="percent" maximumFractionDigits={0} />
          </KPI.Content>
          <KPI.Progress value={62} />
        </KPI>
      </div>
      <div className="cockpit-grid">
        <section className="panel trend-chart">
          <SectionTitle no="A" title="近六个月合同趋势" />
          <AreaChart data={trend} height={280}>
            <defs>
              <linearGradient id="blue-fill" x1="0" x2="0" y1="0" y2="1">
                <stop offset="0%" stopColor="#002FA7" stopOpacity={0.18} />
                <stop offset="100%" stopColor="#002FA7" stopOpacity={0.01} />
              </linearGradient>
            </defs>
            <AreaChart.Grid vertical={false} />
            <AreaChart.XAxis dataKey="month" />
            <AreaChart.YAxis />
            <AreaChart.Area
              dataKey="amount"
              name="合同金额（万元）"
              stroke="#002FA7"
              fill="url(#blue-fill)"
              strokeWidth={2}
            />
            <AreaChart.Tooltip content={<AreaChart.TooltipContent />} />
          </AreaChart>
        </section>
        <section className="panel distribution">
          <SectionTitle no="B" title="生命周期分布" />
          {[
            ["草稿", 1, 17],
            ["审查与审批", 2, 33],
            ["待签署", 1, 17],
            ["履行中", 1, 17],
            ["已完成", 1, 17],
          ].map(([n, c, p]) => (
            <div key={n as string}>
              <span>
                <b>{n}</b>
                <em>{c}</em>
              </span>
              <i>
                <b style={{ width: `${p}%` }} />
              </i>
            </div>
          ))}
        </section>
        <section className="panel risk-board">
          <SectionTitle no="C" title="风险分布" />
          <div className="risk-numbers">
            <span>
              <b>1</b>
              <small>高风险</small>
            </span>
            <span>
              <b>1</b>
              <small>中风险</small>
            </span>
            <span>
              <b>3</b>
              <small>低风险</small>
            </span>
            <span>
              <b>1</b>
              <small>未审查</small>
            </span>
          </div>
        </section>
        <section className="panel category-board">
          <SectionTitle no="D" title="合同类型金额" />
          {[
            ["销售合同", 72],
            ["服务合同", 48],
            ["租赁合同", 36],
            ["采购合同", 28.6],
          ].map(([n, v]) => (
            <div key={n as string}>
              <span>{n}</span>
              <i>
                <b style={{ width: `${Number(v)}%` }} />
              </i>
              <strong>{v} 万</strong>
            </div>
          ))}
        </section>
      </div>
    </>
  );
}

function Templates({ start }: { start: () => void }) {
  const rows = [
    ["通用服务合同模板", "服务合同", "v4", "已发布"],
    ["标准采购合同模板", "采购合同", "v7", "已发布"],
    ["产品销售合同模板", "销售合同", "v3", "已发布"],
    ["劳动合同标准模板", "劳动合同", "v6", "已发布"],
  ];
  return (
    <>
      <Header
        index="08"
        title="合同模板"
        desc="模板与合同分类、字段字典和版本关联；模板更新不覆盖已有合同。"
        action={
          <Button onPress={start}>
            <Plus size={16} />
            使用模板起草
          </Button>
        }
      />
      <ResourceToolbar placeholder="搜索模板名称、类型或标签" />
      <section className="resource-table">
        <div>
          <span>模板名称</span>
          <span>适用类型</span>
          <span>版本</span>
          <span>状态</span>
          <span>最近更新</span>
          <span />
        </div>
        {rows.map((x, i) => (
          <button key={x[0]}>
            <span>
              <FileText />
              <b>{x[0]}</b>
            </span>
            <span>{x[1]}</span>
            <span>{x[2]}</span>
            <Tag tone="green">{x[3]}</Tag>
            <span>2026-09-0{i + 1}</span>
            <ChevronRight />
          </button>
        ))}
      </section>
    </>
  );
}
function Parties() {
  const rows = [
    ["演示供应商 A", "供应商", "企业主体系统", "可签约"],
    ["演示服务商 B", "服务商", "企业主体系统", "可签约"],
    ["演示客户 C", "客户", "客户主数据", "可签约"],
    ["演示员工", "员工", "HR 系统", "可签约"],
  ];
  return (
    <>
      <Header
        index="09"
        title="交易对方"
        desc="查询外部主体当前信息；合同关键时点保存不可覆盖的签约快照。"
      />
      <ResourceToolbar placeholder="搜索交易对方名称或外部 ID" />
      <section className="resource-table">
        <div>
          <span>交易对方</span>
          <span>主体类型</span>
          <span>来源系统</span>
          <span>签约状态</span>
          <span>同步状态</span>
          <span />
        </div>
        {rows.map((x) => (
          <button key={x[0]}>
            <span>
              <Building2 />
              <b>{x[0]}</b>
            </span>
            <span>{x[1]}</span>
            <span>{x[2]}</span>
            <Tag tone="green">{x[3]}</Tag>
            <span>已同步</span>
            <ChevronRight />
          </button>
        ))}
      </section>
    </>
  );
}
function Knowledge() {
  return (
    <LibraryPage
      index="10"
      title="合同知识库"
      desc="维护法律依据、审查指引和标准条款，为 AI 解释提供可追溯来源。"
      items={[
        ["采购合同审查指引", "采购 / 法律审查", "已发布"],
        ["违约责任标准条款", "通用 / 条款库", "已发布"],
        ["劳动合同必备条款清单", "人力 / 合规", "草稿"],
      ]}
    />
  );
}
function Rules() {
  return (
    <LibraryPage
      index="11"
      title="合同规则库"
      desc="定义命中条件、风险等级和处置建议；规则发布需要授权审核。"
      items={[
        ["违约责任上限检查", "高风险 / 阻断", "已启用"],
        ["验收期限明确性检查", "中风险 / 提醒", "已启用"],
        ["通知方式完整性检查", "低风险 / 建议", "已启用"],
      ]}
    />
  );
}
function LibraryPage({
  index,
  title,
  desc,
  items,
}: {
  index: string;
  title: string;
  desc: string;
  items: string[][];
}) {
  return (
    <>
      <Header
        index={index}
        title={title}
        desc={desc}
        action={
          <Button>
            <Plus size={16} />
            新建
          </Button>
        }
      />
      <ResourceToolbar placeholder={`搜索${title}`} />
      <section className="library-grid">
        {items.map((x, i) => (
          <article className="panel" key={x[0]}>
            <span className="lib-index">{String(i + 1).padStart(2, "0")}</span>
            <FileText />
            <h2>{x[0]}</h2>
            <p>{x[1]}</p>
            <Tag tone={x[2].includes("草稿") ? "gray" : "green"}>{x[2]}</Tag>
            <button>
              查看详情
              <ChevronRight />
            </button>
          </article>
        ))}
      </section>
    </>
  );
}
function ResourceToolbar({ placeholder }: { placeholder: string }) {
  return (
    <div className="resource-toolbar">
      <label>
        <Search size={17} />
        <input placeholder={placeholder} />
      </label>
      <button className="outline">
        <SlidersHorizontal size={16} />
        筛选
      </button>
      <button className="outline">导入</button>
    </div>
  );
}
function SettingsPage() {
  const items = [
    [Bot, "AI 能力配置", "解析、法律审查与起草 Agent"],
    [Database, "外部连接", "主体、文件与审批服务"],
    [ShieldCheck, "权限与数据范围", "角色、组织、合同和字段"],
    [Users, "账号管理", "账号、角色和默认范围"],
    [FolderCog, "合同基础配置", "分类、编号和履约事项"],
    [ClipboardCheck, "操作日志", "只读审计和访问记录"],
  ];
  return (
    <>
      <Header
        index="12"
        title="系统配置"
        desc="配置按职责分区，密钥、服务地址和敏感信息不在前端暴露。"
      />
      <section className="settings-list">
        {items.map(([Icon, title, desc], i) => (
          <button className="panel" key={title as string}>
            <span>{String(i + 1).padStart(2, "0")}</span>
            <Icon />
            <div>
              <h2>{title as string}</h2>
              <p>{desc as string}</p>
            </div>
            <Tag tone={i < 2 ? "blue" : "gray"}>
              {i < 2 ? "已配置" : "进入管理"}
            </Tag>
            <ChevronRight />
          </button>
        ))}
      </section>
    </>
  );
}

function RecordRows() {
  return (
    <div className="records">
      <div>
        <span>
          <Check />
        </span>
        <p>
          <b>AI 提取完成</b>
          <small>12 个字段等待人工确认 · 今天 10:26</small>
        </p>
      </div>
      <div>
        <span>
          <FileText />
        </span>
        <p>
          <b>生成内容版本 v3</b>
          <small>演示经办人上传 · 今天 10:24</small>
        </p>
      </div>
      <div>
        <span>
          <UserRound />
        </span>
        <p>
          <b>创建合同草稿</b>
          <small>前端演示记录 · 昨天 16:08</small>
        </p>
      </div>
    </div>
  );
}
function statusTone(s: ContractStatus) {
  return {
    草稿: "gray",
    待审查: "blue",
    审批中: "orange",
    待签署: "blue",
    履行中: "green",
    已完成: "gray",
    已终止: "red",
  }[s];
}
function approvalTone(s: ApprovalStatus) {
  return { 未提交: "gray", 审批中: "orange", 已通过: "green", 已驳回: "red" }[
    s
  ];
}
function riskTone(s: RiskLevel) {
  return { 高: "red", 中: "orange", 低: "green", 未审查: "gray" }[s];
}
