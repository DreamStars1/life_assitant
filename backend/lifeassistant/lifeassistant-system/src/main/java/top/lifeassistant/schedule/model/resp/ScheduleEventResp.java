package top.lifeassistant.schedule.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.schedule.model.entity.ScheduleEventDO;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "日程响应")
public class ScheduleEventResp {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "所属用户 ID")
    private String userId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "开始时间")
    private LocalDateTime startAt;

    @Schema(description = "结束时间")
    private LocalDateTime endAt;

    @Schema(description = "备注")
    private String note;

    @Schema(description = "重复规则：none/daily/weekly")
    private String recurrence;

    @Schema(description = "重复结束日期")
    private LocalDate recurrenceEndDate;

    @Schema(description = "关联事件 ID")
    private String linkedEventId;

    @Schema(description = "展开实例稳定键（eventId|instanceStart）")
    private String instanceKey;

    @Schema(description = "待处理邀约 ID")
    private String pendingInviteId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public static ScheduleEventResp from(ScheduleEventDO event) {
        return ScheduleEventResp.builder()
            .id(event.getId())
            .userId(event.getUserId())
            .title(event.getTitle())
            .startAt(event.getStartAt())
            .endAt(event.getEndAt())
            .note(event.getNote())
            .recurrence(event.getRecurrence())
            .recurrenceEndDate(event.getRecurrenceEndDate())
            .linkedEventId(event.getLinkedEventId())
            .createdAt(event.getCreatedAt())
            .updateTime(event.getUpdateTime())
            .build();
    }
}
