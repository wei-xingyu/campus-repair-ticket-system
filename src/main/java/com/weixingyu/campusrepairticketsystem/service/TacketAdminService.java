package com.weixingyu.campusrepairticketsystem.service;

import com.weixingyu.campusrepairticketsystem.pojo.request.*;
import com.weixingyu.campusrepairticketsystem.pojo.response.LoginResponse;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import org.springframework.stereotype.Service;

@Service
public interface TacketAdminService {
    /**
     * 使用拦截器确认过的登录用户创建工单。
     */
    Result createTackets(CreateTacketsRequest createTacketsRequest, LoginResponse loginUser);

    Result queryByPage(QueryTicketsRequest queryTicketsRequest, LoginResponse loginUser);

    Result queryById(Long id, LoginResponse loginUser);

    Result adminDispatch(Long id, AdminDispatchRequest adminDispatchRequest, LoginResponse loginUser);

    Result adminSendTickets(Long id, AdminSendTicketsRequest request, LoginResponse loginUser);

    Result repairerAccept(Long id, ExpectedVersionRequest request, LoginResponse loginUser);

    Result repairerReject(Long id, ReasonVersionRequest request, LoginResponse loginUser);

    Result repairerStart(Long id, RepairerStartRequest request, LoginResponse loginUser);

    Result repairerResolution(Long id, RepairerResolutionRequest request, LoginResponse loginUser);

    Result studentConfirm(Long id, ExpectedVersionRequest request, LoginResponse loginUser);

    Result studentReopen(Long id, ReasonVersionRequest request, LoginResponse loginUser);

    Result studentCancel(Long id, CancelTicketRequest request, LoginResponse loginUser);

    Result adminClose(Long id, ReasonVersionRequest request, LoginResponse loginUser);

    Result queryTicketHistory(Long id, LoginResponse loginUser);

    Result studentRating(Long id,StudentRatingRequest request, LoginResponse loginUser);
}
