# Memory.md — 当前进度

> 定义：项目做到哪了、刚确认了什么、下次从哪继续。

## 当前阶段
一期开发准备与工程骨架初始化 —— 需求、数据库权威来源、外部集成边界和技术栈已完成多轮收口；业务切片尚未开始。

## 已完成
- 需求定位、功能范围和核心业务流程 → 已形成当前需求基线；数据库表不在本文件复制数量，唯一以 `D:\project\hr-contract\数据库表\合同管理.docx` 当前版本为准
- 需求思维导图（功能动词版）→ 定稿
- 栏目1 合同管理 → 完成
- 栏目2 交易对方 → 完成
- 栏目3 审批中心 → 完成（审批记录表已确认补 t_contract_approval_record）
- 栏目4 系统管理 → 完成（5 个子功能：合同模板 / 审计日志 / 角色权限 / 审批流程配置 / AI 中台接入配置；字典复用 HR 主系统，不设独立页面）
- 数据库设计 → 以用户持续维护的 `D:\project\hr-contract\数据库表\合同管理.docx` 为唯一权威来源；本文件不再记录旧版表数量或历史 Markdown 路径
- AI 专家体需求设计（审查要点/解析字段/报告格式）→ 完成（见 AI专家体需求设计.md）
- 第10章「业务规则定稿 & 开发边界」（10.1作废展示 / 10.2待签署撤回 / 10.3 AI异常 / 10.4强制转交 / 10.5补充协议 / 10.6开发边界5条）→ 定稿
- 二期数据看板（第 5 导航，只做合同自身统计，砍渠道/商机/回款指标）→ 需求设计 6.5
- 导航结构收口：AI 专家体不单列导航（嵌合同台账 + 系统管理），思维导图已删平级分支
- 思维导图版本收口：保留“页面与功能设计版 / 导航层级版 / 功能全量版”三种视角；页面版补充待签署撤回按钮、主体快照分期、强制转交校验和收付款二期边界
- 页面架构最终收口：合同管理为首页（合同台账视图 + 收付款计划视图），合同详情从合同列表或交易对方关联合同进入；审批中心为单一审批工作台，待办/已办/我发起为 Tab；合同模板归系统管理；字典复用主系统，不设独立页面

## 栏目1 决策（合同管理）
1. **新建方式**：模板起草 / 导入(PDF/DOCX) / 手工录入 三选一。
2. **AI 解析/审查不占状态机节点**：独立字段 `is_ai_extract_finish`（解析状态）+ 表 `t_contract_ai_review`（审查，可多次）。
3. **原文存储**：`origin_file_text` 用 MEDIUMTEXT（合同原文可能超 64KB）。
4. **PDF 导出**：合同或主体详情实时引用已保存数据，导出/打印时生成不可变 PDF 文件快照；主体签约时数据库快照属于二期能力。
5. **收付方向**：加 `payment_direction`（1收/2付），服务合同方向不定需显式指定。
6. **变更/补充协议**：记录前后差异，关联原合同。
7. **作废**：签约前（草稿/审批中/待签署）可作废，签约后（履行中）走终止。
8. **补充协议一期定「独立审批流」**：每个补充协议独立生成审批实例（独立 approval_no，与表 t_contract_change 一致）；后续如需关联原合同审批流再评估。

## 栏目2 决策（交易对方）
1. **软删除**：业务数据用软删除 `deleted` + 保留期；日志数据定期物理清理。
2. 主体字段补全：`credit_code`（企业信用代码唯一）、`id_number`（个人身份证号唯一）。
3. 去重：信用代码/身份证号精确查重，命中提示「已存在」；未填允许重复，二期名称相似度+手动合并。
4. 统计口径：关联合同汇总「累计份数/总金额」**不含已作废**。
5. 主体快照分两期：一期导出/打印时按当前已保存主体信息生成不可变 PDF，记录快照时间、操作者和文件地址；二期合同签署生效时另存主体全量数据库快照，用于追溯签约时点数据。
6. 删除约束：名下还有合同（含作废）的主体不许删。
7. 状态机修正：作废允许「草稿/审批中/待签署」；签约后走「终止」。
8. **签署快照固化（二期）**：签约时刻把交易对方完整信息落库快照（数据库层面）；一期仅 PDF 导出固化。

## 栏目3 决策（审批中心，本轮）
1. **审批流程配置放「系统管理」**（不放审批中心），管理员专属。
2. 一期**按合同类型分别配置审批人**（采购/销售/服务各不同），配置加 `contract_type`。
3. 配置变更**只对新单生效**，在途单存「提交时配置快照」。
4. 待签署加**回退出口**（撤回签署 → 回草稿 → 改后重提）。
5. 转交加「转交说明」字段（可选）。
6. 审批详情**内嵌合同原文预览**（AI 结果/风险报告做 tab），不靠跳转。
7. 待办显示「已等待 X 天」超 3 天标红（纯前端，不加字段）。
8. 管理员「强制转交」兜底（审批人请假/离职）。
9. 撤回条件：审批中可撤、撤回回草稿。
10. 补充：审批期间合同**锁定**、驳回重提**保留多轮历史**、审批意见**发起人可见**。
11. 主系统 `sys_approval` 无「审批记录表」，本项目已补 `t_contract_approval_record`。

## 栏目4 决策（系统管理，本轮）
1. **审计日志**：保留期内禁止软删、禁止物理删，只做冷数据归档迁移；到期按制度处理。与合同详情页「操作日志 tab」同一张表 `t_contract_operate_log`（全局视角）。
2. **权限矩阵**：AI 审查独立成一列、分四档（发起 / 编辑意见 / 仅查看）；5 角色=系统管理员/合同管理员/法务/业务审批人/只读查看者。
3. **修正**：系统管理员 AI 审查=仅查看不改意见（法律结论归法务）；系统管理员审批中心补「强制转交」兜底。
4. **补充约束**：作废/删附件→管理员+合同管理员；草稿编辑→发起人+合同管理员；导出按钮纳入权限管控。
5. **字典**：补 `approval_action`（1提交/2通过/3驳回/4转交/5撤回）；系统内置字典不可删。
6. **审批流程配置**：放系统管理（不放审批中心），按合同类型单节点配置。
7. **AI 中台接入配置（4.5 新增）**：新增表 `t_contract_ai_config`，配 Dify 应用 URL + AppID/AppSecret + 专家类型 + 超时重试 + 轮询/回调 + 测试连接。
8. **AI 中台两层**：外层=合同系统→Dify「法律专家」应用（合同系统配）；内层=Dify→大模型（大模型 key 在 Dify 后台配，自申请）。合同系统只配外层一层。
9. **专家体需自建**：公司现无 AI 中台，用 Dify 私有化部署搭「法律专家」（合同解析 + 法律审查两能力）。

## 待办（下次从这继续）
1. 以 `D:\project\hr-contract\数据库表\合同管理.docx` 当前版本为准，建立数据库迁移基线；不按旧版增量方案合并。
2. 补充协议/变更的附件关联逻辑确认（以当前 DOCX 字段为准）。
3. 一期种子数据（角色/字典）+ 系统测试用例（进开发前补）。
4. 与外部系统确认 HR/组织、交易对方、我方主体、文件服务、审批和 AI Provider 接口契约。
5. 开发前补充 AI 结果 schema、幂等键和人工确认合并规则的接口协议。
6. 继续按“功能全量 → 导航层级 → 页面设计 → 字段数据”的顺序维护文档，避免把功能误读成菜单。

## 已确认（主系统核对结果）
- 主系统**无**「审批记录」表 → 本项目补 `t_contract_approval_record`。
- 主系统**无**「AI 中台接入配置」可复用 → 本项目新增 `t_contract_ai_config`。

## 通用合同重构（本轮评审）

- 产品定位调整为**通用合同管理系统**，不再限定为商务合同。
- 员工合同纳入合同系统；员工、客户、供应商等主数据仍由外部系统维护，合同系统只保存外部主体 ID、必要展示信息和关键时点快照。
- 合同采用“通用底座 + 类型扩展”，类型扩展字段不重复保存通用底座已有的交易对方引用。
- 补充协议和变更协议不是合同类型，而是依附原合同的变更关系，必须关联原合同。
- 审批引擎外部化：合同系统不实现审批引擎，通过适配层对接外部审批系统，开发阶段使用 Mock。
- 合同业务状态与 `approval_status` 分离；提交审批后业务状态保持草稿，由 `approval_status` 表示审批中并驱动关键字段锁定。
- AI 专家体继续采用 Dify，统一合同解析和法律审查能力，通过合同类型、任务类型和审查视角路由。
- 交易对方重新定义为合同引用：本系统不建设主体列表、主体增删改、主体启停用或主体去重；“企业/个人”只是外部主体自然属性，员工/客户/供应商等是合同业务身份。

