package top.lifeassistant.mediacategory.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import top.lifeassistant.mediacategory.model.entity.MediaCategoryDO;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "媒体分类响应")
public class MediaCategoryResp {

    private String id;
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updateTime;

    public static MediaCategoryResp from(MediaCategoryDO category) {
        return MediaCategoryResp.builder()
            .id(category.getId())
            .name(category.getName())
            .createdAt(category.getCreatedAt())
            .updateTime(category.getUpdateTime())
            .build();
    }
}
