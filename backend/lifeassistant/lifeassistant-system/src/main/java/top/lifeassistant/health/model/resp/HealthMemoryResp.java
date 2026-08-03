package top.lifeassistant.health.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.health.model.entity.HealthMemoryDO;

@Data
@Builder
@Schema(description = "健康规律响应")
public class HealthMemoryResp {

    private String id;
    private String title;
    private String detail;

    public static HealthMemoryResp from(HealthMemoryDO row) {
        return HealthMemoryResp.builder()
            .id(row.getId())
            .title(row.getTitle())
            .detail(row.getDetail())
            .build();
    }
}
