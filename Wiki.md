# Wiki.md — 项目常识库（长期稳定信息）

> 定义：项目背景、业务口径、命名规范、接口说明等长期不变的信息。

## 1. 项目背景
- **通用合同管理系统**：统一管理企业内部和对外的各类合同，隶属 HR 产品线。
- 核心卖点：AI 合同解析 + AI 法律风险审查（调 AI 中台「法律专家」）。
- **员工合同纳入本系统**：员工是合同中的交易对方关系之一，员工主数据仍由 HR/组织系统维护。
- **交易对方 ≠ 客户管理（CRM）**：本项目只引用合同交易对方，不建设客户或供应商主数据模块；客户、供应商、合作方等由外部交易对方主数据系统维护。

### 1.1 “合同中心”与“合同中心区”

- **合同中心**：合同系统的一级业务导航，承载工作台、合同台账、履行与收付款，以及从合同详情进入的创建、AI、审批、签署、变更和附件操作。
- **合同中心区**：公司统一文件服务内为合同系统分配的专属业务存储空间，保存合同原始文件、正文文件、签署文件、扫描件、附件、履行佐证和导出文件。
- 两者不是同一个概念：合同中心是页面和业务能力，合同中心区是文件服务中的存储隔离空间；合同中心区不是合同系统菜单，也不是本系统自建文件服务器。
- 合同系统保存合同、附件关联、文件版本、权限和审计；文件服务负责实际文件存储、预览、下载和短期访问链接。业务数据库不保存文件二进制、物理绝对路径或长期公开 URL。
- 文件流程统一为：申请上传 → 文件服务上传 → 上传完成确认并返回 `file_id`、哈希和大小 → 合同系统创建附件关联；预览和下载均先经过合同权限校验，再申请短期访问链接。
- 商用上线前还需确认文件服务的租户/业务空间标识、对象命名规则、保留与归档策略、病毒扫描、大小和格式限制、备份恢复、跨环境隔离及接口幂等规则。

## 2. 命名规范（长期有效）
- 遵循主系统标准《人力资源管理系统数据库设计文档4》：主键 `BIGINT` 雪花非自增、业务主表逻辑删除 `deleted`、表名按模块前缀和业务名使用 `snake_case`。
- 全库不使用物理外键，表间一致性由应用层保证；具体合同模块前缀需按主系统标准进一步确认。
- 枚举 `TINYINT`；索引 `uk_`（唯一）/`idx_`（普通）；字段后缀 `_id/_time/_date/_type/_status/_num/_url/_no/_desc`，布尔 `is_`。
- ⚠️ 公司另有「培训模块标准」（id 自增 + is_deleted），**本项目不用**。

## 3. 合同状态机（重构草案）
```
草稿 → 待签署 → 履行中 → 已完成
  |       |
  +------> 作废
履行中 → 已终止
```
- 审批不占合同业务状态节点，由外部审批关联和 `approval_status` 单独表示。
- `approval_status=审批中` 时锁定合同关键字段；列表按审批状态筛选。
- AI 解析与 AI 审查也不占合同业务状态节点。

## 4. 删除策略（定稿）
- 业务数据（合同/主体/模板）：**软删除** `deleted` + 保留期（到期归档/清理）。
- 日志数据：保留期 + 定期物理清理。
- 有合同引用的主体**不许删**。

## 5. 数据库设计事实来源

- 数据库表、字段、索引、约束和表数量唯一以用户持续维护的 `D:\project\hr-contract\数据库表\合同管理.docx` 当前版本为准。
- 本 Wiki 不复制表清单和历史数量，避免与 DOCX 后续版本不一致。
- `数据库设计增量方案.md`、`归档\数据库表设计.md` 和其他历史 Markdown 不作为开发依据；如与 DOCX 冲突，以 DOCX 为准。
- 公共系统表是否复用、合同系统实际表关系和字段映射，开发前以 DOCX 当前内容及公司主系统接口为准。

