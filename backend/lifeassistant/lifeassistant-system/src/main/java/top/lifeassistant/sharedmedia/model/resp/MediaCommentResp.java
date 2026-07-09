package top.lifeassistant.sharedmedia.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.sharedmedia.model.entity.MediaCommentDO;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "评论响应")
public class MediaCommentResp {
    private String id;
    private String mediaId;
    private String userId;
    private String content;
    private LocalDateTime createdAt;

    public static MediaCommentResp from(MediaCommentDO comment) {
        return MediaCommentResp.builder()
            .id(comment.getId())
            .mediaId(comment.getMediaId())
            .userId(comment.getUserId())
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .build();
    }
}
