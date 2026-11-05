package com.weixingyu.campusrepairticketsystem.mapper;

import com.weixingyu.campusrepairticketsystem.pojo.request.StatisticsRequest;
import com.weixingyu.campusrepairticketsystem.pojo.response.CategoryCountResponse;
import com.weixingyu.campusrepairticketsystem.pojo.response.StatisticsResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StatisticsMapper {

    @Select("SELECT COUNT(*) AS total_count, " +
            "COALESCE(SUM(CASE WHEN t.status = 'PENDING_ASSIGNMENT' THEN 1 ELSE 0 END), 0) AS pending_assignment_count, " +
            "COALESCE(SUM(CASE WHEN t.status IN ('ACCEPTED', 'PROCESSING') THEN 1 ELSE 0 END), 0) AS processing_count, " +
            "COALESCE(SUM(CASE WHEN t.status = 'PENDING_CONFIRMATION' THEN 1 ELSE 0 END), 0) AS pending_confirmation_count, " +
            "COALESCE(SUM(CASE WHEN t.status = 'COMPLETED' THEN 1 ELSE 0 END), 0) AS completed_count, " +
            "COALESCE(SUM(CASE WHEN t.status NOT IN ('COMPLETED', 'CANCELLED', 'CLOSED') " +
            "AND t.sla_due_at < CURRENT_TIMESTAMP(3) THEN 1 ELSE 0 END), 0) AS overdue_count, " +
            "(SELECT AVG(TIMESTAMPDIFF(SECOND, completed_ticket.created_at, history.completed_at) / 60.0) " +
            "FROM ticket completed_ticket JOIN (SELECT ticket_id, MIN(created_at) AS completed_at " +
            "FROM ticket_history WHERE to_status = 'COMPLETED' GROUP BY ticket_id) history " +
            "ON history.ticket_id = completed_ticket.id " +
            "WHERE completed_ticket.created_at >= #{request.from} AND completed_ticket.created_at <= #{request.to} " +
            "AND (#{request.buildingId} IS NULL OR completed_ticket.building_id = #{request.buildingId})) " +
            "AS average_completion_minutes " +
            "FROM ticket t WHERE t.created_at >= #{request.from} AND t.created_at <= #{request.to} " +
            "AND (#{request.buildingId} IS NULL OR t.building_id = #{request.buildingId})")
    StatisticsResponse querySummary(@Param("request") StatisticsRequest request);

    @Select("SELECT t.category_id AS category_id, t.category_name_snapshot AS category_name, COUNT(*) AS count " +
            "FROM ticket t WHERE t.created_at >= #{request.from} AND t.created_at <= #{request.to} " +
            "AND (#{request.buildingId} IS NULL OR t.building_id = #{request.buildingId}) " +
            "GROUP BY t.category_id, t.category_name_snapshot ORDER BY count DESC, t.category_id ASC")
    List<CategoryCountResponse> categoryCounts(@Param("request") StatisticsRequest request);
}
