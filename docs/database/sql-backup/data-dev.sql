-- LOCAL DEVELOPMENT ONLY. Do not import this file into a production database.
-- Test account password for all users: Dev@123456
-- The password hash is BCrypt-compatible; verify passwords with BCryptPasswordEncoder.
-- Inserts are intentionally idempotent. Existing application data is not deleted or reset.

USE campus_repair;

INSERT IGNORE INTO sys_role (role_code, role_name)
VALUES ('STUDENT', '报修人'), ('REPAIRER', '维修人员'), ('ADMIN', '管理员');

INSERT IGNORE INTO repair_category (name, description, enabled, sort_order)
VALUES
    ('水电', '供水、排水、照明及电路故障', 1, 10),
    ('门窗', '门锁、门窗及五金故障', 1, 20),
    ('网络', '校园网络和信息点故障', 1, 30),
    ('家具', '桌椅、柜体及公共设施故障', 1, 40),
    ('其他', '未归入以上类别的问题', 1, 99);

INSERT IGNORE INTO building (name, address, enabled, sort_order)
VALUES
    ('示例宿舍楼', '校园东区', 1, 10),
    ('第一教学楼', '校园中区', 1, 20),
    ('图书馆', '校园中心区', 1, 30);

INSERT IGNORE INTO sys_user (username, password_hash, name, enabled)
VALUES
    ('student001', '$2a$10$4Msl8xxH1KT.ExUMWmDBXuBu6uDVtknKnxSnkh0kHL2vQA8FCP8zq', '测试报修人甲', 1),
    ('student002', '$2a$10$4Msl8xxH1KT.ExUMWmDBXuBu6uDVtknKnxSnkh0kHL2vQA8FCP8zq', '测试报修人乙', 1),
    ('repairer001', '$2a$10$4Msl8xxH1KT.ExUMWmDBXuBu6uDVtknKnxSnkh0kHL2vQA8FCP8zq', '测试维修员', 1),
    ('admin001', '$2a$10$4Msl8xxH1KT.ExUMWmDBXuBu6uDVtknKnxSnkh0kHL2vQA8FCP8zq', '测试管理员', 1);

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.role_code = CASE u.username
    WHEN 'student001' THEN 'STUDENT'
    WHEN 'student002' THEN 'STUDENT'
    WHEN 'repairer001' THEN 'REPAIRER'
    WHEN 'admin001' THEN 'ADMIN'
END
WHERE u.username IN ('student001', 'student002', 'repairer001', 'admin001');

INSERT IGNORE INTO repair_asset (building_id, asset_code, name, location, enabled)
SELECT b.id, 'DORM-3-402-FCT', '卫生间水龙头', '3层402室卫生间', 1
FROM building b WHERE b.name = '示例宿舍楼'
UNION ALL
SELECT b.id, 'TEACH-1-201-LIGHT', '教室照明灯', '201教室', 1
FROM building b WHERE b.name = '第一教学楼'
UNION ALL
SELECT b.id, 'LIB-2-NET-01', '阅览区网络信息点', '二层东侧阅览区', 1
FROM building b WHERE b.name = '图书馆';

-- Example tickets cover the initial, processing, pending-confirmation and completed states.
-- Ticket numbers are stable so this script can be run again without duplicating them.
INSERT IGNORE INTO ticket (
    ticket_no, reporter_id, assignee_id, category_id, building_id, asset_id,
    category_name_snapshot, building_name_snapshot, asset_name_snapshot,
    title, description, room, priority, status, resolution, version, sla_due_at, created_at, updated_at
)
SELECT 'DEV202610190001', reporter.id, NULL, c.id, b.id, a.id,
       c.name, b.name, a.name,
       '宿舍卫生间水龙头持续滴水', '水龙头关闭后仍持续滴水，地面有少量积水。', '3层402室卫生间',
       'NORMAL', 'PENDING_ASSIGNMENT', NULL, 0,
       DATE_ADD(NOW(3) - INTERVAL 2 HOUR, INTERVAL 48 HOUR),
       NOW(3) - INTERVAL 2 HOUR, NOW(3) - INTERVAL 2 HOUR
