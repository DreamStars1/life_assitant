package top.lifeassistant.sharedmedia.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建共享媒体请求")
public class SharedMediaCreateReq {
    @Schema(description = "名称")
    private String title;
    @Schema(description = "类型: movie/book/tv")
    private String mediaType;
    @Schema(description = "简介")
    private String description;
    @Schema(description = "上次一起看日期 yyyy-MM-dd，可空")
    private String lastWatchedAt;
    @Schema(description = "是否仅自己可见，默认 false")
    private Boolean isPrivate;
}
