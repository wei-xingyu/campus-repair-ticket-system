package com.weixingyu.campusrepairticketsystem.pojo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {
    /** 数据库内部主键，用于区分工单记录。 */
    private Long id;
    /** 对外展示的工单编号，例如 BX202610190001。 */
    private String ticketNo;
    /** 报修人的用户 ID。 */
    private Long reporterId;
    /** 负责处理工单的维修人员用户 ID，未分配时为空。 */
    private Long assigneeId;
    /** 报修类别 ID。 */
    private Long categoryId;
    /** 报修地点所属楼栋 ID。 */
    private Long buildingId;
    /** 关联设备 ID，没有关联设备时为空。 */
    private Long assetId;
    /** 创建工单时的类别名称快照，避免类别改名影响历史工单展示。 */
    private String categoryNameSnapshot;
    /** 创建工单时的楼栋名称快照，避免楼栋改名影响历史工单展示。 */
    private String buildingNameSnapshot;
    /** 创建工单时的设备名称快照，避免设备改名影响历史工单展示。 */
    private String assetNameSnapshot;
    /** 工单标题。 */
    private String title;
    /** 报修问题的详细描述。 */
    private String description;
    /** 报修位置，例如房间号或设备所在位置。 */
    private String room;
    /** 工单优先级，例如 LOW、NORMAL、HIGH、URGENT。 */
    private String priority;
    /** 工单当前状态，例如待分配、处理中或已完成。 */
    private String status;
    /** 维修处理结果或解决办法，处理前通常为空。 */
    private String resolution;

    private Integer version;
    /** 工单 SLA 处理时限的截止时间。 */
    private LocalDateTime slaDueAt;
    /** 工单创建时间。 */
    private LocalDateTime createdAt;
    /** 工单最近一次更新时间。 */
    private LocalDateTime updatedAt;
}
