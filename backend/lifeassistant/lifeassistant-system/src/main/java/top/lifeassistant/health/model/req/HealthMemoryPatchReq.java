package top.lifeassistant.health.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "健康规律更新请求")
public class HealthMemoryPatchReq {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "详情")
    private String detail;
}
