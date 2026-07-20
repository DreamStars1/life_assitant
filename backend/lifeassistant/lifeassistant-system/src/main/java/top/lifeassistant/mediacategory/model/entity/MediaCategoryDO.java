package top.lifeassistant.mediacategory.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import top.lifeassistant.common.base.model.entity.OwnedEntity;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("media_category")
public class MediaCategoryDO implements Serializable, OwnedEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    @TableField("user_id")
    private String userId;

    private String name;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @Override
    public String getOwnerId() {
        return userId;
    }
}
