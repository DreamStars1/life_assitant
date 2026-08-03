package top.lifeassistant.health.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "体重记录创建请求")
public class HealthWeightCreateReq {

    @NotNull
    @Schema(description = "日期 yyyy-MM-dd")
    private LocalDate date;

    @NotBlank
    @Schema(description = "类型：晨重/晚重")
    private String weightType;

    @NotNull
    @Schema(description = "体重 kg")
    private BigDecimal kg;

    @Schema(description = "是否纳入趋势，默认 false")
    private Boolean standard;
}
