package top.lifeassistant.schedule.model.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "创建日程请求")
public class ScheduleEventCreateReq {

    @Schema(description = "标题")
    @NotBlank
    private String title;

    @Schema(description = "开始时间")
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startAt;

    @Schema(description = "结束时间")
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endAt;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "重复规则：none/daily/weekly")
    private String recurrence = "none";

    @Schema(description = "重复结束日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate recurrenceEndDate;
}
