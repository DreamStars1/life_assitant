package top.lifeassistant.partner.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("partner_message")
public class PartnerMessageDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId
    private String id;

    @TableField("created_by")
    private String createdBy;

    private String content;

    @TableField("image_urls")
    private String imageUrls;

    @TableField("shared_record_id")
    private String sharedRecordId;

    @TableField("todo_id")
    private String todoId;

    @TableField("points_id")
    private String pointsId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
