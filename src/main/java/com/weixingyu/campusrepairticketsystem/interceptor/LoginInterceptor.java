package com.weixingyu.campusrepairticketsystem.interceptor;

import com.weixingyu.campusrepairticketsystem.annotation.RequireRole;
import com.weixingyu.campusrepairticketsystem.pojo.response.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;

/** 在请求进入 Controller 前统一检查登录状态和接口所需角色。 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    public static final String LOGIN_USER_ATTRIBUTE = "loginUser";
    private static final String LOGIN_SESSION_ATTRIBUTE = "loginResponse";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        // 浏览器跨域预检请求不携带登录 Cookie，应先允许其完成预检。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        Object sessionValue = session == null ? null : session.getAttribute(LOGIN_SESSION_ATTRIBUTE);
        if (!(sessionValue instanceof LoginResponse loginUser) || loginUser.getId() == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return false;
        }

        // Controller/Service 使用拦截器确认过的身份，不再各自读取或验证 Session。
        request.setAttribute(LOGIN_USER_ATTRIBUTE, loginUser);

        if (handler instanceof HandlerMethod handlerMethod) {
            RequireRole requireRole = AnnotatedElementUtils.findMergedAnnotation(
                    handlerMethod.getMethod(), RequireRole.class);
            if (requireRole == null) {
                requireRole = AnnotatedElementUtils.findMergedAnnotation(
                        handlerMethod.getBeanType(), RequireRole.class);
            }
            if (requireRole != null && Arrays.stream(requireRole.value())
                    .noneMatch(role -> role.equals(loginUser.getRole()))) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "当前账号没有权限访问此接口");
                return false;
            }
        }
        return true;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":0,\"msg\":\"" + message + "\",\"data\":null}");
    }
}
