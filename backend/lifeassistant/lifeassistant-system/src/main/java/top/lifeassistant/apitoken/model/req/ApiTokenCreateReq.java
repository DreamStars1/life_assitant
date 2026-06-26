package top.lifeassistant.apitoken.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建 API Token 请求")
public class ApiTokenCreateReq {

    @Schema(description = "令牌别名", example = "Claude 桌面端")
    @NotBlank
    @Size(max = 50)
    private String name;

    @Schema(description = "过期时间，不传表示永不过期")
    private LocalDateTime expiresAt;
}
