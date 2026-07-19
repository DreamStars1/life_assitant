package top.lifeassistant.schedule.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.entity.BaseDO;
import top.lifeassistant.common.base.model.entity.OwnedEntity;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("schedule_event")
public class ScheduleEventDO extends BaseDO implements OwnedEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属用户 */
    private String userId;

    /** 标题 */
    private String title;

    /** 开始时间 */
    private LocalDateTime startAt;

    /** 结束时间 */
    private LocalDateTime endAt;

    /** 备注 */
    private String note;

    /** 重复规则：none/daily/weekly */
    private String recurrence;

    /** 重复结束日期 */
    private LocalDate recurrenceEndDate;

    /** 关联事件 ID（邀约 accept 后互指） */
    private String linkedEventId;

    @Override
    public String getOwnerId() {
        return userId;
    }
}
