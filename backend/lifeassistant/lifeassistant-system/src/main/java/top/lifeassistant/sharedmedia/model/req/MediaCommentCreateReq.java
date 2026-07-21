package top.lifeassistant.sharedmedia.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建评论请求")
public class MediaCommentCreateReq {
    @Schema(description = "评论文字；纯图消息可空")
    private String content;

    @Schema(description = "图片相对路径列表，最多 9 张")
    private List<String> imageUrls;
}
