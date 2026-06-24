package top.lifeassistant.todo.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.entity.BaseDO;

import top.lifeassistant.common.base.model.entity.OwnedEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("todo_ack_template")
public class TodoAckTemplateDO extends BaseDO implements OwnedEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 所属用户 */
    private String userId;

    /** 文案内容 */
    private String content;

    /** 排序顺序 */
    private Integer sortOrder;

    @Override
    public String getOwnerId() {
        return userId;
    }
}
