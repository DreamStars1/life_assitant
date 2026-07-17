package top.lifeassistant.partner.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 伴侣作息打卡记录。
 * 不继承 BaseDO（该表无审计字段）。
 */
@Data
@TableName("partner_checkin")
public class PartnerCheckinDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    /** 打卡用户 ID */
    @TableField("user_id")
    private String userId;

    /** 打卡类型：wake/sleep */
    @TableField("checkin_type")
    private String checkinType;

    /** 打卡时间 */
    @TableField("checkin_time")
    private LocalDateTime checkinTime;

    /** 打卡日期 */
    @TableField("checkin_date")
    private LocalDate checkinDate;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