## 6. 关键决策记录（长期有效）
| 决策 | 结论 |
|---|---|
| 收付方向 | 加 `payment_direction`（1收/2付），服务合同方向不定需显式指定 |
| 审批 | 外部审批系统负责流程和节点，合同系统通过适配层关联；开发阶段使用 Mock |
| 交易对方数据 | 外部主体引用 + 合同关系角色 + 关键时点快照，不复制客户/供应商主档案 |
| 合同类型 | 重构为合同大类 + 子类型/业务场景，补充协议和变更协议不作为合同类型 |
| 交易对方统计 | 累计份数/总金额不含已作废 |
| PDF 导出 | 详情实时引用，导出时取当前值固化快照 |
| 签署快照 | 一期 PDF 导出固化（文件层）；二期数据库快照固化（签约时刻落库交易对方信息） |
| 原文存储 | origin_file_text 用 MEDIUMTEXT |
| 主体唯一标识 | 企业用 credit_code、个人用 id_number |
| 审批配置 | 一期按合同类型配置审批人（单节点），配置放系统管理 |
| 审批在途 | 配置变更只对新单生效，在途单存提交时快照 |
| 审批记录表 | 主系统无，本项目补 t_contract_approval_record |
| AI 中台接入配置 | 主系统无可复用，本项目新增 t_contract_ai_config |
| AI 中台 | 公司现无，一期可用 Dify 私有化部署搭「法律专家」，合同系统通过 AI Provider 适配层调用 |
| AI 两层 | 外层=合同系统→Dify（配 URL+key）；内层=Dify→大模型（key 在 Dify 后台，自申请）。合同系统只配外层 |
| AI 审查权限 | 独立列四档（发起/编辑意见/仅查看），法律意见归法务编辑 |
| AI 专家体模块 | 本系统核心模块（主文档第7节）；实现调 Dify，功能归本系统，不因外置实现当外挂 |
| 超长合同 | 超上下文窗口（约20万字）切段处理：按条款切→逐段→合并 |
| 作废展示 | 作废合同默认仍展示（状态列标「已作废」、排序靠后），不参与交易对方汇总统计 |
| 待签署撤回 | 回草稿→改后重提；原审批实例作废、生成新实例（新 approval_no），历史审批记录保留 |
| AI 重试 | 系统自动重试 retry_num（默认 3）次；自动重试失败后仍可手动重发，手动不限次 |
| 强制转交 | 系统管理员可强制转交任意审批单（不限业务/审批人），留痕 |
| 补充协议/变更 | 作为原合同的变更关系，不作为独立合同类型；审批关联方式待外部审批契约确认 |
| 业务规则定稿 | 第10章为拍板最终规则，功能表只是罗列；开发同步时以第10章为准 |
| 数据看板 | 二期第5导航，只做合同自身统计（合同/收付款/审批/AI/交易对方）；不做渠道转化率、人均成本、订单/商机/回款率（销售/CRM） |
| AI 导航定位 | AI 专家体不单列导航，贯穿「合同台账 AI 功能 + 系统管理 AI 配置 + Dify 后台」三处 |
| 页面信息架构 | 合同管理=合同管理首页（合同台账视图+收付款计划视图）；合同详情从合同列表或交易对方关联合同进入；审批中心=一个工作台（待办/已办/我发起 Tab）；合同模板归系统管理；字典复用 HR 主系统，不设独立页面 |

### 6.1 通用合同重构方案（当前基线）

- 合同系统统一管理内部和对外合同；员工合同纳入本系统，HR 只维护员工主数据。
- 合同分类采用“合同大类 + 合同类型 + 合同子类型”；交易方向、协议性质作为独立维度。
- 劳动合同、劳务协议、顾问协议分别建模；补充协议和变更协议是原合同的变更关系，不是合同类型。
- 默认单主体部署，但通用底座预留我方主体引用以支持多法人扩展。
- 外部审批系统负责流程，合同系统只做适配、关联和结果同步；开发使用 Mock。
- AI 平台负责承载合同解析和法律审查，通过合同类型、任务类型和审查视角路由；Dify 是当前一期推荐实现，不是永久硬依赖。
- 当前 `合同管理系统需求设计.md` 已补齐类型差异矩阵、通用流程、功能清单、权限、接口、非功能要求和一期验收标准；数据库设计以该基线为输入。
- 当前需求评审展示以 `合同管理系统需求思维导图.md` 为准，文字需求作为详细说明；两者必须保持同步。
- 员工合同统一纳入合同系统；员工主数据引用 HR 系统，不在合同系统复制员工主档案。
- 合同业务状态为草稿、待签署、履行中、已完成、已终止、作废；审批中属于外部 `approval_status`，不占合同业务状态；草稿和待签署允许作废，履行中通过终止处理。

