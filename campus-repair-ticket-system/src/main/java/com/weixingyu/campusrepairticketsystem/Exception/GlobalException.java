package com.weixingyu.campusrepairticketsystem.Exception;

import com.weixingyu.campusrepairticketsystem.pojo.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestControllerAdvice
public class GlobalException {
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleValidation(org.springframework.web.bind.MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage()).findFirst().orElse("请求参数错误");
        return ResponseEntity.badRequest().body(Result.error(message));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.HandlerMethodValidationException.class)
    public ResponseEntity<Result> handleMethodValidation(org.springframework.web.method.annotation.HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream().map(error -> error.getDefaultMessage())
                .findFirst().orElse("请求参数错误");
        return ResponseEntity.badRequest().body(Result.error(message));
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result> handleInvalidInput(Exception e) {
        return ResponseEntity.badRequest().body(Result.error("请求内容格式或参数类型错误"));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result> handleResponseStatusException(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String message = e.getReason() == null ? "请求参数错误" : e.getReason();
        return ResponseEntity.status(status).body(Result.error(message));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error("上传内容超过服务器允许的大小"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result> handleMissingRequestParameter(MissingServletRequestParameterException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error("缺少必填上传参数: " + e.getParameterName()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Result> handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("数据库唯一约束冲突", e);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.error("数据已存在，请检查名称、账号、手机号或设备编码是否重复"));
    }
}
