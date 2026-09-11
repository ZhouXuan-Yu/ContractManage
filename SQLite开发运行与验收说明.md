# SQLite开发运行与验收说明

## 范围

当前开发环境仅使用 SQLite：`backend/data/contract-system.db`。正式数据库连接、正式表迁移和生产部署不在本说明范围内，需在获得数据库信息后另行实施。

## 启动

后端：

```powershell
cd backend
mvn.cmd "-Dmaven.repo.local=D:\project\hr-contract\Hrcontract\.m2-local" spring-boot:run
```

前端：

```powershell
cd frontend
npm.cmd run dev
```

默认后端地址为 `http://127.0.0.1:19090`。

## 备份与恢复

停止后端后执行备份：

```powershell
cd backend\scripts
.\backup-sqlite.ps1
```

恢复时必须先停止后端：

```powershell
cd backend\scripts
.\restore-sqlite.ps1 -Backup "..\data\backups\contract-system-YYYYMMDD-HHMMSS.db"
```

脚本输出 SHA-256，用于确认备份或恢复文件完整性。

## 验收范围

- 前端构建：`npm.cmd run build`
- 后端编译：`mvn.cmd "-Dmaven.repo.local=D:\project\hr-contract\Hrcontract\.m2-local" -DskipTests compile`
- 合同创建、草稿编辑、详情、正文、附件、审批、签署、履行、收付款、变更。
- 角色范围、模拟身份、合同级授权和越权拒绝。

外部 HR/SSO、文件服务、审批和 AI 仍为模拟适配，不能作为生产联调验收结论。
