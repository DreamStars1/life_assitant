package top.lifeassistant.partner.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 伴侣积分变动记录。
 * 不继承 BaseDO（该表无审计字段）。
 */
@Data
@TableName("partner_points")
public class PartnerPointsDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    /** 变动人用户 ID */
    @TableField("created_by")
    private String createdBy;

    /** 积分变动值（正为增加，负为扣减） */
    @TableField("points_change")
    private Integer pointsChange;

    /** 变动原因 */
    private String reason;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
