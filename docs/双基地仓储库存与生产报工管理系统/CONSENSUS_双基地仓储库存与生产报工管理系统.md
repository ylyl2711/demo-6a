# CONSENSUS — 双基地仓储库存与生产报工管理系统（项目规划）

> 6A 阶段：Align（对齐）→ 最终共识
> 版本：v1.0
> 日期：2026-10-20
> 状态：已达成（基于用户 4 项决策）
> 上游：[ALIGNMENT_双基地仓储库存与生产报工管理系统.md](file:///c:/Users/pc/Desktop/demo-6a/docs/双基地仓储库存与生产报工管理系统/ALIGNMENT_双基地仓储库存与生产报工管理系统.md)

---

## 一、需求描述（最终确认）

系统面向宁波汽车软轴软管有限公司宁波、合肥双基地，分 Web 后台（管理员/业务人员）与 H5 移动端（操作工），实现以下业务能力，并形成闭环：

| 模块 | 功能点 | 端 |
|------|--------|----|
| 系统管理 | 用户账号、角色权限、基础字典、操作日志 | Web |
| 仓储管理 | 仓库/物料档案、入库(录批次)、出库(FIFO)、批次双向追溯、呆滞预警、库存盘点、台账导出 | Web |
| 生产工单 | 工单创建(选基地/产品/BOM/操作工)、进度监控、状态机流转 | Web |
| H5 报工 | 登录、我的工单、工单详情(工序要求)、逐工序报工(良品/不良/原因/照片)、提交完工 | H5 |
| 质量管控 | 工单质检审核、不良原因分类统计、质检报告导出 | Web |

**核心业务闭环**：入库(录批次) → 生产领料出库(FIFO，关联工单) → 工单分配 → H5 逐工序报工 → 提交完工 → 质检审核 → 成品入库(成品批次) → 批次追溯/不良统计/报告导出。

---

## 二、4 项关键决策（不确定性已解决）

| 编号 | 问题 | **最终决策** | 影响 |
|------|------|--------------|------|
| Q1 | 仓库/物料是否拆表 | **保持单表** `warehouse_material` | 9 张基础表不变，改动最小 |
| Q2 | BOM/工序如何落地 | **新增 BOM + 工序模板表** | 新增 `material_bom`、`process_template`、`work_order_process` |
| Q3 | 前端技术栈 | **Vue2.7 + Element UI 2.15 + Vant2** | 与设计文档一致，不改前端表述 |
| Q4 | 报工/质检粒度 | **逐工序报工 + 整单质检** | `work_report_check` 一工序一条；质检按工单整单审核 |

### 2.1 最终表结构清单（共 **12 张**）

| # | 表名 | 中文名 | 变化 |
|---|------|--------|------|
| 1 | sys_user | 系统用户表 | 不变 |
| 2 | sys_role | 角色表 | 不变 |
| 3 | sys_dict | 数据字典表 | 不变 |
| 4 | sys_oper_log | 操作日志表 | 不变 |
| 5 | warehouse_material | 仓库物料表（单表） | 不变（Q1） |
| 6 | stock_batch | 物料批次表 | 不变 |
| 7 | stock_bill | 库存单据表 | **新增 `work_order_id`**（生产领料关联工单，闭环 G4） |
| 8 | production_work_order | 生产工单表 | 不变（含成品批次 `product_batch`） |
| 9 | work_report_check | 报工质检表 | 一工单多工序 → 一工序一条记录；质检字段整单共享 |
| 10 | material_bom | **BOM 表**（新增） | 成品→原料/半成品 + 用量 |
| 11 | process_template | **工序模板表**（新增） | 产品→固定工序清单（名称/顺序/要求） |
| 12 | work_order_process | **工单工序表**（新增） | 建单时从模板复制的工单级工序清单 + 每工序状态 |

> 说明：`warehouse_material` 保持单表（Q1），故 `stock_batch`、`stock_bill` 仍关联 `warehouse_material.id`，语义不变。

### 2.2 新增表字段定义

**表10 material_bom（BOM 表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键自增 |
| product_material_id | bigint | 成品物料ID（→warehouse_material.id） |
| component_material_id | bigint | 组成物料ID（原料/半成品） |
| quantity | decimal(10,2) | 单位成品用量 |
| remark | varchar(200) | 备注 |

**表11 process_template（工序模板表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键自增 |
| material_id | bigint | 产品物料ID（→warehouse_material.id） |
| process_seq | int | 工序顺序号 |
| process_name | varchar(100) | 工序名称 |
| requirement | varchar(500) | 工序加工要求 |

**表12 work_order_process（工单工序表）**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键自增 |
| work_order_id | bigint | 工单ID（→production_work_order.id） |
| process_seq | int | 工序顺序号 |
| process_name | varchar(100) | 工序名称 |
| requirement | varchar(500) | 工序加工要求 |
| process_status | tinyint | 工序状态：0未开始，1进行中，2已报工 |

---

## 三、技术实现方案与技术约束

| 分类 | 技术 | 版本 | 约束 |
|------|------|------|------|
| JDK | Java | 1.8 | SpringBoot2.x 原生支持 |
| 后端框架 | SpringBoot | 2.7.x | — |
| ORM | MyBatis-Plus | 3.5.x | 自动 CRUD |
| 数据库 | MySQL | 8.0 | utf8mb4 |
| 连接池 | HikariCP | 默认 | — |
| 认证 | JWT (jjwt) | 0.11.x | 无状态 token |
| 接口文档 | Knife4j | 3.x | `http://localhost:8080/doc.html` |
| 工具库 | Hutool | 5.8.x | 日期/加密 |
| Excel | EasyExcel | 3.x | 台账/统计导出 |
| 前端(Web) | Vue + Element UI | 2.7 + 2.15 | — |
| 前端(H5) | Vue + Vant | 2.7 + 2 | — |
| 路由/状态 | Vue Router 3 / Vuex 3 | — | 配合 Vue2 |
| 图表 | ECharts | 5.x | 不良统计 |
| 构建 | Vite | 2.x | 可用 vite-plugin-vue2 |

### 3.1 集成方案
- **一套后端接口**：Web 与 H5 共用 REST 接口，仅前端路由/菜单按角色区分。
- **权限集成**：JWT + 拦截器 + 自定义 `@RequireRole`；后端强制鉴权（前端菜单仅做展示过滤）。
- **文件集成**：本地存储 `resources/static/uploads/`，返回 `/uploads/xxx.jpg`。
- **统一返回**：`{ code, msg, data }`。
- **环境变量**：数据库/密钥置于 `.env`（不入 git）。

### 3.2 关键业务约束（硬性）
1. 入库必录批次号，生成/更新 `stock_batch`；出库按 FIFO 推荐批次并扣减。
2. 生产领料出库 `stock_bill.work_order_id` 必填 → 打通批次追溯链路。
3. 工单状态机仅在后端 Service 硬编码：`待生产→生产中→待质检→已完成`；不合格 → 退回 `生产中`。
4. 操作日志只 insert，不 update/delete。
5. 提交完工后报工记录锁定，不可修改。
6. 不良数量 >0 必须选择不良原因。

---

## 四、任务边界限制

### 4.1 本次规划任务边界
- ✅ 产出规划文档（ALIGNMENT / CONSENSUS / DESIGN / TASK + Approve 清单）
- ❌ 不编码、不建表、不搭环境、不部署

### 4.2 系统开发边界（供后续 Automate）
- 含：Web 后台、H5 移动端、仓储/工单/报工/质检/统计/权限/日志
- 不含：硬件对接、自动化采集、财务记账、客户销售订单

---

## 五、验收标准（供后续开发验收）

| 阶段 | 验收标准 |
|------|----------|
| 数据库 | 12 张表脚本执行无错；初始化角色/字典/管理员数据可登录 |
| 后端基础 | 三类角色登录成功；Knife4j 文档可访问；接口统一返回 code=200 |
| 仓储 | 出入库→FIFO→批次双向追溯(原料↔成品)→呆滞预警→台账导出 全跑通 |
| 工单 | 建单复制工序清单；分配后 H5 可见；非法状态跳转被拦截 |
| H5 报工 | 操作工登录→看工序→逐工序报工(不良+照片)→提交完工；状态变待质检 |
| 质量 | 质检审核闭环；不良统计图表正确；质检报告可导出 |

### 5.1 最终演示路线
```
管理员登录 → 建业务人员/操作工账号
业务人员登录 → 录原料入库(宁波) → 维护 BOM/工序模板 → 创建工单(分配操作工) → 生产领料出库
操作工 H5登录 → 看工单/工序 → 逐工序报工(良品/不良/照片) → 提交完工
业务人员 Web → 待质检 → 审核合格 → 不良统计饼图 → 导出质检报告
```

---

## 六、质量门控核对

- [x] 需求边界清晰无歧义
- [x] 技术方案与现有架构对齐（延续设计文档四层架构）
- [x] 验收标准具体可测试
- [x] 关键假设已确认（4 项决策）
- [x] 项目特性规范已对齐

> 共识达成，进入 Architect（架构设计）阶段。