## 本轮方案设计（已纳入一期基线）

1. 劳动合同专指公司与员工的劳动关系合同；劳务协议、顾问协议作为独立合同类型管理。
2. 默认单主体部署，但模型预留我方主体引用，以支持母公司、子公司等多法人扩展。
3. 合同类型采用“合同大类 + 合同类型 + 合同子类型”；交易方向和协议性质单独建维度。
4. 补充协议/变更协议通过原合同关系管理，不作为合同类型。
5. 一期启用类型、外部适配边界和审批/AI 接入方式已统一定稿；正式接口的鉴权、ID 和回调细节由后续 Adapter 联调按外部系统契约落地，不阻塞开发。

> 说明：归档目录内的旧需求和旧数据库 Markdown 仅作历史记录，仍可能包含旧商务合同、内部审批、固定类型和旧表结构；当前开发不得引用。数据库开发唯一依据是 `D:\project\hr-contract\数据库表\合同管理.docx` 当前版本。

## 后续工作路线（当前）

每完成一个阶段，必须同步说明完成内容、当前结论、下一阶段、待确认事项和预期产出。

1. **需求基线**：确认通用合同边界、合同分类目录、一期范围和外部系统边界。
2. **业务模型**：梳理通用合同流程、合同类型差异、变更/补充协议、履行和收付款规则。
3. **数据模型**：依据《人力资源管理系统数据库设计文档4》设计表、字段、索引、快照和 JSON 扩展。
4. **接口契约**：定义 HR/组织、外部交易对方、我方主体、文件服务、审批系统和 AI Provider 的请求、响应、鉴权、幂等和异常规则。
5. **页面与权限**：设计合同台账、详情、模板、AI、变更、外部审批状态和敏感数据权限。
6. **测试方案**：设计 Mock 外部系统、主流程、类型扩展、AI 异步任务、审批同步和数据一致性用例。
7. **技术实现**：确认技术栈和工程结构后，再开始 backend/frontend 开发。

当前下一步：将完整需求分析同步为思维导图评审版，完成后进入数据库设计。

## 完整需求分析（本轮完成）

- 已补齐合同类型差异矩阵，覆盖用工、交易合作、资产资金、权利成果和自定义合同。
- 已补齐创建、版本、AI 解析、AI 审查、外部审批、签署履行、变更补充协议等通用流程。
- 已补齐合同台账、详情、类型配置、模板、交易对方、附件、履行收付款和审计日志功能清单。
- 已补齐角色权限、外部主体/文件/审批/AI Provider 接口需求、非功能要求和一期验收标准。
- 需求阶段产出文件为 `合同管理系统需求设计.md`，当前已达到数据库设计前的完整需求基线。
- 已重构原 `合同管理系统需求思维导图.md`，恢复页面与功能版、导航层级版、功能全量版、页面信息架构版、页面设计标准版、操作约束版、状态版和 Mermaid 版，作为需求评审主入口；字段不在思维导图展开。
- 已补充模块详细设计：合同中心拆为台账、创建登记、内容版本、交易对方引用、AI解析、AI审查、外部审批、签署生效、履行收付款、变更补充协议、附件文件、审计导出；合同配置拆为分类、模板、我方主体、外部连接、权限和审计。
- 已生成真正的 XMind 文件：`D:\project\hr-contract\流程设计\通用合同管理系统-需求思维导图.xmind`，以现有 XMind 包格式为模板，内容采用新方案，不再使用旧四大栏目。
- 已参考 `D:\project\customers\客户管理 (1).xmind` 的层级表达方式，新增并同步 `合同管理系统需求思维导图-清晰版.md`：按“功能模块 → 页面 → 列表/详情/卡片/查询/操作/校验/规则”组织，避免内容平铺。
- 已进一步合并同组节点：卡片、列表内容、查询条件、操作和规则均按组写在同一行，避免复制到 XMind 后生成多个无意义子框。
- 已统一修正思维导图：补充当前文档关系；补充“草稿/待签署可作废、履行中可终止”的状态机；明确员工合同统一纳入合同系统，员工主数据引用 HR 系统且不复制。

下一步：核对 Markdown 思维导图、XMind 文件和详细需求的模块、动作、规则和期次是否一一对应；核对完成后，再依据《人力资源管理系统数据库设计文档4》开始数据库概念模型、表清单和字段级设计。

## 商用页面基线（2026-08-24）

- 工作台保留四项核心指标卡片：合同总数、草稿数量、待签署数量、履行中数量；即将到期和待处理AI任务进入待处理事项列表，不为了凑卡片增加重复指标。
- 每个工作台指标必须有统计口径、权限范围、空状态、点击后的筛选条件；草稿数量不包含外部审批中的合同，AI待处理不包含正常处理中任务。
- 合同台账独立承担检索、组合筛选、导入、导出和批量导出；工作台只做概览和入口，不复制完整台账。
- 合同详情按概览、正文与版本、条款与履行、收付款、智能与审批、附件与变更、操作记录分组，AI、外部审批、签署、履行和变更通过详情动作串联。
- 已将“创建草稿→AI解析/审查→外部审批→待签署→生效履行→完成/终止/变更”的端到端闭环写入商用版思维导图和详细需求。
- 商用版主思维导图：`合同管理系统需求思维导图.md`；XMind复制使用：`合同管理系统需求思维导图-XMind导入.txt`。

## 商用版一致性修正（2026-08-24）

- 待签署支持“撤回签署”：回到草稿，原审批实例作废，重新提交生成新的审批实例，历史记录保留。
- 合同附件明确支持上传、预览、下载、版本查看和按权限删除；删除附件权限单独列出。
- 我方角色（`our_role`）恢复为合同独立维度，与数据库和 Dify AI 审查输入保持一致。
- 正文关键词检索列为二期全文索引能力；到期提醒一期只做工作台待处理事项，推送渠道后续通过通知接口扩展。
- 思维导图文件收敛为 `合同管理系统需求思维导图.md` 和 `合同管理系统需求思维导图-XMind导入.txt`；旧副本已清理，工具脚本和历史评审文档保留但不作为需求基线。

## 模板与 AI 统一设计（2026-08-24）

- 模板库按模板分类、适用合同类型/子类型、适用场景和标签组织，支持名称关键词检索、收藏/常用和批量导入。
- Word 用于合同模板文件；Excel 用于模板元数据、变量映射和批量配置导入，不默认作为合同正文模板。
- 模板变量、合同通用字段、类型扩展字段和 AI 解析结果统一引用字段字典，字段编码只定义一次。
- AI 只做两件事：合同解析和法律风险审查。解析把文本转换为结构化字段，审查输出风险、依据、说明和建议；两者都必须绑定合同版本并人工确认，不改变合同状态。

## 权限设计基线（2026-08-25）

- 参考 HR 招聘系统已有的角色菜单权限、角色数据范围和后端数据过滤模型，不重复发明基础 RBAC。
- 合同系统在基础权限上扩展合同类型权限、模板使用权限、字段敏感权限和合同级临时授权。
- 权限判定链路为：菜单权限 → 数据范围 → 合同类型/模板权限 → 字段权限 → 合同级授权；前端显示控制不能替代后端校验。
- 销售、HR、采购、法务使用统一合同中心，通过权限过滤合同、类型、模板、字段和操作，不拆成多个业务菜单。
- 合同大类是合同台账和履行与收付款页面内的上下文分组，进入大类后用合同类型顶部 Tab 平铺；大类不作为二级菜单，也不作为独立运行时权限单位。
- 合同类型及子类型是实际授权和后端校验单位；大类是否显示由其下至少一个有权限的启用类型派生。管理员的大类批量授权只是批量勾选当前类型的配置快捷方式。
- 一期只复用 HR 基础组织数据范围；同一用户跨多个部门或多个组织范围叠加列入二期，不能在需求中笼统写成一期“多部门数据隔离”。
- `deleted=0`（未软删除）是数据保留条件，不等于未作废；作废是合同业务状态和有效统计规则，不是权限过滤条件。作废合同仍可按权限查看并保留审计记录，但不计入有效业务统计。
- “数据权限与类型可见范围”不作为导航；已归入“系统管理 → 权限与审计 → 权限过滤规则”，业务页面只展示过滤后的结果。
- 历史数据库增量方案《数据库设计增量方案.md》曾记录新增表和字段设想，当前仅作追溯参考；数据库开发不得按其数量或清单合并，唯一依据是用户持续维护的 `数据库表\合同管理.docx` 当前版本。
- 公司已确认存在统一文件服务。“合同中心区”是文件服务为合同系统配置的业务存储空间，不是合同业务模块或自建存储；合同系统维护附件关联、版本、权限和审计，文件服务维护实际文件、预览/下载及短期访问链接。文件集成采用申请上传→上传完成确认→创建附件关联的闭环。

