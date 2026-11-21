package com.weixingyu.campusrepairticketsystem.mapper;

import com.weixingyu.campusrepairticketsystem.pojo.response.LogRowResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LogMapper {
    void insert(@Param("userId") Long userId,
                @Param("username") String username,
                @Param("methodName") String methodName,
                @Param("operation") String operation,
                @Param("durationMs") long durationMs,
                @Param("status") int status,
                @Param("targetType") String targetType,
                @Param("targetId") Long targetId,
                @Param("errorCode") String errorCode);

    List<LogRowResponse> queryByPage(@Param("userId") Long userId,
                                    @Param("status") Integer status,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

}
