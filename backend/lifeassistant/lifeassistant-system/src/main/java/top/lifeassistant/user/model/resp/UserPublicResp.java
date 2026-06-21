package top.lifeassistant.user.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "用户公开信息")
public class UserPublicResp {

    @Schema(description = "用户 ID")
    private String id;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "显示名称")
    private String fullName;

    @Schema(description = "是否激活")
    private Boolean isActive;

    @Schema(description = "是否超级管理员")
    private Boolean isSuperuser;

    @Schema(description = "伴侣 ID")
    private String partnerId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    public static UserPublicResp from(top.lifeassistant.system.model.entity.user.UserDO user) {
        return UserPublicResp.builder()
            .id(user.getId())
            .email(user.getEmail())
            .fullName(user.getFullName())
            .isActive(user.getIsActive())
            .isSuperuser(user.getIsSuperuser())
            .partnerId(user.getPartnerId())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
