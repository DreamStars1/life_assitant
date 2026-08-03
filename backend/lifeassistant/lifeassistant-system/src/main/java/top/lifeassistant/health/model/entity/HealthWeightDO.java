package top.lifeassistant.health.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_weight")
public class HealthWeightDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("weight_date")
    private LocalDate weightDate;

    @TableField("weight_type")
    private String weightType;

    private BigDecimal kg;

    private Boolean standard;

    private String note;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