### 6.2 商用页面与闭环规则

- 工作台保留四项核心指标卡片；即将到期和待处理AI任务作为待处理事项列表，指标必须复用统一查询口径，并可点击进入对应台账或 AI 任务筛选。
- 合同台账负责完整查询、批量导出和权限校验；工作台不重复建设台账。
- 合同详情按业务任务分组承载正文版本、条款履行、收付款、AI与审批、附件变更和审计，空页签不显示。
- 所有动作必须有后端前置校验、状态变化、版本记录、幂等控制、失败提示、审计记录和下一步入口。
- 创建、AI、外部审批、签署、履行、变更和终止必须形成可回溯的端到端链路。

### 6.3 一致性硬规则

- 待签署撤回必须回草稿，原审批实例作废并生成新审批实例，历史审批记录不可覆盖。
- 附件必须具备上传、预览、下载、版本查看和按权限删除能力；删除附件必须审计。
- `our_role` 是合同独立维度，必须同时出现在页面配置、合同主数据、AI 输入和数据库设计中。
- 正文全文检索为二期能力；到期提醒一期提供站内待处理事项，不强行绑定邮件或推送渠道。

### 6.4 模板与 AI 关系

- 模板库不是简单模板列表，必须支持分类、适用场景、标签、关键词检索、收藏/常用、Word 导入和批量配置导入。
- 模板变量与合同字段、类型扩展字段、AI 解析输出共用字段字典，避免同一字段重复定义。
- AI 只负责合同解析和法律风险审查：解析解决文本结构化，审查解决风险识别；AI 不审批、不签署、不改变合同状态。

### 6.5 权限配置

- 权限基础参考 HR 招聘系统：角色菜单权限、角色数据范围和后端数据过滤。
- 合同系统补充合同类型、模板、字段和合同级授权；销售、HR、采购、法务共用合同中心，通过权限过滤内容。
- 权限不是“角色等于合同类型”；合同类型决定业务规则，角色和数据范围决定可见合同，字段权限决定敏感内容，合同级授权处理跨部门协作。

- 合同大类只作为导航分组，不是独立权限判断单位；合同类型及子类型是实际授权和校验单位。大类可见性由其下至少一个有权限的启用合同类型派生。
- 一期复用 HR 系统基础组织数据范围（本人/本部门/本部门及下级/指定组织等）与合同类型权限取交集；多部门、多组织叠加范围列入二期。
- `deleted=0`（未软删除）是数据保留条件，不等于未作废；作废不是权限过滤条件，而是展示和有效统计规则。作废合同仍可按权限查看，但不计入有效业务统计。
- “数据权限与类型可见范围”是系统运行规则，不是独立菜单；管理员配置入口归“系统管理 → 权限与审计 → 权限过滤规则”。
- 一期数据库以 `D:\project\hr-contract\数据库表\合同管理.docx` 当前版本为唯一依据；具体表和字段必须从 DOCX 当前版本读取。
- 合同级授权是例外并集，只能补充指定合同、指定用户/角色、指定动作和有效期，不能扩大系统管理权限；授权和撤销必须审计。
- 权限落表复用 HR 系统基础 RBAC 和数据范围表，合同系统新增合同类型权限、模板权限、字段权限和合同级授权表。

### 6.6 外部化的「决定 vs 落地」

- 「主体引用外部」「审批外部化」「AI 平台外置」「文件用文件服务」是业务决定（数据/能力放哪）；Dify 只是当前推荐的 AI 平台实现。
- 公司已有统一文件服务；“合同中心区”是其中分配给合同系统的业务存储空间。合同系统不自建物理文件存储，不在数据库保存文件二进制、物理路径或长期公开 URL；附件关联、权限和审计在合同系统，物理存储、预览下载和短期链接在文件服务。
- 「外部连接配置」是这些决定的技术落地：存各外部系统的地址、密钥、测试连通、启停用、记日志。
- 一句话：决定是「去别人那查/办」，外部连接配置是「把路铺好」。

### 6.7 商用 AI 架构原则

