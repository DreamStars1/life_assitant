package top.lifeassistant.health.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "健康规律创建请求")
public class HealthMemoryCreateReq {

    @NotBlank
    @Schema(description = "标题")
    private String title;

    @Schema(description = "详情，可空")
    private String detail;
}
