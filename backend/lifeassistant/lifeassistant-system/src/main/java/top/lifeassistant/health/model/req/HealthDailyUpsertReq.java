package top.lifeassistant.health.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "每日状态 upsert 请求")
public class HealthDailyUpsertReq {

    @NotNull
    @Schema(description = "日期 yyyy-MM-dd")
    private LocalDate date;

    @Schema(description = "胃状态")
    private String stomachStatus;

    @Schema(description = "胃备注")
    private String stomachNote;

    @Schema(description = "周期阶段")
    private String cyclePhase;

    @Schema(description = "周期第几天")
    private Integer cycleDay;
}
