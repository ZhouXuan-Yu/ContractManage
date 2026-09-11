# Learning.md — 复盘记录

> 定义：这次哪里做错了、为什么错、下次怎么改。

## 错误1：把「数据字段」当「功能块」画进思维导图
- 现象：思维导图里写「条款解析（付款/违约/交付/…）」，括号里全是字段名。
- 原因：混淆了「功能（动词）」和「数据（名词）」两个层次。
- 改正：思维导图主干只画动词（发起/跟踪/查看/修正），字段归数据库表设计。

## 错误2：状态机「作废」范围定窄了
- 现象：作废只允许「草稿/审批中」，漏了「待签署」。
- 原因：没抓住「签署是分水岭」这个关键。
- 改正：签约前（草稿/审批中/待签署）都可作废；签约后（履行中）只能终止。

## 错误3：跳步骤、急着全量推进
- 现象：栏目1 还没逐个梳理完，就跳到全系统评审 + 三期总览。
- 原因：把用户「标一期二期三期」误解为「一次全做完」。
- 改正：按栏目一个一个来，分期标签嵌在**每个栏目内部**，而不是单独甩一张总表。

## 错误4：把标准文档的笔误当成用户错误
- 现象：误以为 `status_1` 是用户写错，实际是《人力资源管理系统数据库设计文档4》里就有的。
- 改正：下结论前先核对标准文档原文，再判断是谁的问题。

## 错误5：把「AI 专家体」当外挂，没写进主文档
- 现象：把 AI 专家体设计写成单独文件，主文档里只有「AI 解析 / AI 审查」两个词，没讲清它是什么、要达到什么、用来干嘛。
- 原因：因为实现是「外部 Dify 搭建」，就误以为它是「另一个系统」，不用进本系统主文档。
- 改正：AI 专家体是本系统的**核心卖点模块**，必须作为正式模块进主文档；「实现外置」≠「功能外置」。核心能力要在主文档讲清「解决什么问题 / 做什么 / 达到什么 / 用途」。

## 错误6：反复被外部 review 的「待编写」带偏
- 现象：另一个 AI 三次判断「AI专家体需求设计.md 尚未产出 / 需标【待编写】」，我一度动摇。
- 原因：没先核对文件是否已产出，就顺着外部判断走。
- 改正：外部 review 对「文档完成状态」的判断，先自己读文件核对再决定；文件已存在且完整，就不标「待编写」。

## 错误7：把补充协议/变更协议当成合同类型
- 现象：在合同类型目录中把补充协议和变更协议与采购、租赁等并列。
- 原因：混淆了合同分类和合同变更关系。
- 改正：补充协议/变更协议依附原合同，作为独立业务能力和关联关系设计，不作为合同类型。

## 错误8：员工外部 ID 在通用关联和类型扩展中重复
- 现象：员工合同扩展字段写入 `employee_id`，交易对方引用又保存外部主体 ID。
- 原因：没有先确定交易对方引用是通用底座的一部分。
- 改正：员工合同的 `employee_id` 只保存在交易对方引用中；类型扩展只保存员工合同专属字段，关键时点另存快照。

## 错误9：审批锁定曾绑定合同业务状态
- 现象：旧状态机把「审批中」作为合同状态节点。
- 原因：把外部审批流程状态和合同业务生命周期混在一起。
- 改正：合同业务状态保持草稿，外部审批使用 `approval_status`；审批中时锁定关键字段，列表按审批状态筛选。

## 错误10：没有按原版格式重构思维导图
- 现象：新建的思维导图变成了合同类型和字段的树状清单，缺少原版的页面与功能版、导航层级版、功能全量版、页面信息架构版和页面设计标准版。
- 原因：把“有层级的目录”误当成“可用于产品评审的思维导图”，没有遵守项目既定的功能/数据分层规则。
- 改正：原 `合同管理系统需求思维导图.md` 作为唯一主文件，功能使用“查看/创建/发起/跟踪/确认/维护”等动词表达，字段和数据类型全部放入数据库设计。

