package com.weixingyu.campusrepairticketsystem.controller;

import com.weixingyu.campusrepairticketsystem.annotation.RequireRole;
import com.weixingyu.campusrepairticketsystem.pojo.request.*;
import com.weixingyu.campusrepairticketsystem.pojo.response.LoginResponse;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import com.weixingyu.campusrepairticketsystem.service.TacketAdminService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
// 新旧两个路径都能创建工单；/tickets 是文档中的标准路径。
@RequestMapping({"/tickets"})
public class TacketAdminController {
    @Resource
    private TacketAdminService tacketAdminService;

    //3.1 创建保修工单
    @PostMapping
    @RequireRole("STUDENT")
    public Result createTackets(@Valid @RequestBody CreateTacketsRequest createTacketsRequest,
                                @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.createTackets(createTacketsRequest, loginUser);
    }

    //3.2 分页查询工单
    @GetMapping
    @RequireRole({"STUDENT", "REPAIRER", "ADMIN"})
    public Result queryByPage(@Valid @ModelAttribute QueryTicketsRequest queryTicketsRequest,
                              @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.queryByPage(queryTicketsRequest, loginUser);
    }

    //3.3 查询工单详情
    @GetMapping("/{id}")
    @RequireRole({"STUDENT", "REPAIRER", "ADMIN"})
    public Result queryById(@PathVariable @Positive(message = "ID必须为正数") Long id,
                            @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.queryById(id, loginUser);
    }

    @PostMapping("/{id}/assignment")
    @RequireRole("ADMIN")
    public Result adminDispatch(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody AdminDispatchRequest adminDispatchRequest,
                                @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.adminDispatch(id, adminDispatchRequest, loginUser);
    }

    @RequireRole("ADMIN")
    @PostMapping("/{id}/reassignment")
    public Result adminSendTickets(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody AdminSendTicketsRequest request,
                                   @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.adminSendTickets(id, request, loginUser);
    }

    @RequireRole("REPAIRER")
    @PostMapping("/{id}/accept")
    public Result repairerAccept(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody ExpectedVersionRequest request,
                                 @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.repairerAccept(id, request, loginUser);
    }

    @RequireRole("REPAIRER")
    @PostMapping("/{id}/reject")
    public Result repairerReject(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody ReasonVersionRequest request,
                                 @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.repairerReject(id, request, loginUser);
    }

    @RequireRole("REPAIRER")
    @PostMapping("/{id}/start")
    public Result repairerStart(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody RepairerStartRequest request,
                                @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.repairerStart(id, request, loginUser);
    }

    @RequireRole("REPAIRER")
    @PostMapping("/{id}/resolution")
    public Result repairerResolution(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody RepairerResolutionRequest request,
                                     @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.repairerResolution(id, request, loginUser);
    }

    @RequireRole("STUDENT")
    @PostMapping("/{id}/confirm")
    public Result studentConfirm(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody ExpectedVersionRequest request,
                                 @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.studentConfirm(id, request, loginUser);
    }

    @RequireRole("STUDENT")
    @PostMapping("/{id}/reopen")
    public Result studentReopen(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody ReasonVersionRequest request,
                                 @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.studentReopen(id, request, loginUser);
    }

    @RequireRole("STUDENT")
    @PostMapping("/{id}/cancel")
    public Result studentCancel(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody CancelTicketRequest request,
                                @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.studentCancel(id, request, loginUser);
    }

    @RequireRole("ADMIN")
    @PostMapping("/{id}/close")
    public Result adminClose(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody ReasonVersionRequest request,
                                @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.adminClose(id, request, loginUser);
    }

    @RequireRole({"STUDENT", "REPAIRER", "ADMIN"})
    @GetMapping("/{id}/history")
    public Result queryTicketHistory(@PathVariable @Positive(message = "ID必须为正数") Long id, @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.queryTicketHistory(id, loginUser);
    }

    @RequireRole("STUDENT")
    @PostMapping("/{id}/rating")
    public Result studentRating(@PathVariable @Positive(message = "ID必须为正数") Long id, @Valid @RequestBody StudentRatingRequest request,
                             @RequestAttribute("loginUser") LoginResponse loginUser) {
        return tacketAdminService.studentRating(id, request, loginUser);
    }
}