## 文件与数据库表收口（2026-08-25）

- 数据库表设计权威版由用户持续维护在 `D:\project\hr-contract\数据库表\合同管理.docx`，具体表数量、字段和版本以当前文件为准。
- 不再根据历史记录概括 DOCX 的表数量或列举“已覆盖全部待补项”；后续数据库开发前必须读取当前 DOCX 并以其当前内容建立迁移基线。
- 根目录旧《数据库表设计.md》已归档到 `归档/`；CLAUDE.md 关键文件清单已改为以 docx 为准。
- 文件清理：归档旧评审稿《合同管理需求梳理与设计评审.md》和旧数据库 md；删除 `build_contract_xmind.py` 与旧 `.xmind`（根目录 + 流程设计目录各一份）。思维导图保留 `.md` 源 + `-XMind导入.txt`（Tab 缩进，XMind 直接导入）。
- 思维导图「合同概况」卡片改为「指标 + 口径 + 钻取」结构（每卡片一行 + 公共规则 + 布局分工），「联动」统一为「钻取」；「钻取」对非技术评审偏行话，待确认是否改「点击跳转」。

## 商用 AI 方案原则（2026-08-27）

- Dify 不是必须绑定的永久技术选型；当前仅作为一期推荐的 AI 工作流和模型编排实现。
- 合同系统必须自建 AI 适配层和任务管理层，业务代码不得直接依赖 Dify 返回结构。
- AI 适配层统一抽象任务提交、状态查询、结果获取、取消/重试和连通性测试，Provider 可支持 `mock`、`dify`、直接模型 API 或其他 AI 平台。
- 合同系统负责任务状态、超时重试、幂等、合同版本绑定、Schema 校验、结果落库、人工确认、权限和审计；AI 平台不负责合同状态、审批、签署或最终业务数据确认。
- 商用选型必须同时评估私有化部署、敏感合同数据隔离、模型和工作流版本、可观测性、成本限额、故障降级、供应商锁定和人工兜底。
- 当前建议先用 Mock 完成业务闭环，再以 Dify 私有化接入验证效果；真实接入前需完成 AI 结果 Schema、脱敏/留存策略、幂等键、评测集和上线验收标准。

## 合同中心与文件存储术语收口（2026-08-27）

- “合同中心”定义为合同系统的一级业务导航，包含工作台、合同台账、履行与收付款，以及合同详情下的创建、AI、审批、签署、变更和附件操作。
- “合同中心区”定义为公司统一文件服务为合同系统分配的专属业务存储空间，保存原始文件、正文、签署文件、扫描件、附件、履行佐证和导出文件。
- 合同中心是页面/业务能力，合同中心区是文件服务中的存储空间，两者不能混用；合同中心区不是合同系统菜单，也不是本系统自建文件服务器。
- 合同系统只维护合同与附件关联、版本、权限和审计；文件服务负责物理存储、预览、下载和短期访问链接，数据库不保存文件二进制、物理绝对路径或长期公开 URL。
- 文件集成闭环为“申请上传 → 文件服务上传 → 完成确认返回 file_id/哈希/大小 → 创建附件关联”；预览和下载必须先做合同权限校验，再申请短期链接。
- 待与文件服务确认：业务空间/租户标识、对象命名、保留归档、病毒扫描、格式大小限制、备份恢复、环境隔离和幂等契约。

## 开发启动顺序（2026-08-27）

- 当前不直接先开发前端，也不先单独搭建 AI 专家体；项目仍需先形成可商用的开发基线。
- 第一阶段：以现行需求、思维导图和 `数据库表\合同管理.docx` 当前版本建立一期开发基线；不按历史增量方案重建数据库。
- 第二阶段：定义外部接口契约，覆盖 HR/组织、外部交易对方、我方主体、文件服务、审批系统和 AI Provider；接口必须包含鉴权、幂等、超时、重试、错误码、状态映射和审计要求，开发阶段提供 Mock。
- 第三阶段：确定技术栈并创建工程骨架，先实现合同创建、草稿保存、台账、详情、状态流转和审计日志的后端纵向闭环。
- 第四阶段：前端基于稳定的业务接口实现工作台、合同台账、详情和创建流程；页面不能直接依赖外部服务返回结构。
- AI 专家体已有业务规则设计，不需要从零重新定义；待 AI Provider 契约、解析/审查 Schema 和评测用例确定后，与主业务 Mock 闭环并行配置 Dify，真实接入前完成安全、成本、效果和故障验收。
- 当前最先应产出：一期范围清单、最终数据库基线、外部接口契约、状态/权限矩阵、Mock 方案和测试用例。

## 开发实施规划已建立（2026-08-27）

- 一期、二期、三期产品范围已经确定，不再重新拆分；新增的“步骤一至步骤六”只是一期内部的工程实施顺序。
- 已新增《开发实施规划.md》，统一规划数据库和工程基线、合同核心后端闭环、文件/审批 Mock、AI Provider Mock、Dify 接入、前端页面、商用加固和联调验收。
- 一期第一条可验收主线为：创建合同 → 保存草稿 → 合同台账 → 合同详情 → 正文版本 → 状态流转 → 操作审计。
- AI 专家体已有业务规则，不先单独建设；先冻结 Schema 和 Provider 契约，用 Mock 验证任务、人工确认、版本绑定和异常机制，再接 Dify 私有化。
- 当前执行入口为《开发实施规划.md》第 10 节“当前唯一开发入口”；第 8 节仅保留历史执行顺序说明。

## 外部系统集成边界方案已建立（2026-08-27）

- 外部系统集成边界与 Mock 方案已合并至《开发实施规划.md》；原独立方案已移入 `归档/设计过程/`，仅用于追溯。
- 外部接口不由本项目凭空定稿；本项目先定义业务能力、内部模型、适配层、Mock 行为和外部系统待提供清单。
- 方案覆盖 HR/组织、外部交易对方、我方主体、文件服务、审批系统和 AI Provider 六类依赖，包含鉴权、幂等、超时、重试、回调、状态映射、审计和故障补偿要求。
- 文件服务明确负责合同中心区的物理文件；审批系统负责流程；HR/组织和外部交易对方系统负责各自主数据；我方主体系统负责我方主体主数据；AI Provider 负责模型和工作流执行，合同系统保留任务、版本、人工确认和业务审计。
- 一期先用 Mock 开发，不等待外部系统完成；外部系统正式资料到位后只替换 Adapter 并进行联调。
- 术语修正：客户与供应商是不同的合同业务身份，但目前不能假定它们对应两个独立系统；统一称为“外部交易对方主数据系统”，实际是一套还是多套系统待公司确认。HR/组织、外部交易对方、我方主体、文件服务、审批系统和 AI Provider 分别确认责任和接口。
- 通知、电子签署、归档借阅属于后续能力；一期不把它们作为生产依赖，但保留扩展位置。

## 技术架构方案已建立（2026-08-27）

- 一期技术架构与工程结构方案已合并至《开发实施规划.md》；原独立方案已移入 `归档/设计过程/`。当前规则仍是前端原型、后端业务和 Mock 适配并行，按业务闭环联调。
- 后端按合同核心、正文版本、合同类型、模板、交易对方引用、附件、AI 任务、审批、履行收付款、权限审计拆分；外部依赖统一通过 Adapter/Provider 接入。
- 前端按工作台、台账、创建、详情、审批工作台和系统配置组织，交易对方统一为选择与引用能力，不拆客户/供应商页面。
- 第一条开发切片仍为“创建合同 → 保存草稿 → 台账 → 详情 → 编辑 → 审计”，前后端并行实现并联调。
- 技术栈优先复用 HR 主系统的前后端、认证、权限、数据库、消息和部署标准；只有主系统没有对应标准时才采用规划中的默认技术栈。

