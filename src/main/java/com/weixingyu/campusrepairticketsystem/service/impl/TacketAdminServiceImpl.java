package com.weixingyu.campusrepairticketsystem.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.weixingyu.campusrepairticketsystem.cache.RedisCacheClient;
import com.weixingyu.campusrepairticketsystem.pojo.entity.LoginUser;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.weixingyu.campusrepairticketsystem.mapper.AdminRepairerMapper;
import com.weixingyu.campusrepairticketsystem.mapper.TacketAdminMapper;
import com.weixingyu.campusrepairticketsystem.pojo.entity.SysUser;
import com.weixingyu.campusrepairticketsystem.pojo.entity.Ticket;
import com.weixingyu.campusrepairticketsystem.pojo.request.*;
import com.weixingyu.campusrepairticketsystem.pojo.response.*;
import com.weixingyu.campusrepairticketsystem.service.TacketAdminService;
import com.weixingyu.campusrepairticketsystem.utils.AliyunOSSOperator;
import com.weixingyu.campusrepairticketsystem.utils.RedisConstants;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class TacketAdminServiceImpl implements TacketAdminService {
    @Value("${ticket.sla-hours:48}")
    private long slaHours;

    @Resource
    private TacketAdminMapper tacketAdminMapper;

    @Resource
    private AdminRepairerMapper adminRepairerMapper;

    @Resource
    private AliyunOSSOperator aliyunOSSOperator;

    @Resource
    private RedisCacheClient redisCacheClient;

    @Override
    // 工单和附件必须一起成功或一起撤销，避免只保存一部分。
    @Transactional
    public Result createTackets(CreateTacketsRequest createTacketsRequest, LoginUser loginUser) {

        // 清理首尾空格，并检查必填内容和数据库字段长度限制。
        String title = trimToNull(createTacketsRequest.getTitle());
        String description = trimToNull(createTacketsRequest.getDescription());
        String room = trimToNull(createTacketsRequest.getRoom());
        if (title == null || title.length() < 2 || title.length() > 100) {
            return Result.error("标题长度必须为2到100个字符");
        }
        if (description == null || description.length() < 5 || description.length() > 2000) {
            return Result.error("问题描述长度必须为5到2000个字符");
        }

        // 查询启用中的分类和楼栋，同时取得名称用于保存历史快照。
        String categoryName = tacketAdminMapper.findEnabledCategoryName(createTacketsRequest.getCategoryId());
        String buildingName = tacketAdminMapper.findEnabledBuildingName(createTacketsRequest.getBuildingId());
        if (categoryName == null || buildingName == null) {
            return Result.error("报修类别或楼栋不存在或已停用");
        }

        // 设备是可选项；若选择设备，必须属于当前楼栋且仍处于启用状态。
        String assetName = null;
        if (createTacketsRequest.getAssetId() != null) {
            assetName = tacketAdminMapper.findEnabledAssetName(
                    createTacketsRequest.getAssetId(), createTacketsRequest.getBuildingId());
            if (assetName == null) {
                return Result.error("关联设备不存在、已停用或不属于所选楼栋");
            }
        }

        // 未指定优先级时按普通工单处理；报修人不能自行设置管理员级别的 URGENT。
        String priority = createTacketsRequest.getPriority();
        if (priority == null || priority.isBlank()) {
            priority = "NORMAL";
        }
        if (!Set.of("LOW", "NORMAL", "HIGH").contains(priority)) {
            return Result.error("优先级只能是 LOW、NORMAL 或 HIGH");
        }

        // 附件最多 5 个，只接受本接口生成的当前 Bucket OSS 对象 URL。
        String[] attachmentUrls = createTacketsRequest.getAttachmentUrls();

        if (attachmentUrls != null) {
            for (String url : attachmentUrls) {
                if (!aliyunOSSOperator.isOwnedTicketImage(url, loginUser.getId())) {
                    return Result.error("附件不存在或不是当前用户上传的工单图片");
                }
            }
        }

        // 将用户填写的信息和系统生成的信息组合成待保存的完整工单。
        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setReporterId(loginUser.getId());
        ticket.setCategoryId(createTacketsRequest.getCategoryId());
        ticket.setBuildingId(createTacketsRequest.getBuildingId());
        ticket.setAssetId(createTacketsRequest.getAssetId());
        ticket.setCategoryNameSnapshot(categoryName);
        ticket.setBuildingNameSnapshot(buildingName);
        ticket.setAssetNameSnapshot(assetName);
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setRoom(room);
        ticket.setPriority(priority);
        ticket.setVersion(0);
        ticket.setStatus("PENDING_ASSIGNMENT");
        LocalDateTime createdAt = LocalDateTime.now();
        ticket.setCreatedAt(createdAt);
        ticket.setSlaDueAt(createdAt.plusHours(slaHours));
        // 插入工单后，数据库生成的自增 id 会回填到 ticket.id。
        if (tacketAdminMapper.insertTicket(ticket) != 1) throw new IllegalStateException("工单保存失败");
        if (tacketAdminMapper.insertCreationHistory(ticket.getId(), loginUser.getId()) != 1)
            throw new IllegalStateException("创建历史写入失败");

        // 工单已有 id 后才能保存附件与工单的关联。
        if (attachmentUrls != null) {
            for (int i = 0; i < attachmentUrls.length; i++) {
                tacketAdminMapper.insertReportAttachment(ticket.getId(), loginUser.getId(), attachmentUrls[i], i);
            }
        }

        // 返回数据库实际生成的工单编号、主键、状态和创建时间。
        CreateTacketsResponse createTacketsResponse = new CreateTacketsResponse();
        createTacketsResponse.setId(ticket.getId());
        createTacketsResponse.setCreatedAt(tacketAdminMapper.findCreatedAt(ticket.getId()));
        createTacketsResponse.setTicketNo(ticket.getTicketNo());
        createTacketsResponse.setStatus(ticket.getStatus());
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + ticket.getId());
        return Result.success(createTacketsResponse);
    }

    @Override
    public Result queryByPage(QueryTicketsRequest queryTicketsRequest, LoginUser loginUser) {

        if (queryTicketsRequest.getPage() == null) {
            queryTicketsRequest.setPage(1);
        }
        if (queryTicketsRequest.getPageSize() == null) {
            queryTicketsRequest.setPageSize(10);
        }

        if (queryTicketsRequest.getKeyword() != null) {
            queryTicketsRequest.setKeyword(queryTicketsRequest.getKeyword().trim());
        }

        // 拒绝反向时间范围，LocalDateTime 参数由 Spring 按 ISO 格式绑定。
        if (queryTicketsRequest.getCreatedFrom() != null && queryTicketsRequest.getCreatedTo() != null
                && queryTicketsRequest.getCreatedFrom().isAfter(queryTicketsRequest.getCreatedTo())) {
            return Result.error("创建时间起始值不能晚于结束值");
        }

        // 角色范围由服务端决定；只有管理员可以使用请求中的 reporterId/assigneeId 过滤条件。
        if ("STUDENT".equals(loginUser.getRole())) {
            if (queryTicketsRequest.getReporterId() != null || queryTicketsRequest.getAssigneeId() != null) {
                return Result.error("当前角色不能使用 reporterId 或 assigneeId 筛选");
            }
            queryTicketsRequest.setReporterId(loginUser.getId());
            queryTicketsRequest.setAssigneeId(null);
        } else if ("REPAIRER".equals(loginUser.getRole())) {
            if (queryTicketsRequest.getReporterId() != null || queryTicketsRequest.getAssigneeId() != null) {
                return Result.error("当前角色不能使用 reporterId 或 assigneeId 筛选");
            }
            queryTicketsRequest.setReporterId(null);
            queryTicketsRequest.setAssigneeId(loginUser.getId());
        }

        if (queryTicketsRequest.getReporterKeyword() != null || queryTicketsRequest.getAssigneeKeyword() != null) {
            if (!"ADMIN".equals(loginUser.getRole())) {
                return Result.error("只有管理员可以按人员姓名或账号筛选");
            }
            if (queryTicketsRequest.getReporterKeyword() != null) {
                queryTicketsRequest.setReporterKeyword(queryTicketsRequest.getReporterKeyword().trim());
            }
            if (queryTicketsRequest.getAssigneeKeyword() != null) {
                queryTicketsRequest.setAssigneeKeyword(queryTicketsRequest.getAssigneeKeyword().trim());
            }
        }
        PageHelper.startPage(queryTicketsRequest.getPage(), queryTicketsRequest.getPageSize());
        List<QueryTicketsRow> rows = tacketAdminMapper.queryByPage(queryTicketsRequest);
        PageInfo<QueryTicketsRow> pageInfo = new PageInfo<>(rows);
        return Result.success(new QueryTicketByPageResponse(
                pageInfo.getTotal(), pageInfo.getList(), queryTicketsRequest.getPage(), queryTicketsRequest.getPageSize()));
    }

    @Override
    public Result queryById(Long id, LoginUser loginUser) {
        long ttlMinutes = RedisConstants.TICKET_DETAILED_TTL + RandomUtil.randomInt(-5, 6);
        TicketDetailResponse detail = redisCacheClient.getWithCacheMissLock(
                RedisConstants.TICKET_DETAILED_KEY, id, TicketDetailResponse.class,
                this::loadTicketDetail, ttlMinutes, TimeUnit.MINUTES);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        requireTicketReadAccess(id, loginUser);
        return Result.success(detail);
    }

    private TicketDetailResponse loadTicketDetail(Long id) {
        TicketDetailResponse detail = tacketAdminMapper.findTicketDetail(id);
        if (detail != null) {
            detail.setAttachments(tacketAdminMapper.findTicketAttachments(id));
            detail.setHistory(tacketAdminMapper.findTicketHistory(id));
        }
        return detail;
    }

    @Override
    @Transactional
    public Result adminDispatch(Long id, AdminDispatchRequest adminDispatchRequest, LoginUser loginUser) {

        SysUser targetRepairer = adminRepairerMapper.findEnabledRepairerById(adminDispatchRequest.getAssigneeId());
        if (targetRepairer == null) {
            return Result.error("维修员不存在、未启用或角色无效");
        }
        Ticket ticket = tacketAdminMapper.selectTicketById(id);
        if (ticket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "不存在此工单");
        }
        if (!"PENDING_ASSIGNMENT".equals(ticket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单状态不允许派单");
        }
        if (ticket.getVersion() == null) {
            return Result.error("工单版本数据异常，无法派单");
        }
        if (!adminDispatchRequest.getExpectedVersion().equals(ticket.getVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单版本已变化，请刷新后重试");
        }

        Long previousAssigneeId = ticket.getAssigneeId();
        ticket.setAssigneeId(adminDispatchRequest.getAssigneeId());
        ticket.setStatus("PENDING_ACCEPTANCE");
        int updated = tacketAdminMapper.assignTicket(id, adminDispatchRequest.getAssigneeId(), ticket.getVersion());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }

        // updateTicket 内的 SQL 只递增一次版本；重新读取数据库实际版本供历史记录使用。
        Ticket updatedTicket = tacketAdminMapper.selectTicketById(id);
        if (updatedTicket == null || updatedTicket.getVersion() == null) {
            throw new IllegalStateException("派单后无法读取工单版本");
        }
        int historyInserted = tacketAdminMapper.insertAssignmentHistory(id, updatedTicket.getVersion(),
                "PENDING_ASSIGNMENT", "PENDING_ACCEPTANCE", loginUser.getId(), previousAssigneeId,
                adminDispatchRequest.getAssigneeId(), adminDispatchRequest.getRemark());
        if (historyInserted != 1) {
            throw new IllegalStateException("派单历史写入失败");
        }
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + id);
        return Result.success();

    }

    @Override
    @Transactional
    public Result adminSendTickets(Long id, AdminSendTicketsRequest request, LoginUser loginUser) {

        SysUser targetRepairer = adminRepairerMapper.findEnabledRepairerById(request.getAssigneeId());
        if (targetRepairer == null) {
            return Result.error("维修员不存在、未启用或角色无效");
        }
        Ticket targetTicket = tacketAdminMapper.selectTicketById(id);
        if (targetTicket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "不存在此工单");
        }
        if (!targetTicket.getVersion().equals(request.getExpectedVersion()) ||
                !"PENDING_ACCEPTANCE".equals(targetTicket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "版本号或状态码冲突");
        }
        Long previousAssigneeId = targetTicket.getAssigneeId();
        int updated = tacketAdminMapper.reassignTicket(id, request.getAssigneeId(), request.getExpectedVersion());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }
        int success = tacketAdminMapper.insertReassignmentHistory(id, request.getExpectedVersion() + 1, targetTicket.getStatus(),
                loginUser.getId(), previousAssigneeId, request.getAssigneeId(), request.getReason());
        if (success != 1) {
            throw new IllegalStateException("转派历史写入失败");
        }
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + id);
        return Result.success();
    }

    @Override
    @Transactional
    public Result repairerAccept(Long id, ExpectedVersionRequest request, LoginUser loginUser) {

        Ticket targetTicket = tacketAdminMapper.selectTicketById(id);
        if (targetTicket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        if (!targetTicket.getVersion().equals(request.getExpectedVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单版本号不一致");
        }
        if (!"PENDING_ACCEPTANCE".equals(targetTicket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单状态非法");
        }
        if (!loginUser.getId().equals(targetTicket.getAssigneeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户不是此工单被分派的维修人员");
        }
        int updated = tacketAdminMapper.acceptTicket(id, loginUser.getId(), request.getExpectedVersion());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }
        int success = tacketAdminMapper.insertAcceptHistory(id, request.getExpectedVersion() + 1, targetTicket.getStatus(), loginUser.getId());
        if (success != 1) {
            throw new IllegalStateException("接单历史写入失败");
        }
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + id);
        return Result.success();
    }

    @Override
    @Transactional
    public Result repairerReject(Long id, ReasonVersionRequest request, LoginUser loginUser) {

        Ticket targetTicket = tacketAdminMapper.selectTicketById(id);
        if (targetTicket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        if (!targetTicket.getVersion().equals(request.getExpectedVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单版本号不一致");
        }
        if (!"PENDING_ACCEPTANCE".equals(targetTicket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单状态非法");
        }
        String reason = trimToNull(request.getReason());
        if (reason == null || reason.length() < 2 || reason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "说明原因需在2-500字之间");
        }
        if (!loginUser.getId().equals(targetTicket.getAssigneeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户不是此工单被分派的维修人员");
        }
        int updated = tacketAdminMapper.rejectTicket(id, loginUser.getId(), request.getExpectedVersion());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }
        int success = tacketAdminMapper.insertRejectHistory(id, request.getExpectedVersion() + 1, targetTicket.getStatus(), loginUser.getId(), reason);
        if (success != 1) {
            throw new IllegalStateException("拒绝接单失败");
        }
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + id);
        return Result.success();
    }

    @Transactional
    @Override
    public Result repairerStart(Long id, RepairerStartRequest request, LoginUser loginUser) {
        Ticket targetTicket = tacketAdminMapper.selectTicketById(id);
        if (targetTicket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        if (!targetTicket.getVersion().equals(request.getExpectedVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单版本号不一致");
        }
        if (!"ACCEPTED".equals(targetTicket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单状态非法");
        }
        if (!loginUser.getId().equals(targetTicket.getAssigneeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户不是此工单被分派的维修人员");
        }
        int updated = tacketAdminMapper.repairerStartTicket(id, loginUser.getId(), request.getExpectedVersion());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }
        int success = tacketAdminMapper.repairerStartTicketHistory(id, request.getExpectedVersion() + 1, targetTicket.getStatus(), loginUser.getId());
        if (success != 1) {
            throw new IllegalStateException("开始处理失败");
        }
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + id);
        return Result.success();
    }

    @Transactional
    @Override
    public Result repairerResolution(Long id, RepairerResolutionRequest request, LoginUser loginUser) {
        Ticket targetTicket = tacketAdminMapper.selectTicketById(id);
        if (targetTicket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        if (!targetTicket.getVersion().equals(request.getExpectedVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单版本号不一致");
        }
        if (!"PROCESSING".equals(targetTicket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单状态非法");
        }
        if (!loginUser.getId().equals(targetTicket.getAssigneeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户不是此工单被分派的维修人员");
        }
        String[] urls = request.getAttachmentUrls();
        if (urls != null) {
            for (String url : urls) {
                if (url == null || url.isBlank() || !aliyunOSSOperator.isOwnedTicketImage(url, loginUser.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "附件地址无效，必须是允许的已上传图片地址");
                }
            }
        }
        int updated = tacketAdminMapper.repairerResolutionTicket(id, loginUser.getId(), request.getExpectedVersion(), request.getResolution());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }
        int success = tacketAdminMapper.repairerResolutionTicketHistory(id, request.getExpectedVersion() + 1, targetTicket.getStatus(), loginUser.getId(), request.getResolution());
        if (success != 1) {
            throw new IllegalStateException("处理结果历史写入失败");
        }
        if (urls != null) {
            for (int index = 0; index < urls.length; index++) {
                if (tacketAdminMapper.insertResolutionAttachment(id, loginUser.getId(), urls[index], index) != 1) {
                    throw new IllegalStateException("处理图片记录保存失败");
                }
            }
        }
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + id);
        return Result.success();
    }

    @Transactional
    @Override
    public Result studentConfirm(Long id, ExpectedVersionRequest request, LoginUser loginUser) {
        Ticket targetTicket = tacketAdminMapper.selectTicketById(id);
        if (targetTicket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        if (!targetTicket.getVersion().equals(request.getExpectedVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单版本号不一致");
        }
        if (!"PENDING_CONFIRMATION".equals(targetTicket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单状态非法");
        }
        if (!loginUser.getId().equals(targetTicket.getReporterId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户不是此工单的创建人员");
        }
        int updated = tacketAdminMapper.studentConfirmTicket(id, loginUser.getId(), request.getExpectedVersion());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }
        int success = tacketAdminMapper.studentConfirmTicketHistory(id, request.getExpectedVersion() + 1, targetTicket.getStatus(), loginUser.getId());
        if (success != 1) {
            throw new IllegalStateException("确认完成历史写入失败");
        }
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + id);
        return Result.success();
    }

    @Transactional
    @Override
    public Result studentReopen(Long id, ReasonVersionRequest request, LoginUser loginUser) {
        Ticket targetTicket = tacketAdminMapper.selectTicketById(id);
        if (targetTicket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        if (!targetTicket.getVersion().equals(request.getExpectedVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单版本号不一致");
        }
        if (!"PENDING_CONFIRMATION".equals(targetTicket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单状态非法");
        }
        if (!loginUser.getId().equals(targetTicket.getReporterId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户不是此工单的创建人员");
        }
        int updated = tacketAdminMapper.studentReopenTicket(id, loginUser.getId(), request.getExpectedVersion());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }
        int success = tacketAdminMapper.studentReopenTicketHistory(id, request.getExpectedVersion() + 1, targetTicket.getStatus(), loginUser.getId(), request.getReason());
        if (success != 1) {
            throw new IllegalStateException("重新处理历史写入失败");
        }
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + id);
        return Result.success();
    }

    @Transactional
    @Override
    public Result studentCancel(Long id, CancelTicketRequest request, LoginUser loginUser) {
        Ticket targetTicket = tacketAdminMapper.selectTicketById(id);
        if (targetTicket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        if (!targetTicket.getVersion().equals(request.getExpectedVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单版本号不一致");
        }
        if (!"PENDING_ACCEPTANCE".equals(targetTicket.getStatus()) && !"PENDING_ASSIGNMENT".equals(targetTicket.getStatus()) && !"ACCEPTED".equals(targetTicket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单状态非法");
        }
        if (!loginUser.getId().equals(targetTicket.getReporterId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户不是此工单的创建人员");
        }
        int updated = tacketAdminMapper.studentCancelTicket(id, loginUser.getId(), request.getExpectedVersion());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }
        int success = tacketAdminMapper.studentCancelTicketHistory(id, request.getExpectedVersion() + 1, targetTicket.getStatus(), loginUser.getId(), request.getReason());
        if (success != 1) {
            throw new IllegalStateException("取消工单历史写入失败");
        }
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + id);
        return Result.success();
    }

    @Transactional
    @Override
    public Result adminClose(Long id, ReasonVersionRequest request, LoginUser loginUser) {
        Ticket targetTicket = tacketAdminMapper.selectTicketById(id);
        if (targetTicket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        if (!targetTicket.getVersion().equals(request.getExpectedVersion())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单版本号不一致");
        }
        if ("COMPLETED".equals(targetTicket.getStatus()) || "CANCELLED".equals(targetTicket.getStatus()) || "CLOSED".equals(targetTicket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单状态非法");
        }
        int updated = tacketAdminMapper.adminCloseTicket(id, request.getExpectedVersion());
        if (updated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }
        int success = tacketAdminMapper.adminCloseTicketHistory(id, request.getExpectedVersion() + 1, targetTicket.getStatus(), loginUser.getId(), request.getReason());
        if (success != 1) {
            throw new IllegalStateException("关闭工单历史写入失败");
        }
        redisCacheClient.deleteAfterCommit(RedisConstants.TICKET_DETAILED_KEY + id);
        return Result.success();
    }

    @Override
    public Result queryTicketHistory(Long id, LoginUser loginUser) {
        if (tacketAdminMapper.selectTicketById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        requireTicketReadAccess(id, loginUser);
        return Result.success(tacketAdminMapper.findTicketHistory(id));
    }

    @Override
    public Result studentRating(Long id, StudentRatingRequest request, LoginUser loginUser) {
        Ticket targetTicket = tacketAdminMapper.selectTicketById(id);
        if (targetTicket == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        if (!targetTicket.getReporterId().equals(loginUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前用户不是此工单的创建人员");
        }
        if (!"COMPLETED".equals(targetTicket.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单状态非法");
        }
        StudentRatingResponse rating = tacketAdminMapper.selectRatingById(id);
        if (rating != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前工单已有评价，不可重复评价");
        }
        int inserted = tacketAdminMapper.insertRating(id, loginUser.getId(), request.getScore(), request.getComment());
        if (inserted != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "工单已被其他操作更新，请刷新后重试");
        }
        StudentRatingResponse result = tacketAdminMapper.selectRatingById(id);
        return Result.success(result);
    }

    private void requireTicketReadAccess(Long id, LoginUser loginUser) {
        String role = loginUser.getRole();
        if ("STUDENT".equals(role)) {
            Long reporterId = tacketAdminMapper.findReporterId(id);
            if (!loginUser.getId().equals(reporterId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该工单");
            }
        } else if ("REPAIRER".equals(role)) {
            Long assigneeId = tacketAdminMapper.findAssigneeId(id);
            if (!loginUser.getId().equals(assigneeId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该工单");
            }
        } else if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问该工单");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * 仅接受当前 OSS Bucket 下由上传接口生成的图片 URL。
     */

    /**
     * 用数据库中的每日计数生成 BX + yyyyMMdd + 四位序号。
     */
    private String generateTicketNo() {
        LocalDate today = LocalDate.now();
        tacketAdminMapper.incrementDailySequence(today);
        int sequence = tacketAdminMapper.getDailySequence(today);
        if (sequence > 9999) {
            throw new IllegalStateException("当日工单编号已超过四位序号容量");
        }
        return "BX" + today.format(DateTimeFormatter.BASIC_ISO_DATE)
                + String.format("%04d", sequence);
    }
}

