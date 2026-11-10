package com.weixingyu.campusrepairticketsystem.interceptor;

import cn.hutool.core.util.StrUtil;
import com.weixingyu.campusrepairticketsystem.annotation.RequireRole;
import com.weixingyu.campusrepairticketsystem.cache.RedisCacheClient;
import com.weixingyu.campusrepairticketsystem.pojo.entity.LoginUser;
import com.weixingyu.campusrepairticketsystem.utils.RedisConstants;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/** 在请求进入 Controller 前统一检查登录状态和接口所需角色。 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisCacheClient redisCacheClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 1. 放行跨域预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 获取前端携带的 Token
        String token = request.getHeader("Authorization");
        if (StrUtil.isBlank(token)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return false;
        }
        // 3. 查询 Redis 中的登录态
        String key = RedisConstants.LOGIN_USER_KEY + token;
        LoginUser loginUser = redisCacheClient.get(key, LoginUser.class);
        if (loginUser == null || loginUser.getId() == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "登录已失效，请重新登录");
            return false;
        }

        // 5. 保存到本次请求，供后面的拦截器和 Controller 使用
        request.setAttribute("loginUser", loginUser);

        // 6. 刷新登录有效期
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        // 登录有效之后，继续检查接口要求的角色。
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
