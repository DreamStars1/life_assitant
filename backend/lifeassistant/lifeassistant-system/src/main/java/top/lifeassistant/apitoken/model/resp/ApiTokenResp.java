package top.lifeassistant.apitoken.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.apitoken.model.entity.ApiTokenDO;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "API Token 响应")
public class ApiTokenResp {

    private String id;
    private String name;
    private String tokenPrefix;

    @Schema(description = "完整 token，仅在创建时返回")
    private String fullToken;

    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public static ApiTokenResp from(ApiTokenDO token) {
        return ApiTokenResp.builder()
            .id(token.getId())
            .name(token.getName())
            .tokenPrefix(token.getTokenPrefix())
            .lastUsedAt(token.getLastUsedAt())
            .expiresAt(token.getExpiresAt())
            .isActive(token.getIsActive())
            .createdAt(token.getCreatedAt())
            .build();
    }

    public static ApiTokenResp withFullToken(ApiTokenDO token, String fullToken) {
        ApiTokenResp resp = from(token);
        resp.fullToken = fullToken;
        return resp;
    }
}
