package com.weixingyu.campusrepairticketsystem.mapper;

import com.weixingyu.campusrepairticketsystem.pojo.entity.Ticket;
import com.weixingyu.campusrepairticketsystem.pojo.request.QueryTicketsRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.*;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TacketAdminMapper {
    /**
     * 当天第一次创建时插入序号 1；之后每次请求都原子递增，避免并发取到相同序号。
     */
    @Insert("INSERT INTO ticket_no_sequence (sequence_date, current_value) VALUES (#{date}, 1) " +
            "ON DUPLICATE KEY UPDATE current_value = current_value + 1")
    void incrementDailySequence(@Param("date") java.time.LocalDate date);

    /**
     * 读取当天递增后的序号；事务会持有该行锁，直到创建流程结束。
     */
    @Select("SELECT current_value FROM ticket_no_sequence WHERE sequence_date = #{date} FOR UPDATE")
    int getDailySequence(@Param("date") java.time.LocalDate date);

    /**
     * 只取启用分类名称，用于检查分类并保存创建时名称快照。
     */
    @Select("SELECT name FROM repair_category WHERE id = #{id} AND enabled = 1")
    String findEnabledCategoryName(@Param("id") Long id);

    /**
     * 只取启用楼栋名称，用于检查楼栋并保存创建时名称快照。
     */
    @Select("SELECT name FROM building WHERE id = #{id} AND enabled = 1")
    String findEnabledBuildingName(@Param("id") Long id);

    /**
     * 设备必须启用且属于请求中选择的楼栋。
     */
    @Select("SELECT name FROM repair_asset WHERE id = #{assetId} AND building_id = #{buildingId} AND enabled = 1")
    String findEnabledAssetName(@Param("assetId") Long assetId, @Param("buildingId") Long buildingId);

    /**
     * 保存工单主体，并将数据库生成的主键回填到 Ticket.id。
     */
    @Insert("INSERT INTO ticket (ticket_no, reporter_id, category_id, building_id, asset_id, " +
            "category_name_snapshot, building_name_snapshot, asset_name_snapshot, title, description, room, priority, status,version,sla_due_at,created_at,updated_at) " +
            "VALUES (#{ticketNo}, #{reporterId}, #{categoryId}, #{buildingId}, #{assetId}, " +
            "#{categoryNameSnapshot}, #{buildingNameSnapshot}, #{assetNameSnapshot}, #{title}, #{description}, " +
            "#{room}, #{priority}, #{status},#{version}, #{slaDueAt},now(),now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTicket(Ticket ticket);

    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, operator_id, remark) VALUES (#{ticketId}, 0, NULL, 'PENDING_ASSIGNMENT', 'CREATE', #{operatorId}, '提交报修')")
    int insertCreationHistory(@Param("ticketId") Long ticketId, @Param("operatorId") Long operatorId);

    /**
     * 保存一条报修附件记录；附件大小暂未知，当前写入 0。
     */
    @Insert("INSERT INTO ticket_attachment (ticket_id, uploaded_by, file_type, file_url, sort_order, size_bytes) " +
            "VALUES (#{ticketId}, #{uploadedBy}, 'REPORT', #{fileUrl}, #{sortOrder}, 0)")
    int insertReportAttachment(@Param("ticketId") Long ticketId,
                               @Param("uploadedBy") Long uploadedBy,
                               @Param("fileUrl") String fileUrl,
                               @Param("sortOrder") int sortOrder);

    /**
     * 读取数据库实际写入的创建时间，作为接口响应的一部分。
     */
    @Select("SELECT created_at FROM ticket WHERE id = #{ticketId}")
    LocalDateTime findCreatedAt(@Param("ticketId") Long ticketId);

    List<QueryTicketsRow> queryByPage(QueryTicketsRequest queryTicketsRequest);

    TicketDetailResponse findTicketDetail(@Param("ticketId") Long ticketId);

    Long findReporterId(@Param("ticketId") Long ticketId);

    Long findAssigneeId(@Param("ticketId") Long ticketId);

    List<TicketAttachmentResponse> findTicketAttachments(@Param("ticketId") Long ticketId);

    List<TicketHistoryResponse> findTicketHistory(@Param("ticketId") Long ticketId);

    @Update("UPDATE ticket SET assignee_id = #{assigneeId}, status = 'PENDING_ACCEPTANCE', version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND version = #{version} AND status = 'PENDING_ASSIGNMENT'")
    int assignTicket(@Param("id") Long id, @Param("assigneeId") Long assigneeId, @Param("version") Integer version);

    @Update("UPDATE ticket SET status = 'ACCEPTED', version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND assignee_id = #{assigneeId} AND version = #{version} AND status = 'PENDING_ACCEPTANCE'")
    int acceptTicket(@Param("id") Long id, @Param("assigneeId") Long assigneeId, @Param("version") Integer version);

    @Update("UPDATE ticket SET status = 'PENDING_ASSIGNMENT', assignee_id = NULL, version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND assignee_id = #{assigneeId} AND version = #{version} AND status = 'PENDING_ACCEPTANCE'")
    int rejectTicket(@Param("id") Long id, @Param("assigneeId") Long assigneeId, @Param("version") Integer version);

    @Update("UPDATE ticket SET assignee_id = #{assigneeId}, status = 'PENDING_ACCEPTANCE', version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND version = #{version} AND status = 'PENDING_ACCEPTANCE'")
    int reassignTicket(@Param("id") Long id, @Param("assigneeId") Long assigneeId, @Param("version") Integer version);

    /**
     * 使用 ticket.version 作为期望旧版本；返回 0 表示记录不存在或版本冲突。
     */
    int updateTicket(Ticket ticket);

    @Select("select * from ticket where id = #{id}")
    Ticket selectTicketById(Long id);

    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, " +
            "operator_id, from_assignee_id, to_assignee_id, remark) " +
            "VALUES (#{ticketId}, #{eventVersion}, #{fromStatus}, #{toStatus}, 'ASSIGN', " +
            "#{operatorId}, #{fromAssigneeId}, #{toAssigneeId}, #{remark})")
    int insertAssignmentHistory(@Param("ticketId") Long ticketId,
                                @Param("eventVersion") Integer eventVersion,
                                @Param("fromStatus") String fromStatus,
                                @Param("toStatus") String toStatus,
                                @Param("operatorId") Long operatorId,
                                @Param("fromAssigneeId") Long fromAssigneeId,
                                @Param("toAssigneeId") Long toAssigneeId,
                                @Param("remark") String remark);

    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, " +
            "operator_id, from_assignee_id, to_assignee_id, remark) " +
            "VALUES (#{ticketId}, #{eventVersion}, #{fromStatus}, 'PENDING_ACCEPTANCE', 'REASSIGN', " +
            "#{operatorId}, #{fromAssigneeId}, #{toAssigneeId}, #{remark})")
    int insertReassignmentHistory(@Param("ticketId") Long ticketId,
                                  @Param("eventVersion") Integer eventVersion,
                                  @Param("fromStatus") String fromStatus,
                                  @Param("operatorId") Long operatorId,
                                  @Param("fromAssigneeId") Long fromAssigneeId,
                                  @Param("toAssigneeId") Long toAssigneeId,
                                  @Param("remark") String remark);

    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, " +
            "operator_id,remark) " +
            "VALUES (#{ticketId}, #{eventVersion}, #{fromStatus}, 'ACCEPTED', 'ACCEPT', " +
            "#{operatorId}, '已接单')")
    int insertAcceptHistory(@Param("ticketId") Long ticketId,
                            @Param("eventVersion") Integer eventVersion,
                            @Param("fromStatus") String fromStatus,
                            @Param("operatorId") Long operatorId);

    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, " +
            "operator_id, from_assignee_id, to_assignee_id, remark) " +
            "VALUES (#{ticketId}, #{eventVersion}, #{fromStatus}, 'PENDING_ASSIGNMENT', 'REJECT', " +
            "#{operatorId}, #{operatorId}, NULL, #{remark})")
    int insertRejectHistory(@Param("ticketId") Long ticketId,
                            @Param("eventVersion") Integer eventVersion,
                            @Param("fromStatus") String fromStatus,
                            @Param("operatorId") Long operatorId,
                            @Param("remark") String remark);

    @Update("UPDATE ticket SET status = 'PROCESSING', version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND version = #{version} AND status = 'ACCEPTED' AND assignee_id = #{assigneeId}")
    int repairerStartTicket(@Param("id") Long id, @Param("assigneeId") Long assigneeId, @Param("version") Integer version);


    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, " +
            "operator_id,remark) " +
            "VALUES (#{ticketId}, #{eventVersion}, #{fromStatus}, 'PROCESSING', 'START', " +
            "#{operatorId}, '处理中')")
    int repairerStartTicketHistory(@Param("ticketId") Long ticketId,
                                   @Param("eventVersion") Integer eventVersion,
                                   @Param("fromStatus") String fromStatus,
                                   @Param("operatorId") Long operatorId);

    @Update("UPDATE ticket SET resolution = #{resolution}, status = 'PENDING_CONFIRMATION', version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND version = #{version} AND status = 'PROCESSING' AND assignee_id = #{assigneeId}")
    int repairerResolutionTicket(@Param("id") Long id, @Param("assigneeId") Long assigneeId, @Param("version") Integer version, @Param("resolution") String resolution);

    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, " +
            "operator_id,remark) " +
            "VALUES (#{ticketId}, #{eventVersion}, #{fromStatus}, 'PENDING_CONFIRMATION', 'SUBMIT_RESOLUTION', " +
            "#{operatorId},#{remark})")
    int repairerResolutionTicketHistory(@Param("ticketId") Long ticketId,
                                        @Param("eventVersion") Integer eventVersion,
                                        @Param("fromStatus") String fromStatus,
                                        @Param("operatorId") Long operatorId,
                                        @Param("remark") String remark);

    /**
     * URL-only requests do not provide a file size; 0 means unknown.
     */
    @Insert("INSERT INTO ticket_attachment (ticket_id, uploaded_by, file_type, file_url, size_bytes, sort_order) " +
            "VALUES (#{ticketId}, #{uploadedBy}, 'RESOLUTION', #{fileUrl}, 0, #{sortOrder})")
    int insertResolutionAttachment(@Param("ticketId") Long ticketId,
                                   @Param("uploadedBy") Long uploadedBy,
                                   @Param("fileUrl") String fileUrl,
                                   @Param("sortOrder") Integer sortOrder);

    @Update("UPDATE ticket SET status = 'COMPLETED', version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND version = #{version} AND status = 'PENDING_CONFIRMATION' AND reporter_id = #{reporterId}")
    int studentConfirmTicket(@Param("id") Long id, @Param("reporterId") Long reporterId, @Param("version") Integer version);

    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, " +
            "operator_id,remark) " +
            "VALUES (#{ticketId}, #{eventVersion}, #{fromStatus}, 'COMPLETED', 'CONFIRM', " +
            "#{operatorId},'处理完成')")
    int studentConfirmTicketHistory(@Param("ticketId") Long ticketId,
                                    @Param("eventVersion") Integer eventVersion,
                                    @Param("fromStatus") String fromStatus,
                                    @Param("operatorId") Long operatorId);

    @Update("UPDATE ticket SET status = 'PROCESSING', version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND version = #{version} AND status = 'PENDING_CONFIRMATION' AND reporter_id = #{reporterId}")
    int studentReopenTicket(@Param("id") Long id, @Param("reporterId") Long reporterId, @Param("version") Integer version);

    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, " +
            "operator_id,remark) " +
            "VALUES (#{ticketId}, #{eventVersion}, #{fromStatus}, 'PROCESSING', 'REOPEN', " +
            "#{operatorId},#{remark})")
    int studentReopenTicketHistory(@Param("ticketId") Long ticketId,
                                   @Param("eventVersion") Integer eventVersion,
                                   @Param("fromStatus") String fromStatus,
                                   @Param("operatorId") Long operatorId,
                                   @Param("remark") String remark);

    @Update("UPDATE ticket SET status = 'CANCELLED', version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND version = #{version} AND (status = 'PENDING_ASSIGNMENT' OR status = 'PENDING_ACCEPTANCE' OR status = 'ACCEPTED') AND reporter_id = #{reporterId}")
    int studentCancelTicket(@Param("id") Long id, @Param("reporterId") Long reporterId, @Param("version") Integer version);

    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, " +
            "operator_id,remark) " +
            "VALUES (#{ticketId}, #{eventVersion}, #{fromStatus}, 'CANCELLED', 'CANCEL', " +
            "#{operatorId},#{remark})")
    int studentCancelTicketHistory(@Param("ticketId") Long ticketId,
                                   @Param("eventVersion") Integer eventVersion,
                                   @Param("fromStatus") String fromStatus,
                                   @Param("operatorId") Long operatorId,
                                   @Param("remark") String remark);

    @Update("UPDATE ticket SET status = 'CLOSED', version = version + 1, updated_at = CURRENT_TIMESTAMP(3) WHERE id = #{id} AND version = #{version} AND (status != 'COMPLETED' AND status != 'CANCELLED' AND status != 'CLOSED')")
    int adminCloseTicket(@Param("id") Long id, @Param("version") Integer version);

    @Insert("INSERT INTO ticket_history (ticket_id, event_version, from_status, to_status, action, " +
            "operator_id,remark) " +
            "VALUES (#{ticketId}, #{eventVersion}, #{fromStatus}, 'CLOSED', 'CLOSE', " +
            "#{operatorId},#{remark})")
    int adminCloseTicketHistory(@Param("ticketId") Long ticketId,
                                @Param("eventVersion") Integer eventVersion,
                                @Param("fromStatus") String fromStatus,
                                @Param("operatorId") Long operatorId,
                                @Param("remark") String remark);

    @Insert("INSERT INTO ticket_rating (ticket_id, reporter_id, score, comment, created_at) VALUES " +
            "(#{ticketId},#{reporterId},#{score},#{comment},CURRENT_TIMESTAMP(3))")
    int insertRating(@Param("ticketId") Long ticketId,
                     @Param("reporterId") Long reporterId,
                     @Param("score") Integer score,
                     @Param("comment") String comment);

    @Select("SELECT * FROM ticket_rating WHERE ticket_id = #{ticketId}")
    StudentRatingResponse selectRatingById(@Param("ticketId") Long ticketId);

}


