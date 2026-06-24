package top.lifeassistant.todo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.entity.BaseDO;

import top.lifeassistant.common.base.model.entity.OwnedEntity;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("todo")
public class TodoDO extends BaseDO implements OwnedEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 创建者 */
    private String userId;

    /** 标题 */
    private String title;

    /** 描述 */
    private String description;

    /** 是否完成 */
    private Boolean isCompleted;

    /** 优先级：low/medium/high/urgent */
    private String priority;

    /** 分类标签 */
    private String category;

    /** 截止时间 */
    private LocalDateTime dueDate;

    /** 被分配者 */
    private String assignedTo;

    /** 分配者 */
    private String assignedBy;

    /** 确认状态：none/unconfirmed/confirmed */
    private String ackStatus;

    /** 确认回复文案 */
    private String ackMessage;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 取消时间 */
    private LocalDateTime cancelledAt;

    @Override
    public String getOwnerId() {
        return userId;
    }
}
