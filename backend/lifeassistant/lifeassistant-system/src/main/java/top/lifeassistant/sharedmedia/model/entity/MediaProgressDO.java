package top.lifeassistant.sharedmedia.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 媒体进度。每对 (media_id, user_id) 一条记录，
 * user_id IS NULL 表示共同进度（scope=shared）。
 * 不继承 BaseDO（该表无审计字段）。
 */
@Data
@TableName("media_progress")
public class MediaProgressDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    /** 关联媒体 ID */
    private String mediaId;

    /** 用户 ID（共享进度时 user_id IS NULL） */
    private String userId;

    /** 进度文本描述 */
    private String progressText;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
