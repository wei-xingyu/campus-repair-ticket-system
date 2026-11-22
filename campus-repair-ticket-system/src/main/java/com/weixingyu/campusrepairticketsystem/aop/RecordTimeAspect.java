package com.weixingyu.campusrepairticketsystem.aop;

import com.weixingyu.campusrepairticketsystem.annotation.RecordOperation;
import com.weixingyu.campusrepairticketsystem.mapper.LogMapper;
import com.weixingyu.campusrepairticketsystem.pojo.entity.LoginUser;
import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@Aspect
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class RecordTimeAspect {
    private final LogMapper logMapper;
    private final TransactionTemplate logTransaction;

    public RecordTimeAspect(LogMapper logMapper, PlatformTransactionManager transactionManager) {
        this.logMapper = logMapper;
        this.logTransaction = new TransactionTemplate(transactionManager);
        this.logTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Around("execution(* com.weixingyu.campusrepairticketsystem.service.impl.*.*(..))"
            + " && !@annotation(com.weixingyu.campusrepairticketsystem.annotation.RecordOperation)")
    public Object recordTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            log.info("{} 耗时 {} ms", joinPoint.getSignature().toShortString(),
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        }
    }

    @Around(value = "@annotation(operation)", argNames = "joinPoint,operation")
    public Object recordOperation(ProceedingJoinPoint joinPoint, RecordOperation operation) throws Throwable {
        long start = System.nanoTime();
        int status = 0;
        String errorCode = null;
        Object result = null;
        try {
            result = joinPoint.proceed();
            if (result instanceof Result response && !Integer.valueOf(1).equals(response.getCode())) {
                errorCode = "BUSINESS_FAILURE";
            } else {
                status = 1;
            }
            return result;
        } catch (Throwable exception) {
            errorCode = "OPERATION_EXCEPTION";
            throw exception;
        } finally {
            long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("{} 耗时 {} ms", joinPoint.getSignature().toShortString(), durationMs);
            saveOperation(joinPoint, operation, result, durationMs, status, errorCode);
        }
    }

    private void saveOperation(ProceedingJoinPoint joinPoint, RecordOperation operation,
                               Object result, long durationMs, int status, String errorCode) {
        try {
            LoginUser user = null;
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                    && attributes.getRequest().getAttribute("loginUser") instanceof LoginUser loginUser) {
                user = loginUser;
            }
            Long userId = user == null ? null : user.getId();
            String username = user == null ? null : user.getUsername();
            String method = joinPoint.getSignature().getName();
            String methodName = joinPoint.getTarget().getClass().getSimpleName() + "." + method;
            String targetType = operation.targetType().isBlank() ? null : operation.targetType();
            Long targetId = resolveTargetId(joinPoint.getArgs(), operation, result, status);

            logTransaction.executeWithoutResult(transaction -> logMapper.insert(
                    userId, username, methodName, operation.value(), durationMs,
                    status, targetType, targetId, errorCode));
        } catch (RuntimeException exception) {
            log.warn("保存操作日志失败，方法={}", joinPoint.getSignature().toShortString(), exception);
        }
    }

    private Long resolveTargetId(Object[] args, RecordOperation operation, Object result, int status) {
        int index = operation.targetIdArgument();
        if (index >= 0 && index < args.length && args[index] instanceof Long id) {
            return id;
        }
        if (operation.targetIdFromResult() && status == 1
                && result instanceof Result response && response.getData() != null) {
            var data = PropertyAccessorFactory.forBeanPropertyAccess(response.getData());
            if (data.isReadableProperty("id") && data.getPropertyValue("id") instanceof Long id) {
                return id;
            }
        }
        return null;
    }
}
