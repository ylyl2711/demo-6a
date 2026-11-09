# ACCEPTANCE — 双基地仓储库存与生产报工管理系统

## 1. 交付范围

| 交付物 | 位置 | 说明 |
| --- | --- | --- |
| 数据库脚本 | `sql/init_table.sql`、`sql/init_data.sql`、`sql/test_data.sql` | 12 张表 + 初始数据 + 演示数据 |
| 后端服务 | `backend/`（Spring Boot 2.7.18） | 端口 8080，包名 `com.project.stockwork` |
| Web 管理端 | `new/`（Vue3 + Ant Design Vue + Vite + TS） | 端口 8088（开发） |
| H5 报工端 | `h5/`（Vue3 + Vant 4 + Vite + TS） | 端口 5174（开发） |
| 6A 文档 | `docs/双基地仓储库存与生产报工管理系统/` | ALIGNMENT / CONSENSUS / DESIGN / TASK / CHECKLIST / ACCEPTANCE / FINAL / TODO |

## 2. 功能验收结果

| 模块 | 验收点 | 结果 | 证据 |
| --- | --- | --- | --- |
| 认证与权限 | 三角色登录、`@RequireRole` 拦截、401/403 | 通过 | manager/op_zhang 登录 200；op_zhang 访问 `/api/warehouse/list` 返回 403 |
| 仓储物料档案 | 列表、筛选、新增/编辑/删除、导出 | 通过 | Web 页面显示 6 条物料；`/api/warehouse/list` total=6 |
| 出入库 | 入库必填批次、出库 FIFO/指定批次、作废反冲 | 通过 | 接口冒烟测试通过；单据台账正常 |
| 库存预警/呆滞 | 下限预警、呆滞批次识别、阈值调整 | 通过 | 预警 2 条（MAT-HF-001 等）；呆滞 2 批次 |
| 库存盘点 | 实盘对比、差异记录 | 通过 | `/api/stock/check` 返回 before/realNum/diff |
| 批次追溯 | 原料批次正向、成品批次反向、导出 | 通过 | `B-GS-20260801` 正向、`PB-20261015-A` 反向均返回数据 |
| 生产工单 | 建单复制工序模板、列表、详情、进度 | 通过 | 4 张工单；详情抽屉显示工序进度（浏览器验证 PASS） |
| BOM/工序模板 | 产品维度维护、整单覆盖保存 | 通过 | `/api/workorder/bom`、`/template` 读写正常 |
| H5 逐工序报工 | 顺序校验、不良原因、照片、覆盖提交 | 通过 | op_zhang 提交工单 4 第 1 道工序成功，状态 0→1，进度 1/3 |
| 提交完工 | 全部工序完成后提交、锁定报工 | 通过 | `/api/report/finish` 逻辑校验，已质检/已完成禁止修改 |
| 工单质检 | 合格→成品入库、不合格→退回重工 | 通过 | `/api/reportCheck/todo` 1 条待质检；审核接口返回 200 |
| 质量统计 | 不良原因聚合、周期/基地筛选、导出 | 通过 | `/api/quality/stat` items：设备问题/人工操作/原材料缺陷 |
| 系统管理 | 用户 CRUD/启停、角色权限、字典、日志 | 通过 | admin 用户列表 total=5；角色权限保存后前端菜单即时刷新 |
| Excel 导出 | 台账、统计、追溯、质检报告、日志 | 通过 | 实测文件：stat 3569B、ledger 4389B、trace 3738B |
| 前端工程 | 类型检查、生产构建 | 通过 | `pnpm typecheck` 无错误；`pnpm build` 268 个产物，11.13s |

## 3. 浏览器端到端验证

- Web 管理端（http://localhost:8088）：登录后 4 个统计卡片渲染，左侧菜单 6 个一级菜单正常；物料档案 6 条数据；生产工单 4 条数据、详情抽屉可打开 —— PASS。
- H5 报工端（http://localhost:5174）：登录页预填 `op_zhang/123456`；登录后工单卡片列表正常；工单详情 3 道工序展示正常；待质检工单正确显示锁定提示 —— PASS。

## 4. 验收过程中发现并修复的问题

1. `work_report_check.process_id` 演示数据指向了其他工单的工序，导致详情页“已报工但无明细”。已按 `work_order_id + process_seq` 修正数据库与 `sql/test_data.sql`。
2. 工单 4 原分配给 `op_wang`，导致 `op_zhang` 无可报工工单。已调整为 `op_zhang`，便于 H5 演示完整流程。
3. 前端菜单过滤逻辑误把叶子节点全部过滤，已修复并复验通过。
4. 后端业务接口原仅允许 `BUSINESS`，与管理员菜单权限不一致；已放开 `ADMIN` 访问业务接口。

## 5. 结论

核心业务闭环（入库 → 建单 → 领料 → H5 逐工序报工 → 完工 → 质检 → 成品入库 → 追溯/统计导出）已实现并通过接口与浏览器验证，达到毕业设计演示交付标准。
