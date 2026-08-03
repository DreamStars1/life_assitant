package top.lifeassistant.health.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "耐受记录更新请求")
public class HealthTolerancePatchReq {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "等级：舒适/低风险/谨慎/高风险")
    private String level;
}
