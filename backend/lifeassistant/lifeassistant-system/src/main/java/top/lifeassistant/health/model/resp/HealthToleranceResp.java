package top.lifeassistant.health.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.health.model.entity.HealthToleranceDO;

@Data
@Builder
@Schema(description = "耐受记录响应")
public class HealthToleranceResp {

    private String id;
    private String name;
    private String level;

    public static HealthToleranceResp from(HealthToleranceDO row) {
        return HealthToleranceResp.builder()
            .id(row.getId())
            .name(row.getName())
            .level(row.getLevel())
            .build();
    }
}
