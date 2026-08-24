package top.lifeassistant.partner.model.resp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.partner.model.entity.PartnerMessageDO;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "伴侣留言响应")
public class PartnerMessageResp {

    private String id;

    private String createdBy;

    private String content;

    private List<String> imageUrls;

    private LocalDateTime createdAt;

    private String sharedRecordId;

    private String todoId;

    private String pointsId;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static PartnerMessageResp from(PartnerMessageDO message) {
        return PartnerMessageResp.builder()
            .id(message.getId())
            .createdBy(message.getCreatedBy())
            .content(message.getContent())
            .imageUrls(parseImageUrls(message.getImageUrls()))
            .createdAt(message.getCreatedAt())
            .sharedRecordId(message.getSharedRecordId())
            .todoId(message.getTodoId())
            .pointsId(message.getPointsId())
            .build();
    }

    private static List<String> parseImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> urls = MAPPER.readValue(imageUrlsJson, new TypeReference<List<String>>() {});
            return urls == null ? List.of() : urls;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