## 技术栈已确认（2026-08-27）

- 用户确认采用 Java 17、Spring Boot 3.x、Spring MVC、MyBatis-Plus、Redis、公司标准消息/任务组件、Flyway、OpenAPI、Vue 3 + TypeScript；本地开发数据库改用 SQLite，云 MySQL 仅作为后期目标，等待客户提供具体环境后再迁移。
- 一期采用模块化单体，不先拆微服务；AI、文件和外部任务通过 Adapter/Provider 与异步任务机制隔离。
- 技术栈与工程初始化方案已合并至《开发实施规划.md》；原独立方案已移入 `归档/设计过程/`，当前以规划文件中的统一技术和工程章节为准。
- 技术栈已确认不等于公司基础设施细节已确认；云数据库厂商、认证、消息队列、配置中心、监控和部署标准仍按公司环境接入，但不阻塞方案设计。

## 工程骨架已创建（2026-08-27）

- 已创建 `backend` Java 17 + Spring Boot 3.x 单 Maven 工程，按 API、application、domain、infrastructure、integration、job 包边界初始化。
- 已创建 `frontend` Vue 3 + TypeScript + Vite 工程，包含合同中心工作台占位页面、响应式基础样式和 API 代理配置。
- 已创建后端启动类、基础配置、测试入口和前端构建配置，尚未写合同业务逻辑。
- 基础编译暂未完成：本机 Maven 全局配置将本地仓库指向不可写的 `C:\.m2\repository`，需要调整 Maven 本地仓库或公司构建环境后重试；前端依赖尚未安装，未执行前端构建。

## 协作与文档同步规则（2026-08-27）

- 每次开始实质工作前，先向用户说明目标、原因、修改范围、预期产出和暂不处理的内容，得到确认后再执行。
- 每次形成新的业务决策、技术选型、页面规则、接口约定或待确认事项，及时同步到对应项目文档，不能只保留在对话中。
- 方案设计阶段不直接写代码；代码开发前先确认方案、页面信息架构和接口边界。
- 页面设计必须控制职责边界和信息重复：工作台做概览，台账做查询，详情做单合同业务操作，系统管理做配置，交易对方只做引用。
- 文档同步后要检查主需求、思维导图、技术规划、Wiki 和 Memory 是否出现相互矛盾的旧表述。
- 设计硬原则：局部变化影响尽可能局部化，通过稳定 API、领域边界和统一模型隔离变化；同一信息或能力只保留一个负责位置，其他页面只引用、摘要或提供入口，不做重复页面和重复数据。

## 第一业务切片方案已建立（2026-08-27）

- 第一业务切片的页面信息架构与接口设计已合并至《开发实施规划.md》；原独立方案已移入 `归档/设计过程/`。
- 第一切片闭环为：创建合同 → 保存草稿 → 台账查询 → 详情查看 → 再次编辑 → 操作审计。
- 页面职责确定：工作台做概览，台账做查询，创建页做草稿，详情做单合同业务上下文；交易对方只做统一选择和引用，不建设客户/供应商独立页面。
- 类型配置通过动态 `form-schema` 影响创建页；交易对方通过统一 API/Adapter 隔离外部来源；台账和详情使用稳定摘要/详情接口，减少局部变化影响。
- 本次只完成设计，不新增业务代码；下一步待用户确认后再实现第一切片 API、Mock、后端业务和前端页面。

## 一期整体设计总览已建立（2026-08-27）

- 用户要求不再一次只设计一个零散点，改为先完成一期整体设计，再按整体蓝图实施。
- 一期整体设计总览的有效内容已合并至《开发实施规划.md》；原独立方案已移入 `归档/设计过程/`，仅用于追溯。规划文件统一覆盖产品边界、页面职责、合同模型、状态机、端到端流程、数据库唯一依据、外部系统、AI 专家体、知识库、技术架构、权限安全、测试和实施路线。
- 名称已明确：产品是“通用合同管理系统”；“合同 AI 专家体”是其中的 AI 子能力，不是整个项目名称；Dify 只是 AI Provider 的一期候选实现。
- 硬原则再次确认：局部变化影响尽可能局部化；同一信息和能力只保留一个负责位置，页面不重复建设。
- 当前开发入口改为整体设计确认后的数据库迁移基线、第一切片 API/领域模型、交易对方/权限/审计 Mock 和创建草稿台账详情闭环。

## 当前数据库 DOCX 核对完成（2026-08-27）

- 已读取 `D:\project\hr-contract\数据库表\合同管理.docx` 当前内容，识别出 40 张表：32 张 `t_` 合同业务表、8 张 `sys_` 公共/审批表。
- 旧总结中的 13 张、约 28 张和历史新增表数量全部失效；当前不再从任何 Markdown 推断数据库结构。
- 当前数据库权威文档核对结果已纳入《开发实施规划.md》的数据库基线章节；原核对报告已移入 `归档/设计过程/`，仅用于追溯。
- DOCX 核对发现的两个字段口径已纳入一期基线：`party_source_type` 只表示来源系统，`party_role` 只表示签约角色，新增/补充 `business_relation_type` 表示客户、供应商、合作方、员工等合同关系；合同 `source_type` 统一为模板起草、文件导入、手工录入、外部系统同步。
- Flyway 基线按上述统一口径和当前 DOCX 生成；原 DOCX 保留用户维护，需同步的字段差异在迁移说明中记录，不再作为开发阻塞项。

## 一期开发基线一次性定稿（2026-08-27）

- 一期启用六类高频合同：劳动合同、劳务协议、顾问协议、采购合同、销售合同、服务合同；每类先提供“通用”子类型，其他类型仅保留配置能力。
- 合同来源统一为模板起草、文件导入、手工录入、外部系统同步。
- `party_source_type` 只表示来源系统，`party_role` 只表示签约角色，客户/供应商/合作方/员工通过独立的 `business_relation_type` 表示合同关系；若当前 DOCX 缺少该字段，迁移基线补充并同步 DOCX。
- HR/组织、交易对方、我方主体、文件、审批和 AI Provider 全部采用 Adapter；一期使用 Mock，正式外部接口到位后只替换 Adapter。
- 一期开发顺序定稿为：数据库与工程基线 → 合同核心后端 → Mock 适配 → 前端闭环 → 商用加固 → 外部联调；不再逐项确认产品范围。
- 数据库环境已纠正：本地第一切片使用 SQLite；后期目标数据库由客户确定后再生成迁移脚本，不预设云厂商、地址、账号或版本。

## 一期完整设计收口（2026-08-27）

- 《开发实施规划.md》第 11 章已一次性补齐一期开发设计：业务模块、六类启用合同、字段关系、状态机、内部 API、页面职责、外部 Adapter/Mock、AI 专家体边界、权限安全、测试和完成定义。
- 设计阶段结束，后续不再新增拆分方案文件或重复确认一期范围；实现以《开发实施规划.md》和当前 `D:\project\hr-contract\数据库表\合同管理.docx` 为准。
- 下一步直接执行数据库迁移基线和第一切片代码，前后端围绕“创建草稿→台账→详情→编辑→审计”并行开发；外部正式接口仍以 Mock 替代，不阻塞开发。

## 第一切片已可运行（2026-08-27）

- 后端已实现合同类型、交易对方 Mock、创建草稿、台账、详情、草稿编辑和操作审计 API。
- 前端已实现工作台、创建草稿、合同台账和合同详情闭环，前端直接消费合同系统 API。
- 本地数据已从内存改为 SQLite，数据库文件为 `backend/data/contract-system.db`；启动时自动创建第一切片所需的本地表。
- 后端测试通过，前端构建通过；下一步由用户重启服务验证新建合同在后端重启后仍保留，再进入正文/文件 Mock。

## 正文与文件批次开发中（2026-08-27）

- 已增加 SQLite 正文版本表和文件附件表，支持正文版本序号、哈希、当前版本、附件元数据和本地文件 Mock 存储。
- 已增加正文版本查询/保存接口和 multipart 附件上传接口，合同详情返回版本历史和附件列表；前端接口地址保持不变。
- 前端构建已通过；后端代码验证当前受本机 Maven 缓存中的 `snakeyaml-2.2.jar` 编译异常影响，需修复该单个缓存包后重新执行 Maven 测试。
- 正文与文件批次前端入口已补齐：合同详情可编辑并保存正文新版本，可上传本地文件 Mock，并展示版本历史和附件元数据；前端构建已再次通过。

