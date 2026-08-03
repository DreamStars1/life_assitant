package top.lifeassistant.health.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.health.model.entity.HealthTriggerDO;

@Data
@Builder
@Schema(description = "触发因素响应")
public class HealthTriggerResp {

    private String id;
    private String name;
    private String note;
    private Integer stars;

    public static HealthTriggerResp from(HealthTriggerDO row) {
        return HealthTriggerResp.builder()
            .id(row.getId())
            .name(row.getName())
            .note(row.getNote())
            .stars(row.getStars())
            .build();
    }
}