- 合同系统不直接依赖 Dify API 或其返回结构，必须通过 AI Provider 适配层接入。
- Provider 至少预留 `mock`、`dify`、直接模型 API 和其他平台实现；任务管理、状态、重试、幂等、版本绑定、Schema 校验、人工确认、权限和审计归合同系统负责。
- AI 平台只负责解析和审查推理，不负责合同状态、审批、签署、最终正文或自动确认业务数据。
- 上线前必须评估私有化和数据隔离、模型/工作流版本、监控告警、成本限额、故障降级、结果评测、人工兜底和供应商迁移成本。

## 7. 分期（一期/二期/三期）
- **一期（可商用 MVP）**：合同全生命周期、模板起草、导入、AI 解析、AI 审查、AI 中台接入配置（Dify 对接）、外部审批适配和 Mock、外部主体引用、审计日志、系统管理。
- **二期（增强）**：外部交易对方主数据系统深度对接、金额分级审批、多部门/多组织叠加数据隔离、部分收付、逾期自动标记+提醒、多币种折算、主体去重增强、统计报表增强。
- **三期（扩展）**：电子签章、归档借阅、智能续约/条款比对等更多 AI。

## Implementation Baseline (2026-08-31)

- Actual implementation progress is recorded in `项目开发进度与变更记录.md`; this Wiki retains stable business and technical decisions.
- External systems are integrated through adapters. HR/organization, trading-party master data, legal entities, file service, approval service, and AI providers remain external owners; the contract system owns configuration, contract associations, permission checks, and audit trails.
- Connection secrets are never returned in plain text or written to normal logs. Connection logs store only action, result, time, and an error summary.

## First-Phase Data Scope Rules

- Access is determined by the intersection of role permissions, data scope, contract-type permission, sensitive-field permission, and contract-level authorization.
- Supported scopes: all data, organization, department, self and participants, and named contracts.

## Statistics Rules (2026-08-31)

- Statistics is a read-only navigation module. It reuses the contract visibility and type-permission boundary and does not create a second business ledger.
- Deleted records are excluded from queries. Void contracts remain viewable when authorized but are excluded from valid business totals.
- Current implementation reports real lifecycle, payment-plan, fulfillment-milestone, and AI-task/review counts. Contract amount totals remain unavailable until the formal contract-main amount fields are migrated from the DOCX baseline.
- A statistics result must navigate to the corresponding ledger or fulfillment workspace with the relevant filter applied.

## Home Page Boundary (2026-08-31)

- A system-level Home page is optional, not mandatory.
- It should only be enabled when real data from multiple business domains can support a useful cross-module summary.
- Before that condition is met, keep Workbench under Contract Center and do not create a decorative or duplicate summary page.
- If enabled, Home and Contract Workbench have different responsibilities: Home summarizes the system; Workbench supports contract operations.

## Navigation Visual Rules (2026-08-31)

- First-level business nodes use icon, text, and chevron when expandable; they are not plain group captions.
- Second-level pages use lighter icons and indentation. The selected page uses a restrained blue highlight and left marker.
- Navigation typography uses the local Chinese system font stack. First level is 14px/600; second level is 13px/400, becoming 600 when selected.
- Explicit denial takes precedence. Contract-level authorization may add access to a named contract but cannot grant system administration rights.

## Permission And Audit Implementation Boundary (2026-08-31)

- The local implementation stores configurable role actions, default data scopes, contract-type access, and sensitive-field access in the contract system during the mock stage.
- The system currently has four seeded roles: ADMIN, LEGAL, BUSINESS, and VIEWER. Production users, organizations, and assignments must be sourced from the HR/SSO adapter.
- Global audit reads contract operation history together with permission configuration changes. File access, external approval synchronization, AI provider events, and export events must join the same audit query when their formal adapters are implemented.

## New Contract And Detail Workspace (2026-08-31)

- `CreateContractWorkspace.vue` provides a guided contract-draft workflow and emits the existing save operation to `App.vue`.
- Contract detail uses task tabs and contextual signing/change entries. Lifecycle rules and endpoints remain unchanged.
- Dedicated ledger, fulfillment, and template workspaces are the sole rendered implementation for their navigation targets.

## Formal Field Mapping (2026-08-31)

- The DOCX contract-main authority includes `total_amount`, `currency`, `payment_direction`, `sign_date`, `effective_date`, `expire_date`, `source_type`, and applicant organization fields.
- First-phase compatibility mapping adds these fields to the legacy local contract table and mirrors them into `t_contract_main`.
- The detail page displays the formal field group. Missing values are visible as `未设置`, and no mock business values are generated.

