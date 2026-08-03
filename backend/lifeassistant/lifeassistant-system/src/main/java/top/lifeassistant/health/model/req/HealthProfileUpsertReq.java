package top.lifeassistant.health.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "健康档案 upsert 请求")
public class HealthProfileUpsertReq {

    @Schema(description = "展示名")
    private String displayName;

    @Schema(description = "鼓励文案")
    private String motto;

    @Schema(description = "身高 cm")
    private BigDecimal heightCm;

    @Schema(description = "目标体重 kg，可空")
    private BigDecimal targetKg;

    @Schema(description = "静息代谢 kcal")
    private Integer restingKcal;
}
