package top.lifeassistant.health.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.health.model.entity.HealthMealDO;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@Schema(description = "饮食记录响应")
public class HealthMealResp {

    private String id;
    private LocalDate date;
    private String mealType;
    private String food;
    private BigDecimal proteinG;
    private String feedback;

    public static HealthMealResp from(HealthMealDO row) {
        return HealthMealResp.builder()
            .id(row.getId())
            .date(row.getMealDate())
            .mealType(row.getMealType())
            .food(row.getFood())
            .proteinG(row.getProteinG())
            .feedback(row.getFeedback())
            .build();
    }
}