## 错误11：只改导航名称，没有重做业务结构
- 现象：虽然移除了交易对方导航，但仍保留主体列表、企业/个人主体分类和主体增删改等旧内容。
- 原因：把“删除一个菜单”误当成“重新定义业务边界”。
- 改正：合同是唯一业务中心；交易对方只做外部主体引用、合同身份映射和关键时点快照，主体主数据由外部系统维护。

## 错误12：用 BI 行话「钻取」导致评审看不懂
- 现象：把「点击联动」改成「钻取」，用户追问「什么叫钻取」。
- 原因：面向老板/评审的思维导图用了数据分析领域术语，默认对方能懂。
- 改正：面向非技术评审的文档用直白词（「点击跳转」「点进去看」），行话只保留在内部技术文档里。

## Mistake: Project memory files were not updated first (2026-08-31)

- What happened: after completing several navigation modules, a separate progress document was added without first synchronizing `Memory.md`, `Wiki.md`, and `Learning.md`.
- Why it was wrong: conversation memory was treated as project memory, contrary to the documented project workflow.
- Prevention: each complete module must update `Memory.md` for current status and next step, `Wiki.md` for stable rules, `Learning.md` for process failures, and `项目开发进度与变更记录.md` for code changes and verification before the module is reported complete.

## Mistake: A Compile Error Was Introduced During Permission Refactoring (2026-08-31)

- What happened: `Map` was removed from the imports in `AccessControl` while its role map was still used.
- Prevention: compile backend immediately after each dependency or import refactor, before continuing with the frontend integration.

## Statistics must not invent unavailable fields (2026-08-31)

- The transitional contract table does not contain the complete formal amount and date fields.
- The statistics page therefore exposes only real payment-plan amounts and labels contract amount totals as unavailable.
- Before switching the contract amount/date metrics on, read the current DOCX schema and complete the formal-table mapping; do not add ad-hoc fields to the Mock table.

## Layout refinements must change the full application shell (2026-08-31)

- Do not treat a commercial-backoffice layout concern as an isolated card or button change.
- When the user reports unused side space or weak navigation, inspect sidebar, topbar, content width, page padding, panel density, and responsive rules together.

## A visual change needs a visible acceptance check (2026-08-31)

- CSS can compile while still being overridden by older selectors or failing the requested visual direction.
- For a shell redesign, inspect the running screen after implementation before reporting it as complete. If the expected hierarchy is not visible, continue from the screenshot rather than describing intended CSS changes.

## Reference structure must be translated as a whole (2026-08-31)

- The third reference is defined by the relationship between the dark brand band, independent light navigation rail, grouped entries, and content canvas.
- Reusing only its menu icons or colors does not satisfy the reference. Each visual block must state which structural relationship it adopts.

## Optional home page must follow data reality (2026-08-31)

- A reference product may have a separate Home page because it aggregates several real business domains.
- Do not copy that menu decision before this system has comparable cross-domain data.
- Reserve the decision, keep the current business ownership clear, and revisit it when real data integration makes the value testable.

## Workbench content must have a processing destination (2026-08-31)

- Promotional banners and duplicate quick-action areas consume the first screen without helping a user decide what to process.
- A workbench should prioritize measurable status, actionable queues, and recent records that lead directly to the relevant contract workflow.

## Operational pages need an explicit working hierarchy (2026-08-31)

- A ledger should lead from context and filters into selection, batch actions, and the working table.
- A fulfillment page should use master-detail structure so the user always knows which contract is being processed and which plans or milestones belong to it.

## Workspace ownership must be singular (2026-08-31)

- A navigation target must render exactly one page implementation. A legacy inline view beside a replacement workspace causes duplicate controls, inconsistent spacing, and unclear ownership.
- A guided creation flow fits contract drafts because it reduces first-entry errors while leaving content, approval, signing, fulfillment, and change handling in the contract-detail workflow.

