package top.lifeassistant.health.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.health.model.entity.HealthWeightDO;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Schema(description = "体重记录响应")
public class HealthWeightResp {

    private String id;
    private LocalDate date;
    private String weightType;
    private BigDecimal kg;
    private Boolean standard;

    public static HealthWeightResp from(HealthWeightDO row) {
        return HealthWeightResp.builder()
            .id(row.getId())
            .date(row.getWeightDate())
            .weightType(row.getWeightType())
            .kg(row.getKg())
            .standard(row.getStandard())
            .build();
    }
}