## AI 与审批批次已完成（2026-08-27）

- 已按需求思维导图和《AI专家体需求设计.md》实现合同系统侧 AI Mock：合同解析任务、法律风险审查、风险明细、正文版本绑定、任务状态和人工确认。
- 法律审查 Mock 输出风险等级、七类审查中的风险明细、原文依据和修改建议；AI 不改变合同状态，不替代审批或签署。
- 已实现审批 Mock：提交审批、查询审批、Mock 审批通过；审批通过后合同状态进入“待签署”，审批状态与合同业务状态分开。
- 合同详情已增加“智能与审批”区域，可直接发起解析/审查、确认风险、提交审批和模拟通过审批；前端构建通过，后端 Maven 测试通过。
- 本批未操作 Dify；后续正式接入只实现/替换 AiProvider，不改变页面和业务接口。

## 开发协作方式调整（2026-08-27）

- 用户明确要求项目以开发为主，相关能力按业务批次一次性完成，不再每轮拆出一个孤立设计点反复确认。
- 后续批次先整体说明目标、原因、改动文件、验证步骤和不包含内容，然后直接实现、测试和交付；只有产品范围、权威数据库结构或外部不可逆操作才单独暂停确认。
- 下一批合并实现：正文版本、文件附件 Mock、原始文件导入和正文/附件关联；完成后再整体实现审批与 AI 批次。

## 文档事实审计（2026-08-27）

- 发现并修正多处过期数据库口径：旧版表数量、旧增量表清单、已归档 Markdown 引用，以及“后续合并增量方案”的开发步骤。
- 当前数据库事实只有一个：`D:\project\hr-contract\数据库表\合同管理.docx` 由用户持续维护，表数量、字段、索引、约束和版本均以该文件当前内容为准。
- 已检查并同步 `CLAUDE.md`、`合同管理系统需求设计.md`、`合同管理系统需求思维导图.md`、`Wiki.md`、`Memory.md`、`开发实施规划.md` 和 `数据库设计增量方案.md`；独立技术方案已归档，不再作为当前入口。
- `数据库设计增量方案.md` 已明确降级为历史参考；归档目录文件继续保留，但不作为开发依据。
- 业务模型进一步明确：在合同系统内，客户、供应商、合作方等统一归为“交易对方”；客户表示对方购买我方产品/服务，供应商表示对方向我方提供产品/服务，合作方表示合作关系。它们不是合同系统内的独立主体模块，而是同一交易对方在具体合同中的关系角色；接口统一按交易对方设计。
## 当前执行记录（2026-08-28）

- 当前有效项目总结：通用合同管理系统，后端 Java 17/Spring Boot，前端 Vue 3/Vite，本地 SQLite；后端 19090，前端 5173。
- 两个 Dify 工作流由用户已完成；真实接入仍需用户提供非敏感的 API 基础地址、工作流输入字段和脱敏输出样例，API Key 不通过聊天传递。
- 已在后端增加 `AiProvider`、Mock Provider、Dify Provider 配置骨架。默认 `CONTRACT_AI_PROVIDER=mock`，前端不配置 Dify，真实 Key 只由后端进程读取环境变量。
- 当前 Dify Provider 尚未绑定具体工作流字段映射，不能声称真实 Dify 已接通；收到接口契约后只替换 Provider 映射，保持前端 `/api/contracts/{id}/ai/*` 不变。
- 合同模板仍是下一项业务开发内容：管理位于系统管理，使用位于合同创建，发布模板版本渲染为正文版本；模板不是 Dify 工作流。

## 完整业务批次执行结果（2026-08-28）

- 模板闭环已实现：模板创建、合同类型绑定、模板版本表、发布状态、基础变量提取、已发布模板渲染正文。
- 合同创建已支持 `templateId`；后端创建前校验模板已发布且合同类型匹配，创建后自动保存第一版合同正文；不选模板仍可手工创建。
- 前端已增加合同模板配置入口、模板创建/发布操作和新建合同时的已发布模板选择。
- 合同生命周期已增加后端状态机接口：草稿/待签署可作废，待签署可进入履行中，履行中可终止；所有状态变化写入合同操作日志。
- 本批验证：后端 Maven 测试通过，前端 `npm.cmd run build` 通过。Maven 仍有本机 `snakeyaml-2.2.jar` 权限警告，但不影响构建结果。
- 本批明确未完成：模板 Word/DOCX 导入与复杂排版、模板变量字段字典、真实 Dify 字段映射、正式审批接口、40 张权威 DOCX 数据库迁移基线。

## 下一批开发入口

- 下一批按完整业务域实现“合同履行与外部集成”：履行节点和收付款计划、合同到期/逾期提醒、交易对方查询联动、外部审批 Adapter/Mock 完善、AI 任务幂等与结果结构化保存。
- 继续保持前端不接触 Dify Key，外部系统通过后端 Adapter；正式数据库仍等待目标云数据库信息，不擅自改为 MySQL。

## 履行管理批次执行结果（2026-08-28）

- 已增加本地履行原型表：收付款计划、履行节点；表由后端启动自动创建，当前仍是 SQLite 原型，不代表已按权威 DOCX 生成正式迁移。
- 已增加接口：合同付款计划查询/新增/标记完成，履行节点查询/新增/完成；未到期、逾期、已完成状态由后端计算/保存。
- 履行操作限定为“履行中”合同，草稿、待签署、已作废和已终止不能维护，避免业务状态与履行数据脱节。
- 前端合同详情已增加履行管理区，集中维护付款与节点；没有增加重复的独立入口。合同状态按钮和履行数据可以在同一详情流程中操作。
- 本批验证：后端 Maven 测试通过，前端 `npm.cmd run build` 通过。
- 当前未完成：工作台真实履行统计、提醒任务、外部审批正式 Adapter、AI 结构化结果和真实 Dify 字段映射；下一批优先处理 AI 任务契约与外部审批 Mock 完整化。
## 履行批次口径修正（2026-08-28）

- 上一条“下一批开发入口”中把工作台履行统计和提醒写在待办项内；本批实际完成的是合同详情履行管理、状态约束和逾期计算，工作台汇总卡片与提醒任务尚未完成。
- 下一批优先实现工作台履行汇总和提醒，再进入 AI 任务结构化与外部审批 Adapter。

## 前端产品化整理（2026-08-28）

- 已重整 `frontend/src/App.vue` 和 `frontend/src/styles.css`：移除历史编码损坏文本和超长模板结构，统一为中文合同中心工作台、合同台账、模板、合同创建和合同详情五个清晰视图。
- 保留既有业务接口和闭环：创建、模板起草、正文版本、附件、AI 解析/审查、审批 Mock、生命周期、履行计划和履行节点；前端构建通过。
- 当前界面是可演示的一期应用壳，尚未达到商用交付标准。后续商用优先项为：账号/权限、字段脱敏、正式审批 Adapter、真实 AI Provider 契约、正式数据库迁移、文件服务、日志监控和部署容器化。

## 商用基础能力批次执行结果（2026-08-28）

- 后端新增本地角色权限边界：`ADMIN`、`LEGAL`、`BUSINESS`、`VIEWER`；通过 `X-Role` 请求头切换测试角色，默认角色为 `ADMIN`，不影响本地开发。
- 权限已落到后端合同操作入口：合同编辑/正文/附件/AI 发起需要编辑权限，模板维护需要模板权限，审批和审查确认需要审批权限，履行计划需要履行权限；前端隐藏只是体验层，后端拒绝才是最终边界。
- 新增 `GET /api/me` 返回当前本地操作人和权限；前端请求统一携带 `X-Role`，侧栏显示当前角色。
- 新增统一 API 错误响应，业务错误返回 `timestamp/status/message`，未知异常不暴露堆栈和数据库细节。
- 本批验证：后端 Maven 测试通过，前端 `npm.cmd run build` 通过。
- 仍未完成正式登录/SSO、用户中心 Adapter、数据范围权限、字段脱敏和审计查询页面；这些依赖客户身份系统和正式安全要求，下一批继续实现本地可替换 Adapter。

## 产品大框架实际落地（2026-08-28）