## Formal fields need a compatibility phase (2026-08-31)

- When the authoritative schema is more complete than the running mock schema, add a compatibility mapping first and preserve existing demo data; do not replace tables in one step.
- Missing formal data must be shown as unavailable rather than inferred. Scope fields are only meaningful after HR/organization identity data and permission joins are available.

## Scope must be enforced at every read boundary (2026-08-31)

- Filtering only the list is insufficient: detail and child resources such as fulfillment plans must also reject contracts outside the current role scope.
- When combining permissions, append constraints instead of replacing the existing SQL predicate; otherwise a type filter can accidentally bypass organization scope.

## Keep identity integration replaceable (2026-08-31)

- A mock directory is useful for validating scope behavior, but it must sit behind an endpoint/context boundary so HR/SSO can replace the source without changing business screens.
- Every child-resource request must carry the same identity context as the list request; otherwise users can see a filtered list but access unrelated detail or fulfillment data directly.

## Contract authorization is an additive exception (2026-08-31)

- Contract-level access supplements a user's normal scope for a named contract. It must be checked at each action boundary and cannot grant unrelated system-management rights.
- Authorization should retain both revocation state and expiry; deleting authorization rows loses auditability and makes access history ambiguous.

## Do not infer production database decisions (2026-08-31)

## MySQL Migration Lesson (2026-09-01)

- DDL completion and runtime cutover are different acceptance points: tables existing does not prove application read/write paths use them.
- SQLite SQL is not directly portable: MySQL 8 rejects defaults on `TEXT`, and SQLite `ON CONFLICT` requires a MySQL equivalent.
- Stage a MySQL compatibility boundary, verify mirror writes, then migrate each business module to formal authority tables in complete blocks.
- Keep database credentials in environment variables or a secret store only.

## AI provider acceptance needs two separate proofs (2026-09-01)

- Validate the business workflow independently from the vendor: content prerequisite, task lifecycle, risk report persistence, confirmation, and downstream approval should pass with a controlled Mock provider.
- A provider interface alone is insufficient when business services bypass it. The service must invoke the provider boundary so a real Dify adapter can replace Mock behavior without rewriting the workflow.
- A live AI integration is not complete until its workflow identifiers, input fields, output schema, failure behavior, and credentials are tested against its real environment.

## Dify app mode and streaming (2026-09-01)

- A Dify app can be configured as `advanced-chat` even when the business name says workflow; using `/workflows/run` then returns `not_workflow_app`.
- Long executions can be cut off by nginx in blocking mode. Streaming mode must consume completion events and avoid that gateway timeout.
- Persist provider output before normalization; normalization failures must be visible and must not create fabricated business data.

- When database connection details and deployment ownership are unavailable, keep SQLite as the active development store and pause formal migration work. Database structure should not be invented from a future deployment assumption.
## AI 结果不能直接写入正式字段（2026-09-01）

- AI 结构化输出即使格式正确，也不代表业务事实已被确认；必须保留原始结果、提供字段级复核，并由用户显式确认后回填。
- 日期是高风险字段。自然语言期限不能直接转换成到期日期，否则会制造错误合同数据；应保留原文并进入待确认队列。
- 页面设计上，AI 能力应紧贴合同详情和正文上下文。深色科技感只用于 AI 决策工作区，不能让整个商用后台变成装饰性大屏。
## AI 建议写入履行数据的边界（2026-09-01）

- 付款计划必须同时具备名称、正数金额和标准日期；缺少任一项就不能静默写入。
- AI 结果确认需要条目级选择，而不是一个“确认全部”按钮；合同中常见的自然语言条件必须留给人工判断。
- 幂等不能只依靠前端按钮禁用，后端需要用合同、解析任务和期次作为重复确认的业务键。
## 企业系统登录设计（2026-09-01）

