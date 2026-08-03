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
        if (row == null) {
            return new HealthDailyResp();
        }
        return HealthDailyResp.builder()
            .date(row.getDailyDate())
            .stomachStatus(row.getStomachStatus())
            .stomachNote(row.getStomachNote())
            .cyclePhase(row.getCyclePhase())
            .cycleDay(row.getCycleDay())
            .build();
    }
}