- 已按最终思维导图的导航原则调整前端框架：三个一级导航为“合同中心、合同统计、系统管理”。
- 合同中心包含工作台、合同台账、履行与收付款；合同统计包含统计分析；系统管理包含合同模板、合同分类、外部连接、权限与审计。
- 合同模板不再作为合同中心一级菜单，交易对方、AI、审批、签署、履行和变更不拆成独立一级导航；这些能力分别在创建、详情、履行台账和系统管理中承载。
- 已为履行台账、统计分析、合同分类、外部连接、权限与审计实际创建可点击页面骨架，并明确标记尚未接入的功能，不把空页面伪装成完成能力。
- 这次修正了之前“先做局部切片导致看起来不像完整项目”的问题；后续开发先按该页面框架挂载业务能力，再扩展具体接口。

## 正式导航壳修正（2026-08-28）

- 侧栏已落地为三个始终可见的一级分组：合同中心、合同统计、系统管理；每个一级分组下面的二级菜单可独立展开和折叠。
- 当前二级菜单具有当前页高亮、当前分组自动展开和 `localStorage` 展开状态记忆；移动端保留分组层级并支持横向浏览子菜单。
- 履行与收付款、统计分析、合同分类、外部连接、权限与审计页面已归回主内容 `<main>`，不再位于应用壳之外。
- 导航使用项目自有 Vue 模板和 CSS，不复制第三方 UI 库代码、商标、图标或页面视觉；参考的是通用后台信息架构和无障碍属性（如 `aria-expanded`）。
- 本批只调整前端应用壳和导航，不新增后端接口、不改数据库、不宣称骨架页已经具备完整业务能力；前端 `npm.cmd run build` 已通过。

## 企业后台视觉规范修正（2026-08-28）

- 根据需求思维导图和实施规划重新收敛应用壳：合同是唯一业务中心，工作台负责概览，台账负责查询，详情负责单合同处理，系统管理负责配置。
- 前端视觉改为中性浅灰工作区、白色侧栏与顶部栏、深灰正文、蓝色主操作和选中态；橙色只用于提醒和风险语义，取消原有偏重的蓝绿色加橙色主配色。
- 统一调整侧栏、导航、标题、面板、表格、表单、状态标签、按钮、间距和移动端布局；不使用渐变、装饰性图形或第三方产品的代码、商标、截图、图标和 CSS。
- 本批只调整表现层，不补写尚未实现的合同大类选择器、合同类型 Tab、统计指标或系统配置接口；这些仍按需求范围作为后续业务开发项。
- 前端 `npm.cmd run build` 通过，开发服务 `http://127.0.0.1:5173/` 返回 HTTP 200。

## 需求基线与变更确认规则（2026-08-28）

- 后续开发必须以《合同管理系统需求设计.md》《合同管理系统需求思维导图.md》《开发实施规划.md》和用户维护的 `D:\project\hr-contract\数据库表\合同管理.docx` 为当前基线；不同文档有冲突时先识别并报告，不自行选择口径。
- 可以提出新的产品、交互或技术想法，但必须先说明原基线、建议变更、变更原因、影响范围、风险和验证方式；未经用户确认，不改变菜单层级、业务职责、状态机、接口契约、数据库口径或外部系统边界。
- 普通实现任务按已确认方案直接开发，不反复拆成无关的小确认；只有涉及需求变更、权威数据库结构、外部正式接口、权限安全或不可逆操作时暂停等待确认。
- 每次实质开发开始前说明本批目标、原因、修改文件、验证方法和明确不处理的内容；完成后同步代码、验证结果和项目文档。

## Current Implementation Status (2026-08-31)

- The project is now implementing complete navigation modules. Initial versions completed: login, workspace, contract ledger, fulfillment and payments, category configuration, template configuration, and external connection configuration.
- The next complete navigation module is Permissions and Audit. It will include role permissions, data scopes, sensitive fields, and global audit queries.
- `项目开发进度与变更记录.md` is the detailed delivery log for actual changes, APIs, verification, and known gaps.

## Confirmed Decisions

- A complete module is defined by the left navigation. Detail-page actions are not separate navigation modules.
- The DOCX database design remains authoritative. Mock tables are transitional compatibility only and will be replaced incrementally.
- First-phase data scopes: all data, organization, department, self and participants, named contracts, and sensitive-field visibility. Organization data remains simulated until the HR adapter is connected.

## Next Step

Implement the complete Permissions and Audit navigation module and unify the existing AccessControl checks.

## Statistics Analysis Delivered (2026-08-31)