## Scope And Formal Edit Closure (2026-08-31)

- Create and draft-update requests accept and validate `totalAmount`, `currency`, `paymentDirection`, and `expireDate`.
- List, detail, fulfillment, and statistics endpoints use the configured role scope and type permissions together.
- Current mock identity defaults to user `10001`, organization `100`, department `101`; headers can replace these values until HR/SSO is connected.

## Identity Scope Acceptance (2026-08-31)

- `GET /api/directory/mock` returns mock users, organizations, and departments for acceptance testing.
- `IdentityScopeWorkspace.vue` switches the active identity and reloads the visible contract set.
- Identity headers are propagated to ledger, detail, fulfillment, and statistics calls.

## Contract-Level Authorization (2026-08-31)

- `t_contract_object_auth` stores target user, view/edit/export/legal-confirm permissions, expiry, status, and authorization remark.
- APIs: `GET/POST /api/contract-authorizations`, `POST /api/contract-authorizations/{id}/revoke`.
- The authorization workspace is contained in the existing Permissions and Audit module, not added as a top-level business navigation module.

## SQLite Operations (2026-08-31)

- Development database: `backend/data/contract-system.db`.
- Backup: `backend/scripts/backup-sqlite.ps1`; restore: `backend/scripts/restore-sqlite.ps1` after stopping the backend.
- Operating instructions: `SQLite开发运行与验收说明.md`.

## SQLite Regression

## MySQL DEV Runtime (2026-09-01)

- Profile: `mysql`; configuration: `backend/src/main/resources/application-mysql.yml`.
- Run from `backend`: `./scripts/run-mysql-dev.ps1 -DbPassword '<DEV password>'`.
- Baseline: `src/main/resources/db/migration/V1__contract_schema.sql`. Flyway is disabled for the shared DEV profile because the baseline was applied manually.
- `MySqlCompatibilitySchema` is transitional: legacy APIs remain operational and core contract/party writes are mirrored into the DOCX-defined authority tables.

## AI Acceptance Boundary (2026-09-01)

- The current accepted AI workflow uses `MockAiProvider`: contract content is required before extraction or review; results are persisted and require a legal confirmation action.
- Approval submission is routed through `ApprovalProvider`; the development provider returns a mock ticket. This validates contract workflow behavior without claiming external approval integration.
- `DifyAiProvider` exists only as a provider boundary. It requires real workflow mappings and credentials before live use.

## Dify Integration (2026-09-01)

- Extraction is `advanced-chat`: `POST /chat-messages`; legal review is `workflow`: `POST /workflows/run`. Both use streaming responses.
- Extraction inputs: `contract_text`, `contract_id`, `contract_type`, `content_version_id`, `schema_version`.
- Review inputs additionally include `our_role`, `review_view`, `confirmed_party_info`, and `query`.
- Run from `backend`: `./scripts/run-mysql-dev.ps1 -DbPassword '<DB password>' -UseDify -DifyBaseUrl '<Dify base URL>' -ExtractApiKey '<extract key>' -ReviewApiKey '<review key>'`.
- Keys remain backend environment variables and are never exposed to frontend.

- Run `backend/scripts/smoke-test.ps1` with the backend running to exercise contract creation, formal field persistence, content, AI, approval, named authorization, and edit denial.
- Use `SQLite商用验收清单.md` for the full manual regression matrix.
## AI 解析确认流程（2026-09-01）

AI 解析采用“识别 -> 复核 -> 确认 -> 正式字段”的闭环。原始 Dify 响应保留在 AI 任务记录中，页面只展示结构化建议；未经人工确认，不得改变合同正式字段。标准日期才允许回填，类似“验收后 30 日内”的内容继续显示为付款条件待确认信息。

解析确认接口由合同后端提供并统一经过 `VIEW`/`EDIT` 与合同级授权校验，前端不接触 Dify Key。AI 解析确认工作区位于合同详情，不新增独立一级导航，避免把 AI 能力割裂成脱离合同上下文的页面。
## AI 履行建议确认

付款计划和履行节点属于业务事实，AI 只能提供建议。确认工作区按条目提供选择权，用户可以修改名称、金额和日期；只有勾选并通过校验的条目才进入履行与收付款计划。每条写入关联解析任务来源，重复确认不会重复生成同一期数据。
## 登录与账号策略