FROM sys_user reporter
JOIN repair_category c ON c.name = '水电'
JOIN building b ON b.name = '示例宿舍楼'
JOIN repair_asset a ON a.building_id = b.id AND a.asset_code = 'DORM-3-402-FCT'
WHERE reporter.username = 'student001'
UNION ALL
SELECT 'DEV202610190002', reporter.id, repairer.id, c.id, b.id, NULL,
       c.name, b.name, NULL,
       '宿舍走廊照明不亮', '三层走廊有两盏灯不亮，夜间通行较暗。', '3层走廊',
       'HIGH', 'PROCESSING', NULL, 3,
       DATE_ADD(NOW(3) - INTERVAL 60 HOUR, INTERVAL 48 HOUR),
       NOW(3) - INTERVAL 60 HOUR, NOW(3) - INTERVAL 59 HOUR - INTERVAL 30 MINUTE
FROM sys_user reporter
JOIN sys_user repairer ON repairer.username = 'repairer001'
JOIN repair_category c ON c.name = '水电'
JOIN building b ON b.name = '示例宿舍楼'
WHERE reporter.username = 'student001'
UNION ALL
SELECT 'DEV202610190003', reporter.id, repairer.id, c.id, b.id, a.id,
       c.name, b.name, a.name,
       '图书馆阅览区网络信息点故障', '靠窗位置的信息点无法连接校园网络。', '二层东侧阅览区',
       'NORMAL', 'PENDING_CONFIRMATION', '已重新压接网线并完成连通性检查，请报修人确认。', 4,
       DATE_ADD(NOW(3) - INTERVAL 12 HOUR, INTERVAL 48 HOUR),
       NOW(3) - INTERVAL 12 HOUR, NOW(3) - INTERVAL 1 HOUR
FROM sys_user reporter
JOIN sys_user repairer ON repairer.username = 'repairer001'
JOIN repair_category c ON c.name = '网络'
JOIN building b ON b.name = '图书馆'
JOIN repair_asset a ON a.building_id = b.id AND a.asset_code = 'LIB-2-NET-01'
WHERE reporter.username = 'student002'
UNION ALL
SELECT 'DEV202610190004', reporter.id, repairer.id, c.id, b.id, a.id,
       c.name, b.name, a.name,
       '教学楼教室照明故障', '教室前排一盏照明灯闪烁。', '201教室',
       'LOW', 'COMPLETED', '已更换灯管并测试正常。', 5,
       DATE_ADD(NOW(3) - INTERVAL 5 DAY, INTERVAL 48 HOUR),
       NOW(3) - INTERVAL 5 DAY, NOW(3) - INTERVAL 3 DAY - INTERVAL 23 HOUR
FROM sys_user reporter
JOIN sys_user repairer ON repairer.username = 'repairer001'
JOIN repair_category c ON c.name = '水电'
JOIN building b ON b.name = '第一教学楼'
JOIN repair_asset a ON a.building_id = b.id AND a.asset_code = 'TEACH-1-201-LIGHT'
WHERE reporter.username = 'student001';

