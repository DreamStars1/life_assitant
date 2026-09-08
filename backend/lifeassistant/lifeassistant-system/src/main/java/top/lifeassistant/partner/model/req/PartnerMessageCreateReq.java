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

    /** 同步「一起做过的事」时的标题（可选，缺省取留言正文） */
    private String sharedRecordTitle;

    /** 同步「一起做过的事」时的正文（可选） */
    private String sharedRecordContent;

    private Boolean publishTodo;

    /** self | partner | none */
    private String todoAssignedTo;

    private Boolean publishPoints;

    private Integer pointsChange;

    private String pointsReason;
}