当前开发环境使用 SQLite 本地账号和服务端 Session 完成登录闭环。合同系统不开放用户自助注册，账号由管理员维护；后续接入 IAM/SSO 时只替换认证来源，业务角色、数据范围和页面不改。
## 管理员账号策略

账号维护属于系统管理，不属于公开注册。管理员可以维护账号状态、角色、组织/部门归属和密码；每次变更进入审计。角色权限仍由权限配置区维护，账号只引用角色，不复制权限明细。
## HR/IAM 身份目录接入边界

正式 Adapter 需要提供当前用户、用户查询、组织查询、部门查询和账号停用通知能力。合同系统只读取稳定用户 ID、账号状态、显示名、组织 ID、部门 ID 与角色映射；主数据维护仍由 HR/IAM 负责。接口、认证方式和字段未确认前，系统使用本地目录降级并明确展示状态。
## IAM BFF/OIDC 双入口

合同系统保留自有登录页，但正式认证由 IAM 完成；门户入口通过 OIDC Authorization Code 回调。两条入口最后都建立合同系统 HttpOnly Session。后端 BFF 代理外部服务请求并注入 IAM Token 和 `X-App-Code`，浏览器不接触 Token。IAM OIDC 路径以环境变量配置，不能假定平台固定路径。
## IAM application integration
- Backend endpoints: `POST /api/auth/login`, `GET /api/auth/oidc/login`, `GET /api/auth/oidc/callback`, and `POST /api/bff/proxy`.
- Required runtime settings: `IAM_BASE_URL`, `IAM_APP_CODE`, `IAM_CLIENT_ID`; portal OIDC additionally needs `IAM_CLIENT_SECRET` and the platform-confirmed authorize/token paths.
- `IAM_GATEWAY_BASE_URL` is optional and isolates document/metadata gateway calls from IAM authentication endpoints.

## MySQL runtime
- Use the `mysql` Spring profile for RDS. The baseline is additive and tracked by `flyway_schema_history`.
- Required runtime settings: `SPRING_PROFILES_ACTIVE=mysql`, `DB_PASSWORD`, and optionally `SERVER_PORT`.
- The application includes the Flyway MySQL extension and a MySQL-compatible `local_user` table definition.
- The staged cutover installs MySQL triggers after compatibility tables are created, synchronizing content, attachments, payments, performance, AI, changes and templates into formal tables. This is a transition layer, not yet a claim that every read path has switched.

## Dify workflow runtime
- Configure `DIFY_BASE_URL=http://metric-dify.qd-ecs.com:8088/v1` for the current development environment.
- Contract extraction calls `/chat-messages` and finishes at `message_end`; legal review calls `/workflows/run` and finishes at `workflow_finished.data.outputs`.
- Workflow results support a direct output object or a JSON string in `outputs.result`.
- Legal-review results may have an additional `result` object wrapper. Extraction payment plans accept numeric direction (`1` receive, `2` pay), `plan_amount`, `plan_due_date`, `trigger_condition`, and `evidence`.

## IAM 账号归属（长期）

- 正式接入 IAM 后，自有登录页（BFF）和门户 SSO（OIDC）的账号都存在于 IAM，合同系统不保存账号和密码，只接收身份结果（userId/displayName/orgId/departmentId 等）并建立 HttpOnly Session。
- 自有登录页只是登录界面，账号密码由后端调 IAM `/api/v1/auth/login/password` 校验；OIDC 门户才有浏览器授权码回调。两条入口归一到同一 IAM 身份和本地会话。
- 仅 `CONTRACT_AUTH_MODE=local` 开发模式在合同系统 `local_user` 表保存账号（admin/admin123，SHA-256 摘要）；接 IAM 后废弃。
- 合同角色（ADMIN/LEGAL/BUSINESS/VIEWER）是合同域自有权限模型，不等于 IAM 的 roleIds/isSuperAdmin；两者需单独映射，登录时不能把 IAM 角色直接当合同角色。

## 页面组织原则（长期）

- 业务模块才单独成导航页；所有"配置"收敛进"基础配置"页；账号与权限单独成页。参照 HR 招聘系统的扁平结构。
- 归类不等于删除：合并只收导航入口，原组件保留为分组页内 Tab/子页，功能不丢失。