-- Each current ticket version has a matching history event version.
INSERT IGNORE INTO ticket_history (
    ticket_id, event_version, from_status, to_status, action,
    operator_id, from_assignee_id, to_assignee_id, remark, created_at
)
SELECT t.id, 0, NULL, 'PENDING_ASSIGNMENT', 'CREATE', reporter.id, NULL, NULL, '提交报修', t.created_at
FROM ticket t JOIN sys_user reporter ON reporter.username = 'student001'
WHERE t.ticket_no = 'DEV202610190001'
UNION ALL
SELECT t.id, 0, NULL, 'PENDING_ASSIGNMENT', 'CREATE', reporter.id, NULL, NULL, '提交报修', t.created_at
FROM ticket t JOIN sys_user reporter ON reporter.username = 'student001'
WHERE t.ticket_no = 'DEV202610190002'
UNION ALL
SELECT t.id, 1, 'PENDING_ASSIGNMENT', 'PENDING_ACCEPTANCE', 'ASSIGN', admin.id, NULL, repairer.id, '管理员派单', t.created_at + INTERVAL 10 MINUTE
FROM ticket t JOIN sys_user admin ON admin.username = 'admin001' JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190002'
UNION ALL
SELECT t.id, 2, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', repairer.id, repairer.id, repairer.id, '维修人员已接单', t.created_at + INTERVAL 20 MINUTE
FROM ticket t JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190002'
UNION ALL
SELECT t.id, 3, 'ACCEPTED', 'PROCESSING', 'START', repairer.id, repairer.id, repairer.id, '开始现场检查', t.created_at + INTERVAL 30 MINUTE
FROM ticket t JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190002'
UNION ALL
SELECT t.id, 0, NULL, 'PENDING_ASSIGNMENT', 'CREATE', reporter.id, NULL, NULL, '提交报修', t.created_at
FROM ticket t JOIN sys_user reporter ON reporter.username = 'student002'
WHERE t.ticket_no = 'DEV202610190003'
UNION ALL
SELECT t.id, 1, 'PENDING_ASSIGNMENT', 'PENDING_ACCEPTANCE', 'ASSIGN', admin.id, NULL, repairer.id, '管理员派单', t.created_at + INTERVAL 5 MINUTE
FROM ticket t JOIN sys_user admin ON admin.username = 'admin001' JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190003'
UNION ALL
SELECT t.id, 2, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', repairer.id, repairer.id, repairer.id, '维修人员已接单', t.created_at + INTERVAL 10 MINUTE
FROM ticket t JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190003'
UNION ALL
SELECT t.id, 3, 'ACCEPTED', 'PROCESSING', 'START', repairer.id, repairer.id, repairer.id, '开始处理', t.created_at + INTERVAL 20 MINUTE
FROM ticket t JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190003'
UNION ALL
SELECT t.id, 4, 'PROCESSING', 'PENDING_CONFIRMATION', 'SUBMIT_RESOLUTION', repairer.id, repairer.id, repairer.id, '提交处理结果，等待报修人确认', t.created_at + INTERVAL 11 HOUR
FROM ticket t JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190003'
UNION ALL
SELECT t.id, 0, NULL, 'PENDING_ASSIGNMENT', 'CREATE', reporter.id, NULL, NULL, '提交报修', t.created_at
FROM ticket t JOIN sys_user reporter ON reporter.username = 'student001'
WHERE t.ticket_no = 'DEV202610190004'
UNION ALL
SELECT t.id, 1, 'PENDING_ASSIGNMENT', 'PENDING_ACCEPTANCE', 'ASSIGN', admin.id, NULL, repairer.id, '管理员派单', t.created_at + INTERVAL 5 MINUTE
FROM ticket t JOIN sys_user admin ON admin.username = 'admin001' JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190004'
UNION ALL
SELECT t.id, 2, 'PENDING_ACCEPTANCE', 'ACCEPTED', 'ACCEPT', repairer.id, repairer.id, repairer.id, '维修人员已接单', t.created_at + INTERVAL 10 MINUTE
FROM ticket t JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190004'
UNION ALL
SELECT t.id, 3, 'ACCEPTED', 'PROCESSING', 'START', repairer.id, repairer.id, repairer.id, '开始处理', t.created_at + INTERVAL 20 MINUTE
FROM ticket t JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190004'
UNION ALL
SELECT t.id, 4, 'PROCESSING', 'PENDING_CONFIRMATION', 'SUBMIT_RESOLUTION', repairer.id, repairer.id, repairer.id, '更换灯管并测试正常', t.created_at + INTERVAL 20 HOUR
FROM ticket t JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190004'
UNION ALL
SELECT t.id, 5, 'PENDING_CONFIRMATION', 'COMPLETED', 'CONFIRM', reporter.id, repairer.id, repairer.id, '报修人确认完成', t.created_at + INTERVAL 25 HOUR
FROM ticket t JOIN sys_user reporter ON reporter.username = 'student001' JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190004';

INSERT IGNORE INTO ticket_attachment (
    ticket_id, uploaded_by, file_type, file_url, original_filename, media_type, size_bytes, sort_order
)
SELECT t.id, reporter.id, 'REPORT', '/uploads/dev/DEV202610190001/leaking-tap.jpg',
       'leaking-tap.jpg', 'image/jpeg', 128000, 0
FROM ticket t JOIN sys_user reporter ON reporter.username = 'student001'
WHERE t.ticket_no = 'DEV202610190001'
UNION ALL
SELECT t.id, repairer.id, 'RESOLUTION', '/uploads/dev/DEV202610190004/repaired-light.jpg',
       'repaired-light.jpg', 'image/jpeg', 156000, 0
FROM ticket t JOIN sys_user repairer ON repairer.username = 'repairer001'
WHERE t.ticket_no = 'DEV202610190004';

INSERT IGNORE INTO ticket_rating (ticket_id, reporter_id, score, comment)
SELECT t.id, reporter.id, 5, '维修及时，问题已解决。'
FROM ticket t JOIN sys_user reporter ON reporter.username = 'student001'
WHERE t.ticket_no = 'DEV202610190004' AND t.status = 'COMPLETED';
