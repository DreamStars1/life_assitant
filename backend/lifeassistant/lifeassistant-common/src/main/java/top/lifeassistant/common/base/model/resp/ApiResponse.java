package top.lifeassistant.common.base.model.resp;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private int code;
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<FieldError> errors;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String traceId;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "ok", data, null, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data, null, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(200, "ok", null, null, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, null, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, null, traceId);
    }

    @Data
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
    }
}
