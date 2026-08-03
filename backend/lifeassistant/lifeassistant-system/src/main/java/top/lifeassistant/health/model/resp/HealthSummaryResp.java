package top.lifeassistant.health.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@Schema(description = "健康概览响应")
public class HealthSummaryResp {

    @Schema(description = "最近晨重 kg")
    private BigDecimal latestMorningKg;

    @Schema(description = "近 7 日标准晨重均值 kg")
    private BigDecimal avg7MorningKg;

    @Schema(description = "目标体重 kg，未设置则为 null")
    private BigDecimal targetKg;

    @Schema(description = "距目标 kg（最近晨重 - 目标），可 null")
    private BigDecimal gapToTargetKg;

    @Schema(description = "近 30 日日均蛋白 g，无蛋白天则为 null")
    private BigDecimal avg30ProteinG;
}