- 登录状态不应由前端 localStorage 中的标记决定，必须由后端会话验证；否则用户可以伪造管理员身份。
- 本地开发账号可以作为替换适配器，但密码仍应保存摘要，且会话失效要能统一处理 401。
- 企业系统通常不做公开注册，账号生命周期交给管理员或 IAM/HR；“登录页存在”不等于“允许自行注册”。
## 账号管理与角色引用（2026-09-01）

- 用户和角色要分离：账号保存角色编码，权限从角色配置读取，避免每个用户复制一套权限造成口径漂移。
- 停用账号必须在登录查询条件中拦截，不能只在页面隐藏按钮。
- 密码重置和角色调整属于高风险管理动作，必须有管理员权限和审计记录。
## 外部目录的降级边界（2026-09-02）

- “Provider 可替换”不等于“已经接通”；必须在运行状态中区分正式外部连接、Mock 和本地降级。
- 身份目录是只读主数据来源，合同系统不应在本地复制一套组织维护能力，否则会产生双主数据。
- 对接前先冻结字段契约和错误降级行为，接口文档缺失时只提供契约检查，不伪造真实联调结果。
## 接入文档中的端点不能擅自写死（2026-09-02）

- BFF、OIDC、SSO 是不同层级：BFF 是后端调用架构，OIDC 是认证协议，SSO 是入口体验。
- 自有登录页和门户入口可以并存，但必须归一到同一 IAM 身份和本地会话。
- 当接入文档只描述 Token/授权端点而未给出固定路径时，应配置化端点，等待环境参数，不要假设可直接联调。
## IAM integration notes (2026-09-02)
- BFF and OIDC are complementary: BFF handles the system's own credential form; OIDC handles portal entry.
- OIDC token responses and IAM session/user responses use different field names. Normalize `accessToken`/`access_token` and obtain user identity from the server-side user endpoint before creating the local session.
- Portal publication and application visibility are later deployment steps, not prerequisites for local authentication development.

## MySQL migration notes (2026-09-02)
- Flyway 10 requires the database-specific `flyway-mysql` dependency for MySQL 8.
- A non-empty dedicated schema without Flyway history must be baselined before additive versioned migrations can run.
- SQLite `TEXT UNIQUE` definitions are not portable to MySQL. Key columns must use bounded `VARCHAR` types.

## Dify integration notes (2026-09-02)
- Dify application type controls the endpoint: Workflows use `/workflows/run`; chat applications use `/chat-messages`. UI labels alone must be confirmed by the API response (`not_workflow_app` exposed the actual extraction type).
- A HTTP 200 response containing an unrelated platform HTML page is not an API success. API verification requires the documented content type and JSON/SSE response.
- For long-running AI flows, the gateway timeout must exceed the work execution time; streaming is preferred, but an upstream idle timeout can still terminate the connection.
- Normalize real AI schemas at the adapter boundary: unwrap known result envelopes and convert numeric domain enums before they reach confirmation screens.

## Formal table cutover notes (2026-09-02)
- Flyway migrations run before Spring `@PostConstruct`; triggers that depend on compatibility tables must therefore be installed by the MySQL compatibility component after it creates those tables.
- Formal tables have stricter required audit fields and numeric enums than the development compatibility tables. A direct SQL table-name replacement risks null/enum failures and is not an acceptable migration strategy.
- The current safe stage is write mirroring with explicit field mapping. Formal read cutover requires checking live `information_schema` fields and resolving authorization and approval semantics first.

## 错误13：在不理解模块职责时建议"移除"
- 现象：页面归类评审时，我建议移除账号管理、权限与审计、数据范围验收、外部连接配置，被用户追问"你知道这些是干嘛的吗"。
- 原因：把"IAM 统一身份/权限"误读为"这些合同域模块冗余"，没有先读服务实现就下结论。
- 改正：判断模块去留前，先读对应 Service 代码搞清它做什么、归谁管、是不是合同域特有；"合并归类"是保留功能、只收导航入口，不等于"删除"。判断前先核实，再给结论。
