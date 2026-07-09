package top.lifeassistant.sharedmedia.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建评论请求")
public class MediaCommentCreateReq {
    @NotBlank
    @Schema(description = "评论内容")
    private String content;
}
