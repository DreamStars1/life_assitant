package top.lifeassistant.common.config.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.core.exception.BaseException;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.common.base.model.resp.ApiResponse;
import top.lifeassistant.common.base.model.resp.ApiResponse.FieldError;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotLogin(NotLoginException e) {
        log.warn("NotLoginException type={}", e.getType());
        String msg = switch (e.getType()) {
            case NotLoginException.NOT_TOKEN         -> "未登录";
            case NotLoginException.INVALID_TOKEN     -> "登录已过期，请重新登录";
            case NotLoginException.TOKEN_TIMEOUT     -> "登录已过期，请重新登录";
            case NotLoginException.BE_REPLACED       -> "账号已在其他设备登录";
            case NotLoginException.KICK_OUT          -> "账号已被管理员踢下线";
            default                                  -> "未认证";
        };
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(401, msg));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, e.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(BadRequestException e, HttpServletRequest request) {
        log.warn("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, e.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e,
                                                                        HttpServletRequest request) {
        log.warn("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, "参数校验失败"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e,
                                                                 HttpServletRequest request) {
        log.warn("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, "参数 '%s' 缺失".formatted(e.getParameterName())));
    }

    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e, HttpServletRequest request) {
        log.warn("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        List<FieldError> errors = e.getFieldErrors().stream()
            .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
            .collect(Collectors.toList());
        ApiResponse<Void> resp = new ApiResponse<>(422, "参数校验失败", null, errors, null);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(resp);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                                 HttpServletRequest request) {
        log.warn("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, "参数 '%s' 类型不匹配".formatted(e.getName())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e,
                                                                HttpServletRequest request) {
        log.warn("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        if (e.getCause() instanceof InvalidFormatException invalidFormatException) {
            log.warn("[DEBUG] InvalidFormatException value='{}' targetType='{}'",
                     invalidFormatException.getValue(),
                     invalidFormatException.getTargetType());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "参数 '%s' 类型不匹配".formatted(invalidFormatException.getValue())));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, "参数缺失或格式不正确"));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipartException(MultipartException e,
                                                                       HttpServletRequest request) {
        log.warn("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        String msg = e.getMessage();
        if (CharSequenceUtil.isNotBlank(msg)) {
            Throwable cause = e.getCause();
            if (cause != null) msg = msg.concat(cause.getMessage().toLowerCase());
            if (msg.contains("larger than")) {
                String sizeLimit = CharSequenceUtil.subAfter(msg, "larger than ", true);
                try {
                    long size = Long.parseLong(sizeLimit);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(400, "请上传小于 %s 的文件".formatted(FileUtil.readableFileSize(size))));
                } catch (NumberFormatException ignored) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(400, "文件大小超出限制"));
                }
            } else if (msg.contains("size") && msg.contains("exceed")) {
                String sizeLimit = CharSequenceUtil.subBetween(msg, "the maximum size ", " for");
                try {
                    long size = Long.parseLong(sizeLimit);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(400, "请上传小于 %s 的文件".formatted(FileUtil.readableFileSize(size))));
                } catch (NumberFormatException ignored) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.error(400, "文件大小超出限制"));
                }
            }
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, "文件上传失败"));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(404, "请求 URL 不存在"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                       HttpServletRequest request) {
        log.warn("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error(405, "请求方式 '%s' 不支持".formatted(e.getMethod())));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        log.error("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        MDC.clear();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, e.getMessage(), traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleAll(Exception e, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);
        log.error("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        MDC.clear();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, "服务器内部错误", traceId));
    }
}
