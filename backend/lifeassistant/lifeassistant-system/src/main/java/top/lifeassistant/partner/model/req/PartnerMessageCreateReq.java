package top.lifeassistant.partner.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建伴侣留言")
public class PartnerMessageCreateReq {

    private String content;

    private List<String> imageUrls;

    private Boolean publishSharedRecord;

    private Boolean publishTodo;

    /** self | partner | none */
    private String todoAssignedTo;

    private Boolean publishPoints;

    private Integer pointsChange;

    private String pointsReason;
}
