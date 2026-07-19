package top.lifeassistant.schedule.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日程邀约记录。
 * 不继承 BaseDO（该表无 update_time / create_by / update_by）。
 */
@Data
@TableName("schedule_invite")
public class ScheduleInviteDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    /** 源事件 ID */
    @TableField("source_event_id")
    private String sourceEventId;

    /** 邀请人用户 ID */
    @TableField("inviter_user_id")
    private String inviterUserId;

    /** 被邀请人用户 ID */
    @TableField("invitee_user_id")
    private String inviteeUserId;

    /** 状态：pending/accepted/rejected */
    private String status;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 响应时间 */
    @TableField("responded_at")
    private LocalDateTime respondedAt;
}
