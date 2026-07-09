package top.lifeassistant.sharedmedia.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新共享媒体请求")
public class SharedMediaUpdateReq {
    @Schema(description = "名称")
    private String title;
    @Schema(description = "类型: movie/book/tv")
    private String mediaType;
    @Schema(description = "简介")
    private String description;
}
