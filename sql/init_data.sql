-- 初始化数据：3 角色 / 基础字典 / 管理员与业务账号
USE stockwork_db;

-- 角色（权限标识与后端 @RequireRole 对应）
INSERT INTO sys_role (id, role_name, role_code, menu_perms) VALUES
(1, '系统管理员', 'ADMIN', 'dashboard,warehouse,stock,workorder,quality,reportCheck,system:user,system:role,system:dict,system:log'),
(2, '业务管理人员', 'BUSINESS', 'dashboard,warehouse,stock,batch,workorder,quality,reportCheck'),
(3, '车间操作工', 'OPERATOR', 'h5:order,h5:report');

-- 用户（密码均为 123456 的 MD5：e10adc3949ba59abbe56e057f20f883e）
INSERT INTO sys_user (id, username, password, real_name, role_id, status, create_time) VALUES
(1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '系统管理员', 1, 1, '2026-09-01 09:00:00'),
(2, 'manager', 'e10adc3949ba59abbe56e057f20f883e', '王业务', 2, 1, '2026-09-01 09:10:00');

-- 字典：生产基地
INSERT INTO sys_dict (dict_type, dict_label, dict_value, sort) VALUES
('base_site', '宁波基地', '宁波', 1),
('base_site', '合肥基地', '合肥', 2),
-- 字典：不良原因
('bad_reason', '原材料缺陷', '原材料缺陷', 1),
('bad_reason', '设备问题', '设备问题', 2),
('bad_reason', '人工操作问题', '人工操作问题', 3),
-- 字典：物料类型
('material_type', '原材料', '1', 1),
('material_type', '半成品', '2', 2),
('material_type', '成品', '3', 3),
-- 字典：工单状态
('work_status', '待生产', '0', 1),
('work_status', '生产中', '1', 2),
('work_status', '待质检', '2', 3),
('work_status', '已完成', '3', 4);
