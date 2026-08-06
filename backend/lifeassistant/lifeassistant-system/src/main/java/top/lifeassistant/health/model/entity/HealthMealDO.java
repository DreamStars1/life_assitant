package top.lifeassistant.health.model.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
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
@TableName("health_meal")
public class HealthMealDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("meal_date")
    private LocalDate mealDate;

    @TableField("meal_type")
    private String mealType;

    private String food;

    @TableField("protein_g")
    private BigDecimal proteinG;

    @TableField(value = "kcal", updateStrategy = FieldStrategy.ALWAYS)
    private Integer kcal;

    private String feedback;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
