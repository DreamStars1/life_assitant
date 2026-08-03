package top.lifeassistant.health.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("health_daily")
public class HealthDailyDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    @TableField("user_id")
    private String userId;

    @TableField("daily_date")
    private LocalDate dailyDate;

    @TableField("stomach_status")
    private String stomachStatus;

    @TableField("stomach_note")
    private String stomachNote;

    @TableField("cycle_phase")
    private String cyclePhase;

    @TableField("cycle_day")
    private Integer cycleDay;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
