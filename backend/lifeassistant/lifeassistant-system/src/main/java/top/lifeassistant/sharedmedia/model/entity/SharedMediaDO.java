package top.lifeassistant.sharedmedia.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.entity.BaseDO;
import top.lifeassistant.common.base.model.entity.OwnedEntity;

import java.io.Serial;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("shared_media")
public class SharedMediaDO extends BaseDO implements OwnedEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 记录者 */
    private String createdBy;

    /** 标题 */
    private String title;

    /** 媒体类型（如 movie, book, anime） */
    private String mediaType;

    /** 封面路径 */
    private String coverPath;

    /** 详细描述 */
    private String description;

    /** 是否已完结 */
    private Boolean isFinished;

    /** 看完时间 */
    private LocalDateTime finishedAt;

    @Override
    public String getOwnerId() {
        return createdBy;
    }
}
