package com.weixingyu.campusrepairticketsystem.service;

import com.weixingyu.campusrepairticketsystem.pojo.entity.LoginUser;
import com.weixingyu.campusrepairticketsystem.pojo.request.*;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import org.springframework.stereotype.Service;

@Service
public interface TacketAdminService {
    /**
     * 使用拦截器确认过的登录用户创建工单。
     */
    Result createTackets(CreateTacketsRequest createTacketsRequest, LoginUser loginUser);

    Result queryByPage(QueryTicketsRequest queryTicketsRequest, LoginUser loginUser);

    Result queryById(Long id, LoginUser loginUser);

    Result adminDispatch(Long id, AdminDispatchRequest adminDispatchRequest, LoginUser loginUser);

    Result adminSendTickets(Long id, AdminSendTicketsRequest request, LoginUser loginUser);

    Result repairerAccept(Long id, ExpectedVersionRequest request, LoginUser loginUser);

    Result repairerReject(Long id, ReasonVersionRequest request, LoginUser loginUser);

    Result repairerStart(Long id, RepairerStartRequest request, LoginUser loginUser);

    Result repairerResolution(Long id, RepairerResolutionRequest request, LoginUser loginUser);

    Result studentConfirm(Long id, ExpectedVersionRequest request, LoginUser loginUser);

    Result studentReopen(Long id, ReasonVersionRequest request, LoginUser loginUser);

    Result studentCancel(Long id, CancelTicketRequest request, LoginUser loginUser);

    Result adminClose(Long id, ReasonVersionRequest request, LoginUser loginUser);

    Result queryTicketHistory(Long id, LoginUser loginUser);

    Result studentRating(Long id,StudentRatingRequest request, LoginUser loginUser);
}
