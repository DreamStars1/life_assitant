package top.lifeassistant.sharedmedia.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.sharedmedia.model.entity.SharedMediaDO;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "共享媒体响应")
public class SharedMediaResp {
    private String id;
    private String createdBy;
    private String title;
    private String mediaType;
    private String coverPath;
    private String description;
    private Boolean isFinished;
    private LocalDateTime createdAt;
    private LocalDateTime updateTime;

    public static SharedMediaResp from(SharedMediaDO media) {
        return SharedMediaResp.builder()
            .id(media.getId())
            .createdBy(media.getCreatedBy())
            .title(media.getTitle())
            .mediaType(media.getMediaType())
            .coverPath(media.getCoverPath())
            .description(media.getDescription())
            .isFinished(media.getIsFinished())
            .createdAt(media.getCreatedAt())
            .updateTime(media.getUpdateTime())
            .build();
    }
}
