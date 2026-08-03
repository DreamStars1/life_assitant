package top.lifeassistant.health.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
@Schema(description = "触发因素更新请求")
public class HealthTriggerPatchReq {

    @Schema(description = "名称")
    private String name;

    @Schema(description = "备注")
    private String note;

    @Min(1)
    @Max(5)
    @Schema(description = "星级 1-5")
    private Integer stars;
}
