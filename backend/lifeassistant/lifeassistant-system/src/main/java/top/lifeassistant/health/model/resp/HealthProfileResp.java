package top.lifeassistant.health.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.lifeassistant.health.model.entity.HealthProfileDO;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "健康档案响应")
public class HealthProfileResp {

    private String displayName;
    private String motto;
    private BigDecimal heightCm;
    private BigDecimal targetKg;
    private Integer restingKcal;

    public static HealthProfileResp from(HealthProfileDO row) {
        if (row == null) {
            return new HealthProfileResp();
        }
        return HealthProfileResp.builder()
            .displayName(row.getDisplayName())
            .motto(row.getMotto())
            .heightCm(row.getHeightCm())
            .targetKg(row.getTargetKg())
            .restingKcal(row.getRestingKcal())
            .build();
    }
}
