package top.lifeassistant.sharedmedia.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "更新进度请求")
public class MediaProgressUpdateReq {
    @NotBlank
    @Schema(description = "范围: shared=共同, personal=个人")
    private String scope;

    @NotBlank
    @Schema(description = "进度文本，如 '第5集/共24集'")
    private String progressText;
}
