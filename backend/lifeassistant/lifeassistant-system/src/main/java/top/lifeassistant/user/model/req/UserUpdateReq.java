package top.lifeassistant.user.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户信息更新请求")
public class UserUpdateReq {

    @Schema(description = "显示名称")
    private String fullName;

    @Schema(description = "邮箱")
    private String email;
}