- Completed the Statistics Analysis navigation page with contract lifecycle, fulfillment/payment, and AI risk overview sections.
- Added \`GET /api/statistics/overview\`; it requires VIEW permission and applies configured contract-type scope for non-admin roles.
- Lifecycle and payment results support drill-down to the contract ledger or fulfillment workspace.
- Contract amount statistics remain explicitly unavailable until the formal contract amount fields are read from the authoritative DOCX schema; payment-plan totals use existing plan data only.
- Frontend and backend builds pass. A running old backend instance returned HTTP 500 before restart, so endpoint runtime verification remains pending after restart.

## Next Step

The next complete navigation module is not a new menu page. First reconcile formal contract-main fields from the DOCX and switch ledger/detail/statistics reads from transitional Mock tables to the formal schema, then implement the remaining contract-detail capabilities and real HR/SSO integration.

## Application Shell Refresh (2026-08-31)

- The application shell now uses a light, grouped, collapsible sidebar for the confirmed navigation hierarchy and a full-width main working area.
- The current page is distinguished by a restrained blue state and left marker. Desktop and mobile layout rules are covered together.
- This is a presentation-layer change only; no navigation ownership, database schema, API contract, or business workflow was changed.

## Navigation Correction (2026-08-31)

- The first application-shell refresh did not sufficiently change the sidebar hierarchy in the running screen.
- The sidebar is now treated as a complete system-navigation surface: grouped entries, per-entry glyphs, current-page marker, compact vertical density, and a fixed user area.
- The workspace hero is reduced to a compact action strip so the contract overview and attention queue lead the first screen.

## Global Shell Block Delivered (2026-08-31)

- Implemented the first complete visual block agreed from the three references: dark system brand band on the right, independent light navigation rail on the left, grouped collapsible navigation, and full-width work canvas.
- This block changes only the application shell; the workbench business arrangement remains the next independent block.

## Home Page Decision Reserved (2026-08-31)

- An independent system home page is reserved as a possible future entry for cross-business summaries.
- It is not enabled or added to the navigation yet because the system currently has one primary business domain: contracts.
- When real contract, approval, fulfillment, AI, and organization data are connected, reassess whether a system-level home page provides meaningful cross-module value.
- If enabled later, the home page will be a separate top-level entry; the Contract Workbench will remain under Contract Center.

## Final Navigation Presentation (2026-08-31)

- Contract Center, Contract Statistics, and System Management are first-level expandable navigation nodes, not gray section labels.
- Each first-level node has an icon and a chevron. Children retain lighter icons, indentation, and selected-page state.
- The current menu content and Workbench ownership remain unchanged.

## Workbench Layout Block Delivered (2026-08-31)

- The Workbench now follows an operational sequence: summary metrics, contract-state handling area, right-side attention queue, then recently updated contracts.
- Removed the duplicate lifecycle promotion banner and standalone quick-action strip from the Workbench.
- Real status counts are retained. No fabricated contract amount, due-date, or AI values are added.

## Next Step

Rebuild the complete Contract Ledger and Fulfillment workspace block: query filters, full-width work tables, batch actions, and direct processing entry points.

## Ledger And Fulfillment Layout Block Delivered (2026-08-31)

- Contract Ledger is structured as category/type context, filter toolbar, batch actions, and a full-width work table.
- Fulfillment uses a master-detail layout: active-contract selection on the left and contract summary, milestones, and payment-plan actions on the right.
- Existing query, export, selection, payment, and milestone operations remain intact.

## Next Step

Rebuild the complete Create Contract and Contract Detail block: guided creation steps, detail summary, task-oriented tabs, and lifecycle actions.

## New Contract And Detail Delivery (2026-08-31)

- New contract registration is a three-step workflow: choose the source, enter existing authoritative fields, and review before saving a draft.
- No database, API, or permission contract changed. The flow uses only `typeId`, `partyId`, `name`, `remark`, and `templateId`.
- Contract detail keeps the existing lifecycle operations, organized into overview/audit, content/attachments/AI/approval, and fulfillment/payment task tabs.
- Legacy inline ledger, fulfillment, and template views are excluded when their dedicated workspace is active, preventing duplicate screens and controls.

## Formal Contract Fields First Phase (2026-08-31)

- DOCX authority was read and confirmed for the contract main table: total amount, currency, payment direction, sign/effective/expiry dates, source type, and applicant organization fields.
- The compatibility schema now carries the main formal fields without deleting legacy mock tables. Existing data remains available during migration.
- Contract detail exposes the formal field group and explicitly shows unavailable values as `未设置`; no fabricated amount, date, or organization data is shown.
- Real HR/organization scope filtering and editable formal-field input remain a separate follow-up because their source systems and permissions are not yet connected.

## Contract Data End-to-End Scope Block (2026-08-31)

- Formal draft fields now flow through create/update requests: amount, currency, payment direction, and expiry date, with validation and CNY fallback.
- Contract lists expose amount/currency and expiry date while preserving explicit unavailable values.
- The same role scope plus contract-type intersection is applied to list, detail, fulfillment, and statistics queries. Mock user/org/department headers provide a replaceable adapter boundary.

## Identity And Scope Acceptance Block (2026-08-31)

- Added a complete mock identity acceptance workspace with administrator, business, and legal users plus organization/department context.
- All frontend requests now carry the selected mock identity; fulfillment and statistics no longer use role-only context.
- The directory endpoint is the replacement boundary for future HR/SSO integration. Business pages and scope rules remain unchanged.

## Contract Authorization And Overreach Acceptance (2026-08-31)

- Completed contract-level authorization as part of Permissions and Audit: create/list/revoke, operation permissions, expiry, and status.
- Effective authorization supplements role scope for a named contract; it does not grant system administration.
- Detail, content, attachments, AI, approval, payment, and fulfillment access now use a unified action-level authorization check.

## SQLite Development Acceptance (2026-08-31)

## MySQL DEV Baseline And Compatibility Cutover (2026-09-01)

- The DOCX-aligned MySQL baseline has been applied to DEV RDS: 40 tables, including 32 `t_contract_*` tables and 8 `sys_*` tables.
- The `mysql` profile reads its password only from `DB_PASSWORD`; no secret is committed. `backend/scripts/run-mysql-dev.ps1` is the launch helper.
- `MySqlCompatibilitySchema` replaces SQLite-specific startup DDL under MySQL. Core contract create/update data is mirrored into formal `t_contract_main` and `t_contract_party` by MySQL triggers.
- API creation and a direct read-only query verified the mirror. Remaining business APIs still require staged formal-table cutover.

## AI Expert Workflow Acceptance (2026-09-01)

- Verified against the MySQL DEV runtime: content version save, AI extraction, legal review, task/report query, manual review confirmation, approval submission, and mock approval all passed.
- `AiWorkflowService` now calls the `AiProvider` boundary before persisting its workflow result. This keeps the current Mock acceptance path and preserves a single replacement point for Dify.
- Fixed MySQL approval submission by using `ON DUPLICATE KEY UPDATE` in the MySQL path; SQLite retains its `ON CONFLICT` implementation.
- Dify remains intentionally unconfigured until workflow IDs and exact request/response field mappings are supplied. It must not be presented as a tested live AI service.

## Dify Dual Expert Integration (2026-09-01)

- Confirmed actual Dify modes: contract extraction is `advanced-chat`; legal review is `workflow`.
- Extraction uses `/chat-messages`; legal review uses `/workflows/run`; both use streaming mode to avoid long blocking gateway timeouts.
- Dify base URL and keys are environment-only. Real DEV verification passed: extraction returned `extract-v1` JSON; legal review returned `review-v1` with 9 structured risks.
- Raw provider responses are persisted. Extraction fields are not automatically written into the contract until overwrite and confirmation rules are agreed.

- Formal database migration is paused by product decision. SQLite remains the only active development datastore until database connection information is supplied.
- Added repeatable SQLite backup and restore scripts plus a development runbook.

## SQLite Commercial Regression Block (2026-08-31)

- Added a repeatable smoke script and acceptance checklist for the complete local contract lifecycle and authorization boundaries.
- Formal migration remains paused; this block validates only the SQLite-compatible implementation.

## Permissions And Audit Delivered (2026-08-31)

- The Permissions and Audit navigation module now provides role permission configuration, first-phase data scopes, contract-type permissions, sensitive-field access, and global audit queries.
- The next navigation module is Statistics Analysis. Its scope is contract amount, fulfillment, due-date, and AI risk metrics using unified data-permission filtering.

## SQLite Regression And Integration Contracts (2026-08-31)

- Completed the SQLite regression block with a real HTTP smoke run on port 19091. Creation, amount persistence, content, AI, approval, named-contract view authorization, content view, and edit denial passed.
- Fixed SQLite numeric amount mapping, recursive content lookup while building contract detail, and the error handler so it preserves HTTP error statuses.
- HR/SSO directory, file storage, approval, and AI now have replaceable provider boundaries with local Mock implementations. No real external service, credential, or production database is connected.
- Next complete block: commercial acceptance and deployment preparation after the user provides the target deployment and external interface information.
## AI 解析确认回填与工作区升级（2026-09-01）

- AI 解析结果不直接覆盖人工数据：新增读取最近一次解析结果和显式确认回填流程，前端先展示建议值，用户确认后才写入合同正式字段。
- 当前可确认回填：合同名称、合同金额、币种、收付方向、签署日期、生效日期、到期日期；无法标准化为 `yyyy-MM-dd` 的自然语言日期不强行写入，保留为待确认付款条件。
- 回填接口：`GET /api/contracts/{id}/ai/extract/latest`、`POST /api/contracts/{id}/ai/extract/confirm`；均经过合同级数据权限校验，确认写入记录操作审计。
- 新增前端 `AiExtractionReview.vue`，放在合同详情 AI 区域，包含字段矩阵、置信度/条款/付款计划摘要、付款条件建议和确认写入动作。
- AI 区域采用深色蓝色科技感作为高注意力决策区，主业务页面仍保持浅色、克制、全宽的商用后台布局；已补齐桌面端和移动端响应式规则。
- 本轮未自动回填付款计划和履约项，原因是付款条件常为自然语言，需另设金额、日期、触发条件的人工确认规则。
## AI 付款计划与履行项确认闭环（2026-09-01）

- AI 解析结果新增履行建议接口：`GET /api/contracts/{id}/ai/extract/fulfillment-recommendations`。
- 合同详情 AI 工作区现在支持付款计划、履行节点逐条勾选和修改后统一确认；未勾选项不会写入业务表。
- 后端确认时校验付款计划名称、正数金额、标准日期，以及履行节点名称和标准日期；自然语言日期继续留在 AI 建议中，不生成虚假日期。
- SQLite 兼容表新增 `source_extract_task_id`、`is_ai_confirmed`，同一合同同一解析任务重复确认按期次更新，避免重复插入；确认结果写入合同操作日志。
- MySQL 兼容表同步预留同名字段，正式 `t_contract_payment_plan` / `t_contract_perform_item` 切换仍按正式迁移块处理。
- 新增撤销接口 `POST /api/contracts/{id}/ai/extract/fulfillment-revoke`：仅撤销当前解析任务下仍处于待处理状态的 AI 写入项，保留历史记录并写审计。
## 本地登录与会话权限闭环（2026-09-01）

- 新增 SQLite `local_user` 用户表，开发默认账号为 `admin / admin123`，密码只保存 SHA-256 摘要；企业环境由管理员创建账号，暂不开放公开注册。
- 新增 `POST /api/auth/login`、`GET /api/auth/session`、`POST /api/auth/logout`，使用服务端 HttpOnly Session，前端不再用 localStorage 保存登录身份。
- `AccessControl` 优先读取服务端 Session 身份，兼容请求头仅保留给旧开发联调；正式接入 IAM/SSO 时替换认证适配层。
## 管理员账号与角色维护（2026-09-01）

- 新增管理员账号管理能力：用户列表、启用/停用、角色分配、组织 ID、部门 ID 和密码重置。
- 接口：`GET /api/admin/users`、`PUT /api/admin/users/{id}`、`POST /api/admin/users/{id}/reset-password`，全部要求管理员权限。
- 账号配置变更和密码重置写入系统审计；停用账号不能继续登录。
- 页面 `AdminUserWorkspace.vue` 与“权限与审计”合并，避免系统管理出现孤立入口；公开注册仍关闭。
## 外部身份目录适配准备（2026-09-02）

- 已确认 `DirectoryProvider` 是 HR/IAM 用户、组织和部门的替换边界；Mock 目录数据改为由 Provider 提供。
- 新增 `IdentityDirectoryService` 和 `GET /api/identity-directory/contract`，明确必需字段：用户 ID、登录账号、显示名、启用状态、组织、部门和角色编码。
- 运行规则：合同系统只读身份与组织主数据，不创建或修改 HR/IAM 数据；外部未配置或不可用时显示本地降级，不冒充正式接入。
- 外部连接页新增身份目录状态、数据边界、字段契约和降级说明。
## IAM BFF 与门户 OIDC 双入口集成（2026-09-02）

- 新增 `IamClient`、`IamProperties`、`AuthenticationService`：自有登录页在 `CONTRACT_AUTH_MODE=iam` 时调用 IAM `/api/v1/auth/login/password`，后端保存 IAM Token 于服务端 Session，不落库、不下发前端。
- 新增 OIDC 入口 `/api/auth/oidc/login` 和 `/api/auth/oidc/callback`，采用 state 校验、授权码后端换 Token，回调后复用同一 HttpOnly Session。
- 新增受限 BFF 代理 `/api/bff/proxy`，仅允许 `/api/v1/docs/` 和 `/api/v1/mdm/` 路径，统一注入 `Authorization: Bearer` 与 `X-App-Code`。
- IAM 地址、client、secret、app code、redirect URI 和 OIDC 端点均由环境变量配置；默认 `local` 模式仍可用于开发，未配置 IAM 时明确返回配置错误。
## IAM dev integration status (2026-09-02)
- IAM application created: `hr_contract_management`; client ID: `hr_contract_management-client`.
- The IAM development login endpoint is reachable from this environment. The application remains in development and is not published.
- Do not commit `IAM_CLIENT_SECRET`. Use backend process environment variables; `backend/.env.iam.example` is a non-secret template.
- Self login uses IAM password login. Portal entry uses OIDC authorization code and requires the actual client secret plus confirmed OIDC paths.

## MySQL migration status (2026-09-02)
- Dedicated RDS database `hr_contract_db` is reachable and the contract application starts against it successfully.
- Flyway baselined the existing schema at version `0` and applied `V1__contract_schema.sql` at version `1`.
- SQLite remains a local development fallback; it was not deleted or overwritten.

## Formal-table runtime cutover progress (2026-09-03)
- Added MySQL runtime mirrors for content versions, attachments, payment plans, fulfillment items, AI extraction/review, changes, templates, template versions and approvals in `MySqlCompatibilitySchema`.
- The compatibility tables remain the write API during this staged cutover; triggers materialize each new/updated record into the DOCX-defined `t_contract_*` tables, preserving rollback and SQLite behavior.
- Backend compile passed with the project-local Maven settings. A MySQL start attempt reached the RDS but had no `DB_PASSWORD`; RDS full-chain acceptance is still not claimed.
- Remaining reads are now switched (2026-09-03): authorization uses `auth_target_type/auth_target_id` (V1-aligned, keeping the `targetUserId` string contract), approval reads `sys_approval_instance`, and statistics reads the `t_contract_*` tables with numeric status/risk/scope mapping.
- `FormalSchemaMigration` (SQLite) now creates `t_contract_ai_extract_task`/`t_contract_ai_review`, runs `migrateAi()`, and installs runtime triggers for payment/milestone/AI/approval so compatibility writes keep materializing into the formal tables; `migrateApprovals()` status CASE was corrected to the real English values.
- Verified via `mvn test` (7 tests) with a new `FormalTableCutoverSmokeTest` (6 tests).
- MySQL full regression closed (2026-09-03): create / detail / content / AI extract / AI review / approval / authorization / authorized view / edit-denied(403) / statistics all pass against RDS.
- Fixed two MySQL 500s caused by ISO-8601 strings written/parsed against MySQL `DATETIME` columns: `ContractAuthorizationService` now writes `yyyy-MM-dd HH:mm:ss` (UTC) via `dbNow()` for `auth_start_time`/`create_time`/`revoked_time` and compares expiry with the same format; `AiWorkflowService.approval()` reads `update_time` via a `parseInstant()` helper that tries `Instant.parse` then falls back to `Timestamp.valueOf().toInstant()`.
- DOCX reconciliation: `t_contract_main` authority has `applicant_id` (合同发起人 ID) + `applicant_org_id` (description 「发起部门 ID」) but **no** `applicant_department_id`. MySQL V1 is correct; SQLite's extra `applicant_department_id` and `StatisticsService`'s DEPARTMENT scope reference a non-authoritative column. No role currently uses the DEPARTMENT scope, so it does not block — next step is to map department scope to `applicant_org_id` and drop the SQLite extra column.

## Dify workflow verification (2026-09-02)
- The actual Dify API is reachable on port `8088`, not the port-80 base URL.
- Contract extraction is a chat application (`/chat-messages`); legal review is a Workflow application (`/workflows/run`).
- The review blocking call reached Dify but returned gateway 504 after about 60 seconds. The extraction streaming call did not finish within 120 seconds. Both need Dify/gateway execution-time diagnosis before being marked as accepted.
- Dify logs subsequently confirmed both requests finished with SUCCESS. Legal review takes about 152 seconds, so blocking calls exceed the gateway limit; retain SSE streaming for the contract system.
- Real response schemas were received. Extraction uses `basic_info` and `payment_plans` with `plan_amount`, `plan_due_date`, and numeric `payment_direction`; review wraps the review payload in `result`.

## Dify timeout and answer-fence hardening (2026-09-02)

- SSE streaming and field mapping were already in place; this block only hardened two edges.
- Dify read timeout raised 180s → 300s (legal review runs ~152s). Stored extraction answer now strips ```json fences before parsing.
- Backend clean compile passed. Real Dify acceptance still needs `DIFY_BASE_URL=http://metric-dify.qd-ecs.com:8088/v1` plus the two API keys in the backend runtime env; keys are never exchanged through chat.

