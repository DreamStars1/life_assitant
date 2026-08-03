package top.lifeassistant.health.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "触发因素创建请求")
public class HealthTriggerCreateReq {

    @NotBlank
    @Schema(description = "名称")
    private String name;

    @Schema(description = "备注，可空")
    private String note;

    @NotNull
    @Min(1)
    @Max(5)
    @Schema(description = "星级 1-5")
    private Integer stars;
}
