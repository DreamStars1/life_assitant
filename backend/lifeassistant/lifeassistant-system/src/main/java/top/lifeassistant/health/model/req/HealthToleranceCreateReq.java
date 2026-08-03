package top.lifeassistant.health.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "耐受记录创建请求")
public class HealthToleranceCreateReq {

    @NotBlank
    @Schema(description = "名称")
    private String name;

    @NotBlank
    @Schema(description = "等级：舒适/低风险/谨慎/高风险")
    private String level;
}
