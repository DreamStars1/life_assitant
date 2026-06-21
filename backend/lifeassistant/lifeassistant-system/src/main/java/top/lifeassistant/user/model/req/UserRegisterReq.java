package top.lifeassistant.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "用户注册请求")
public class UserRegisterReq {

    @Schema(description = "邮箱", example = "user@example.com")
    @NotBlank @Email
    private String email;

    @Schema(description = "密码", example = "password123")
    @NotBlank
    private String password;

    @Schema(description = "显示名称")
    private String fullName;
}
