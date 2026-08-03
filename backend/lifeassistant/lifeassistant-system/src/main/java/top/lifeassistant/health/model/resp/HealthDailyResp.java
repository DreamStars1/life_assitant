package top.lifeassistant.health.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.lifeassistant.health.model.entity.HealthDailyDO;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "每日状态响应")
public class HealthDailyResp {

    private LocalDate date;
    private String stomachStatus;
    private String stomachNote;
    private String cyclePhase;
    private Integer cycleDay;

    public static HealthDailyResp from(HealthDailyDO row) {
        return from(row, row != null ? row.getDailyDate() : null);
    }

    public static HealthDailyResp from(HealthDailyDO row, LocalDate date) {
        if (row == null) {
            return HealthDailyResp.builder().date(date).build();
        }
        return HealthDailyResp.builder()
            .date(row.getDailyDate() != null ? row.getDailyDate() : date)
            .stomachStatus(row.getStomachStatus())
            .stomachNote(row.getStomachNote())
            .cyclePhase(row.getCyclePhase())
            .cycleDay(row.getCycleDay())
            .build();
    }
}
