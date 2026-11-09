-- 双基地仓储库存与生产报工管理系统 - 建表脚本（12 张表）
-- MySQL 8.0 / utf8mb4

CREATE DATABASE IF NOT EXISTS stockwork_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE stockwork_db;

DROP TABLE IF EXISTS work_report_check;
DROP TABLE IF EXISTS work_order_process;
DROP TABLE IF EXISTS production_work_order;
DROP TABLE IF EXISTS process_template;
DROP TABLE IF EXISTS material_bom;
DROP TABLE IF EXISTS stock_bill;
DROP TABLE IF EXISTS stock_batch;
DROP TABLE IF EXISTS warehouse_material;
DROP TABLE IF EXISTS sys_oper_log;
DROP TABLE IF EXISTS sys_dict;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;

-- 表1 sys_role 角色表
CREATE TABLE sys_role (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
  role_code   VARCHAR(50)  NOT NULL COMMENT '角色编码',
  menu_perms  VARCHAR(500) DEFAULT '' COMMENT '菜单权限标识，逗号分隔',
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 表2 sys_user 系统用户表
CREATE TABLE sys_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
  password    VARCHAR(100) NOT NULL COMMENT '密码（MD5）',
  real_name   VARCHAR(50)  DEFAULT '' COMMENT '真实姓名',
  role_id     BIGINT       NOT NULL COMMENT '角色ID',
  status      TINYINT      NOT NULL DEFAULT 1 COMMENT '0-禁用 1-启用',
  create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 表3 sys_dict 数据字典表
CREATE TABLE sys_dict (
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  dict_type  VARCHAR(50) NOT NULL COMMENT '字典类型',
  dict_label VARCHAR(50) NOT NULL COMMENT '显示名称',
  dict_value VARCHAR(50) NOT NULL COMMENT '存储值',
  sort       INT         NOT NULL DEFAULT 0 COMMENT '排序号',
  PRIMARY KEY (id),
  KEY idx_dict_type (dict_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- 表4 sys_oper_log 操作日志表
CREATE TABLE sys_oper_log (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  oper_user    VARCHAR(50)  DEFAULT '' COMMENT '操作人姓名',
  oper_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  oper_module  VARCHAR(100) DEFAULT '' COMMENT '操作模块',
  oper_content VARCHAR(500) DEFAULT '' COMMENT '操作内容',
  ip_address   VARCHAR(50)  DEFAULT '' COMMENT '操作IP',
  PRIMARY KEY (id),
  KEY idx_oper_time (oper_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 表5 warehouse_material 仓库物料表（单表：仓库 + 物料）
CREATE TABLE warehouse_material (
  id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  warehouse_name VARCHAR(100)  NOT NULL COMMENT '仓库名称',
  base_site      VARCHAR(30)   NOT NULL COMMENT '生产基地：宁波/合肥',
  material_code  VARCHAR(50)   NOT NULL COMMENT '物料编码',
  material_name  VARCHAR(100)  NOT NULL COMMENT '物料名称',
  material_type  TINYINT       NOT NULL DEFAULT 1 COMMENT '1原料 2半成品 3成品',
  total_stock    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '物料总库存',
  stock_min      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '库存下限',
  stock_max      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '库存上限',
  stagnant_day   INT           NOT NULL DEFAULT 90 COMMENT '呆滞阈值天数',
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_code (material_code),
  KEY idx_base_site (base_site)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库物料表';

-- 表6 stock_batch 物料批次表
CREATE TABLE stock_batch (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  warehouse_material_id BIGINT        NOT NULL COMMENT '仓库物料ID',
  batch_no              VARCHAR(80)   NOT NULL COMMENT '批次号',
  remain_stock          DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '批次剩余库存',
  create_time           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间（FIFO依据）',
  last_io_time          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后一次出入库时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_batch (warehouse_material_id, batch_no),
  KEY idx_last_io_time (last_io_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料批次表';

-- 表7 stock_bill 库存单据表
CREATE TABLE stock_bill (
  id                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  bill_no               VARCHAR(60)   NOT NULL COMMENT '单据编号',
  bill_type             TINYINT       NOT NULL COMMENT '1入库 2出库',
  warehouse_material_id BIGINT        NOT NULL COMMENT '仓库物料ID',
  batch_id              BIGINT        NOT NULL COMMENT '批次ID',
  oper_num              DECIMAL(10,2) NOT NULL COMMENT '出入库数量',
  oper_user             VARCHAR(50)   DEFAULT '' COMMENT '操作人',
  bill_time             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '单据时间',
  remark                VARCHAR(200)  DEFAULT '' COMMENT '备注',
  work_order_id         BIGINT        DEFAULT NULL COMMENT '生产工单ID（生产领料时必填）',
  bill_status           TINYINT       NOT NULL DEFAULT 0 COMMENT '0-正常 1-作废',
  PRIMARY KEY (id),
  UNIQUE KEY uk_bill_no (bill_no),
  KEY idx_material (warehouse_material_id),
  KEY idx_work_order (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存单据表';

-- 表8 production_work_order 生产工单表
CREATE TABLE production_work_order (
  id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  work_order_no    VARCHAR(60)   NOT NULL COMMENT '工单编号',
  base_site        VARCHAR(30)   NOT NULL COMMENT '生产基地',
  material_id      BIGINT        NOT NULL COMMENT '生产成品物料ID',
  plan_num         DECIMAL(10,2) NOT NULL COMMENT '计划生产数量',
  operator_user_id BIGINT        DEFAULT NULL COMMENT '分配操作工用户ID',
  work_status      TINYINT       NOT NULL DEFAULT 0 COMMENT '0待生产 1生产中 2待质检 3已完成',
  product_batch    VARCHAR(80)   DEFAULT '' COMMENT '成品批次号',
  create_time      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_work_order_no (work_order_no),
  KEY idx_operator (operator_user_id),
  KEY idx_product_batch (product_batch)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产工单表';

-- 表9 work_report_check 报工质检表（一工序一条）
CREATE TABLE work_report_check (
  id                  BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  work_order_id       BIGINT        NOT NULL COMMENT '工单ID',
  process_id          BIGINT        DEFAULT NULL COMMENT '工单工序ID',
  process_name        VARCHAR(100)  DEFAULT '' COMMENT '工序名称',
  good_num            DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '良品数量',
  bad_num             DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '不良品数量',
  bad_reason_dict_id  BIGINT        DEFAULT NULL COMMENT '不良原因字典ID',
  photo_url           VARCHAR(255)  DEFAULT '' COMMENT '生产照片地址',
  report_time         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报工时间',
  check_result        TINYINT       NOT NULL DEFAULT 0 COMMENT '0未质检 1合格 2不合格',
  check_user          VARCHAR(50)   DEFAULT '' COMMENT '质检人',
  check_time          DATETIME      DEFAULT NULL COMMENT '质检时间',
  check_remark        VARCHAR(200)  DEFAULT '' COMMENT '质检备注',
  check_bad_reason_id BIGINT        DEFAULT NULL COMMENT '质检不良原因字典ID',
  PRIMARY KEY (id),
  KEY idx_work_order (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报工质检表';

-- 表10 material_bom BOM 表
CREATE TABLE material_bom (
  id                   BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  product_material_id  BIGINT        NOT NULL COMMENT '成品物料ID',
  component_material_id BIGINT       NOT NULL COMMENT '组成物料ID',
  quantity             DECIMAL(10,2) NOT NULL DEFAULT 1 COMMENT '单位成品用量',
  remark               VARCHAR(200)  DEFAULT '' COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_product (product_material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BOM表';

-- 表11 process_template 工序模板表
CREATE TABLE process_template (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  material_id  BIGINT       NOT NULL COMMENT '产品物料ID',
  process_seq  INT          NOT NULL DEFAULT 1 COMMENT '工序顺序号',
  process_name VARCHAR(100) NOT NULL COMMENT '工序名称',
  requirement  VARCHAR(500) DEFAULT '' COMMENT '工序加工要求',
  PRIMARY KEY (id),
  KEY idx_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工序模板表';

-- 表12 work_order_process 工单工序表
CREATE TABLE work_order_process (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  work_order_id BIGINT      NOT NULL COMMENT '工单ID',
  process_seq  INT          NOT NULL DEFAULT 1 COMMENT '工序顺序号',
  process_name VARCHAR(100) NOT NULL COMMENT '工序名称',
  requirement  VARCHAR(500) DEFAULT '' COMMENT '工序加工要求',
  process_status TINYINT    NOT NULL DEFAULT 0 COMMENT '0未开始 1进行中 2已报工',
  PRIMARY KEY (id),
  KEY idx_work_order (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单工序表';
