package top.lifeassistant.sharedmedia.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("media_progress_event")
public class MediaProgressEventDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    private String mediaId;

    private String userId;

    private String scope;

    private String progressText;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
