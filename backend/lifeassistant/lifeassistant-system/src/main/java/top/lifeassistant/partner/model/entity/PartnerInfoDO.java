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
 * 伴侣关系属性（一对一行）。
 * 不继承 BaseDO（该表无 create_by）。
 */
@Data
@TableName("partner_info")
public class PartnerInfoDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    /** 字典序较小的用户 ID */
    @TableField("user_a_id")
    private String userAId;

    /** 字典序较大的用户 ID */
    @TableField("user_b_id")
    private String userBId;

    /** 在一起起点 */
    @TableField("partner_since")
    private LocalDate partnerSince;

    /** 积分余额缓存 */
    @TableField("points_balance")
    private Integer pointsBalance;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
