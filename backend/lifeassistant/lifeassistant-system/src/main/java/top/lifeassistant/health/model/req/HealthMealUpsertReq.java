package top.lifeassistant.health.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "饮食记录 upsert 请求")
public class HealthMealUpsertReq {

    @NotNull
    @Schema(description = "日期 yyyy-MM-dd")
    private LocalDate date;

    @NotBlank
    @Schema(description = "餐次：早餐/午餐/晚餐")
    private String mealType;

    @NotBlank
    @Schema(description = "食物")
    private String food;

    @Schema(description = "蛋白质 g，可空")
    private BigDecimal proteinG;

    @Schema(description = "热量 kcal，可空")
    private Integer kcal;

    @Schema(description = "反馈，可空")
    private String feedback;
}
