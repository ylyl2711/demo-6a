# TODO — 双基地仓储库存与生产报工管理系统

> 当前系统已可本地完整演示，以下为上线前/后续迭代的待办事项，按优先级排列。

## P1 上线前必须处理

| # | 事项 | 现状 | 操作指引 |
| --- | --- | --- | --- |
| 1 | 生产环境部署 | 仅本地 dev 运行 | Web：`cd new && pnpm build`，用 Nginx 托管 `new/dist`，`try_files $uri $uri/ /index.html`；H5：`cd h5 && pnpm build` 单独域名/端口托管；Nginx 将 `/api`、`/uploads` 反代到 `127.0.0.1:8080` |
| 2 | 敏感配置替换 | `.env` 中 `DB_PASSWORD=123456`、`JWT_SECRET` 为演示值 | 修改 `backend/.env`，使用强密码与 32 位以上随机 JWT 密钥；确认 `.env` 未提交 git |
| 3 | 附件目录持久化 | 图片存 `backend/uploads` | 配置 `app.upload.dir` 到持久化磁盘；定期备份；或改接 OSS/MinIO（需改 `FileController`） |
| 4 | 数据库备份 | 无自动备份 | 配置每日 `mysqldump stockwork_db`，保留 7~30 天 |

## P2 功能完善建议

| # | 事项 | 说明 |
| --- | --- | --- |
| 5 | 操作日志覆盖 | 目前登录、出入库、工单、质检等关键操作已记录；可补充物料档案、BOM/模板、用户的写操作日志 |
| 6 | 报工照片 | 当前每道工序 1 张；如需多张/压缩，扩展 `work_report_check.photo_url` 为 JSON 或新增附件表 |
| 7 | 真机拍照验证 | H5 的 `van-uploader` 拍照已在桌面浏览器验证上传接口，建议在 Android/iOS 真机再走一遍 |
| 8 | 统计可视化 | 不良统计目前为指标卡 + 表格，可接入 ECharts 增加趋势图/占比图 |
| 9 | 大数据量导出 | 目前一次性生成 Excel；数据量大时改为异步导出 + 下载中心 |
| 10 | 工单排产 | 当前为手工建单；可增加按交期/产能的排产与工单优先级 |

## P3 工程化

| # | 事项 | 说明 |
| --- | --- | --- |
| 11 | Node 版本 | 基座要求 Node 22.x，本机为 26.13.0，仅警告不影响运行；建议用 nvm 固定 22 LTS |
| 12 | 清理演示代码 | `new/src/views` 仍保留基座 demo/account 页面（构建产物含 1.7MB email chunk）；不影响功能，可后续删除并精简依赖 |
| 13 | 自动化测试 | 目前为接口冒烟 + 浏览器验证；建议补 Service 层单测（FIFO、反冲、状态机）与 Playwright E2E |
| 14 | CI/CD | 建议增加 `mvn -DskipTests package` + `pnpm typecheck && pnpm build` 流水线 |

## 需要你确认/提供的信息

1. 生产服务器与域名（决定 Nginx 与 HTTPS 配置）。
2. 图片存储方案：继续本地磁盘，还是对象存储（OSS/MinIO）。
3. 是否需要对接企业微信/钉钉消息（工单待质检、库存预警推送）。
4. 上述 P2 功能中优先实现哪几项。
