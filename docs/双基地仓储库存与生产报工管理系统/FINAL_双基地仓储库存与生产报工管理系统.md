# FINAL — 双基地仓储库存与生产报工管理系统

## 1. 项目概述

宁波汽车软轴软管有限公司“双基地（宁波 / 合肥）仓储库存与生产报工管理系统”。系统覆盖原料入库、批次管理、FIFO 出库、呆滞预警、生产工单、逐工序报工（含现场照片）、工单质检、成品入库、正反向批次追溯、质量统计与 Excel 导出，以及用户/角色/字典/日志等系统管理能力。

## 2. 技术架构

```
┌────────────────────┐      ┌────────────────────┐
│  Web 管理端         │      │  H5 报工端          │
│ Vue3 + AntdV + Vite│      │ Vue3 + Vant4 + Vite│
│ :8088              │      │ :5174              │
└────────┬───────────┘      └─────────┬──────────┘
         │        /api  /uploads      │
         └──────────────┬─────────────┘
                        ▼
          ┌───────────────────────────┐
          │ Spring Boot 2.7.18 :8080  │
          │ MyBatis-Plus + JWT + POI  │
          └────────────┬──────────────┘
                       ▼
                ┌──────────────┐
                │ MySQL 8.0.46 │
                │ stockwork_db │
                └──────────────┘
```

- 后端分层：`controller → service → mapper(MyBatis-Plus) → entity`，统一返回 `{code, msg, data}`。
- 鉴权：JWT（Hutool）放在 `Authorization: Bearer <token>`，`AuthInterceptor` 解析后写入 `UserContext`，并通过自定义 `@RequireRole` 做角色校验。
- 数据一致性：出库按 `stock_batch` 最早 `create_time` 做 FIFO；库存更新使用条件 SQL 防止超卖；单据作废逐条反冲批次与物料库存。
- Excel：Apache POI 5.2.5，封装 `ExcelUtil`；前端通过 `download()` 以 blob 下载。
- 前端权限：后端返回角色 `perms`，前端按静态路由 `meta.perms` 过滤菜单，不依赖后端菜单表。

## 3. 目录结构

```
demo-6a/
├─ backend/                 # Spring Boot 后端
│  ├─ src/main/java/com/project/stockwork/{common,config,controller,dto,entity,mapper,service,util}
│  ├─ src/main/resources/application.yml
│  └─ .env                  # DB / JWT 配置（已 gitignore）
├─ new/                     # Web 管理端（基于 vue3-antdv-admin 基座）
│  └─ src/{api,router,store,views}
│     ├─ api/stockwork.ts   # 统一业务接口封装
│     ├─ views/{dashboard,warehouse,stock,batch,workorder,reportCheck,quality,system}
│     └─ router/routes/modules/{dashboard,warehouse,stock,batch,workorder,quality,system}.ts
├─ h5/                      # H5 报工端
│  └─ src/{views,components,api,stores,router}
├─ sql/{init_table,init_data,test_data}.sql
└─ docs/双基地仓储库存与生产报工管理系统/
```

## 4. 运行方式

### 4.1 数据库

```bash
mysql -uroot -p123456 --default-character-set=utf8mb4 -e "CREATE DATABASE IF NOT EXISTS stockwork_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;"
mysql -uroot -p123456 --default-character-set=utf8mb4 stockwork_db -e "source sql/init_table.sql; source sql/init_data.sql; source sql/test_data.sql;"
```

### 4.2 后端

`backend/.env` 关键配置：

```properties
DB_URL=jdbc:mysql://localhost:3306/stockwork_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=123456
JWT_SECRET=stockwork-secret-key-2026
JWT_EXPIRE=86400000
```

```bash
cd backend
mvn -DskipTests package
java -jar target/stockwork-1.0.0.jar
# http://localhost:8080/api   Knife4j: http://localhost:8080/doc.html
```

### 4.3 Web 管理端

```bash
cd new
pnpm install
pnpm dev          # http://localhost:8088
pnpm build        # 生产构建输出 new/dist
```

`.env.development` 已配置代理：`/api`、`/uploads` → `http://localhost:8080`（不剥离 `/api` 前缀）。

### 4.4 H5 报工端

```bash
cd h5
pnpm install
pnpm dev          # http://localhost:5174
pnpm build
```

## 5. 演示账号

| 角色 | 账号 | 密码 | 入口 | 能力 |
| --- | --- | --- | --- | --- |
| 管理员 ADMIN | `admin` | `123456` | Web | 全部菜单 + 用户/角色/字典/日志 |
| 业务员 BUSINESS | `manager` | `123456` | Web | 仓储、库存、追溯、工单、质检、统计 |
| 操作工 OPERATOR | `op_zhang` / `op_li` / `op_wang` | `123456` | H5 | 我的工单、逐工序报工、提交完工 |

## 6. 关键接口

| 分组 | 路径 |
| --- | --- |
| 认证 | `POST /api/auth/login`、`GET /api/auth/me`、`POST /api/auth/logout` |
| 公共 | `GET /api/common/{dict,baseSite,badReason,operator,product,warning}` |
| 仓储 | `GET/POST /api/warehouse/{list,options,save,delete}` |
| 库存 | `/api/stock/{inBound,outBound,batch/fifo,bill/list,bill/void,stagnant,stagnant/config,check,export}` |
| 追溯 | `/api/batch/{traceForward,traceBackward,traceForward/export,traceBackward/export}` |
| 工单 | `/api/workorder/{save,list,detail,progress,bom,bom/save,template,template/save,options}` |
| 质检 | `/api/reportCheck/{todo,audit}`、`/api/quality/{stat,stat/export,report/export}` |
| H5 报工 | `/api/report/{myOrders,detail,submit,finish}` |
| 系统 | `/api/system/{user/*,role/*,dict/*,log/*}` |
| 文件 | `POST /api/file/upload`、静态访问 `/uploads/**` |

## 7. 数据库表（12 张）

`sys_role`、`sys_user`、`sys_dict`、`sys_oper_log`、`warehouse_material`、`stock_batch`、`stock_bill`、`production_work_order`、`work_report_check`、`material_bom`、`process_template`、`work_order_process`。

## 8. 已知取舍

- 原文档建议 Vue2 + ElementUI，实际基座采用 `vue3-antdv-admin`（Vue3 + Ant Design Vue），接口与功能范围不变。
- 报工照片为本地磁盘存储（`backend/uploads`），生产环境建议替换为对象存储。
- 统计页以表格 + 关键指标呈现，未引入独立图表库。
- 演示数据中包含一条已报工的工单 4 第 1 道工序，便于直接演示 H5 报工与进度。
