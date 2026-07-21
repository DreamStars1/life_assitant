package top.lifeassistant.sharedmedia.model.resp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.sharedmedia.model.entity.MediaCommentDO;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "评论响应")
public class MediaCommentResp {
    private String id;
    private String mediaId;
    private String userId;
    private String content;
    private List<String> imageUrls;
    private LocalDateTime createdAt;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static MediaCommentResp from(MediaCommentDO comment) {
        return MediaCommentResp.builder()
            .id(comment.getId())
            .mediaId(comment.getMediaId())
            .userId(comment.getUserId())
            .content(comment.getContent())
            .imageUrls(parseImageUrls(comment.getImageUrls()))
            .createdAt(comment.getCreatedAt())
            .build();
    }

    private static List<String> parseImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(imageUrlsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
