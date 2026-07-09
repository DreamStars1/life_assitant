package top.lifeassistant.sharedmedia.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.sharedmedia.model.entity.MediaProgressDO;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "进度响应")
public class MediaProgressResp {
    private String id;
    private String mediaId;
    private String userId;
    private String scope;
    private String progressText;
    private LocalDateTime createdAt;

    public static MediaProgressResp from(MediaProgressDO p) {
        return MediaProgressResp.builder()
            .id(p.getId())
            .mediaId(p.getMediaId())
            .userId(p.getUserId())
            .scope(p.getUserId() == null ? "shared" : "personal")
            .progressText(p.getProgressText())
            .createdAt(p.getCreatedAt())
            .build();
    }
}
