package top.lifeassistant.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "密码修改请求")
public class UserPasswordUpdateReq {

    @Schema(description = "当前密码")
    @NotBlank
    private String currentPassword;

    @Schema(description = "新密码")
    @NotBlank
    private String newPassword;
}
