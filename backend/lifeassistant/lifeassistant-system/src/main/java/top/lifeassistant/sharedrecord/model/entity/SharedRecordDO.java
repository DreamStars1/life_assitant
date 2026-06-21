package top.lifeassistant.sharedrecord.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.entity.BaseDO;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shared_record")
public class SharedRecordDO extends BaseDO {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录者 */
    private String createdBy;

    /** 标题 */
    private String title;

    /** 详细描述 */
    private String content;

    /** 事件发生时间 */
    private LocalDateTime occurredAt;
}
