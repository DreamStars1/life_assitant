package top.lifeassistant.health.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("health_trigger")
public class HealthTriggerDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    @TableField("user_id")
    private String userId;

    private String name;

    private String note;

    private Integer stars;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