## 页面与导航归类评审（2026-09-03）

- 用户提出多数页面不宜单独成界面、部分模块看似无用，要求参照招聘系统做归类；我此前建议"移除"账号管理/权限审计/数据范围/外部连接，读代码后已收回——它们是合同域特有能力，只归类不删除。
- IAM 账号归属已澄清：正式接入后 BFF（自有登录页）与 OIDC（门户）的账号都存于 IAM，不在合同系统库；自有登录页只是登录 UI，账号密码由 IAM 校验；仅 `CONTRACT_AUTH_MODE=local` 开发模式用 `local_user` 表（admin/admin123）作兜底。
- 对照 BFF/OIDC 两份接入文档核对我们 IAM 代码（IamClient/IamProperties/AuthenticationService）：路径与 Header 配套、字段命名做了驼峰/下划线双兼容，但有 4 个缺口——① IAM roleIds/isSuperAdmin 未映射到合同角色（现一律 VIEWER）② scope 缺 email ③ OIDC userinfo 复用 BFF session/current 端点待确认 ④ PKCE 参数位已留但未启用。
- 提出 7 菜单归类方案（对齐招聘系统"业务成页+配置收拢+账号单列"）：首页/工作台、合同台账、发起合同、履行与变更、AI 审查、基础配置（分类+模板+外部连接）、账号与权限（账号+权限审计+数据范围）。功能全部保留，只收导航入口。
- 待用户确认 7 菜单归类后调整导航/路由；roleIds→合同角色映射是接入 IAM 前的真缺口，需单独设计映射关系。

## 登录与会话/界面可见性修复（2026-09-03）

- 修复 admin/admin123 登录 401：`LocalAuthService.init()` 由「仅空表插入」改为幂等校正——admin 缺失则插入、存在但密码哈希不是 `admin123` 则重置并启用，历史残留旧密码行不再导致登录失败。
- 修复刷新即退出：前端 `api` 基地址由绝对 `http://127.0.0.1:19090` 改为空串，改走 Vite 已配好的 `/api` 代理（同源）。根因是 `localhost` 与 `127.0.0.1` 对浏览器属跨站，JSESSIONID 默认 SameSite=Lax 不被跨站 fetch 回传，会话每次都新建。
- 修复「全是白色/看不出改动」：`styles.css` 两条遗留规则盖掉了设计——`body .app-shell .sidebar{background:#fff !important}`（!important 压过深色导航）和 `workspace-banner{display:none}`（隐藏渐变大横条）。现改为深色 L 形框架（深色侧栏+顶栏）+ 亮色内容 + 恢复靛蓝渐变大横条；这是明显可见的颜色变化，非全深色。
