package top.lifeassistant.auth.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Schema(description = "登录请求参数")
@Data
public class LoginReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "邮箱", example = "user@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "密码", example = "password123")
    @NotBlank(message = "密码不能为空")
    private String password;
}
