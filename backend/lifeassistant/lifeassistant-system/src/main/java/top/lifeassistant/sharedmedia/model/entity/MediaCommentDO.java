package top.lifeassistant.sharedmedia.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 媒体评论。与 shared_media 为多对一关系。
 * 不继承 BaseDO（该表无审计字段）。
 */
@Data
@TableName("media_comment")
public class MediaCommentDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    /** 关联媒体 ID */
    private String mediaId;

    /** 评论人用户 ID */
    private String userId;

    /** 评论内容 */
    private String content;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
