package top.lifeassistant.schedule.service;

import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.schedule.model.entity.ScheduleEventDO;
import top.lifeassistant.schedule.model.resp.ScheduleEventResp;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** 按查询区间展开日程重复实例（纯逻辑，无 DB）。 */
public final class ScheduleRecurrenceExpander {

    private static final int MAX_INSTANCES = 400;

    private ScheduleRecurrenceExpander() {}

    public static List<ScheduleEventResp> expand(ScheduleEventDO event, LocalDateTime from, LocalDateTime to) {
        String recurrence = event.getRecurrence();
        if (recurrence == null || "none".equals(recurrence)) {
            return expandNone(event, from, to);
        }
        if ("daily".equals(recurrence)) {
            return expandRecurring(event, from, to, 1);
        }
        if ("weekly".equals(recurrence)) {
            return expandRecurring(event, from, to, 7);
        }
        throw new BadRequestException("不支持的重复规则: " + recurrence);
    }

    private static List<ScheduleEventResp> expandNone(ScheduleEventDO event, LocalDateTime from, LocalDateTime to) {
        if (!intersects(event.getStartAt(), event.getEndAt(), from, to)) {
            return List.of();
        }
        return List.of(toResp(event, event.getStartAt(), event.getEndAt()));
    }

    private static List<ScheduleEventResp> expandRecurring(
        ScheduleEventDO event, LocalDateTime from, LocalDateTime to, int stepDays
    ) {
        List<ScheduleEventResp> result = new ArrayList<>();
        Duration duration = Duration.between(event.getStartAt(), event.getEndAt());
        LocalDateTime instanceStart = event.getStartAt();
        LocalDate recurrenceEnd = event.getRecurrenceEndDate();
        LocalDateTime recurrenceEndAt = recurrenceEnd != null
            ? recurrenceEnd.atTime(LocalTime.of(23, 59, 59))
            : null;

        while (true) {
            if (recurrenceEndAt != null && instanceStart.isAfter(recurrenceEndAt)) {
                break;
            }
            if (!instanceStart.isBefore(to)) {
                break;
            }

            LocalDateTime instanceEnd = instanceStart.plus(duration);
            if (intersects(instanceStart, instanceEnd, from, to)) {
                result.add(toResp(event, instanceStart, instanceEnd));
                if (result.size() > MAX_INSTANCES) {
                    throw new BadRequestException("展开实例过多");
                }
            }

            instanceStart = instanceStart.plusDays(stepDays);
        }

        return result;
    }

    private static boolean intersects(LocalDateTime start, LocalDateTime end, LocalDateTime from, LocalDateTime to) {
        return start.isBefore(to) && end.isAfter(from);
    }

    private static ScheduleEventResp toResp(ScheduleEventDO event, LocalDateTime start, LocalDateTime end) {
        ScheduleEventResp resp = ScheduleEventResp.from(event);
        resp.setStartAt(start);
        resp.setEndAt(end);
        resp.setInstanceKey(event.getId() + "|" + start);
        return resp;
    }

    public static void main(String[] args) {
        ScheduleEventDO e = new ScheduleEventDO();
        e.setId("e1");
        e.setTitle("standup");
        e.setStartAt(LocalDateTime.of(2026, 7, 1, 9, 0));
        e.setEndAt(LocalDateTime.of(2026, 7, 1, 9, 30));
        e.setRecurrence("daily");
        e.setRecurrenceEndDate(LocalDate.of(2026, 7, 3));
        var list = expand(e, LocalDateTime.of(2026, 7, 1, 0, 0), LocalDateTime.of(2026, 7, 5, 0, 0));
        if (list.size() != 3) {
            throw new AssertionError("expected 3 got " + list.size());
        }
        System.out.println("ScheduleRecurrenceExpander OK");
    }
}
