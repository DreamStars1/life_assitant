package top.lifeassistant.sharedmedia.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.sharedmedia.model.entity.MediaProgressEventDO;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "进度变更事件响应")
public class MediaProgressEventResp {
    private String id;
    private String mediaId;
    private String userId;
    private String scope;
    private String progressText;
    private LocalDateTime createdAt;

    public static MediaProgressEventResp from(MediaProgressEventDO e) {
        return MediaProgressEventResp.builder()
            .id(e.getId())
            .mediaId(e.getMediaId())
            .userId(e.getUserId())
            .scope(e.getScope())
            .progressText(e.getProgressText())
            .createdAt(e.getCreatedAt())
            .build();
    }
}
