package top.lifeassistant.sharedrecord.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "共享记录响应")
public class SharedRecordResp {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "记录者 ID")
    private String createdBy;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "详细描述")
    private String content;

    @Schema(description = "事件发生时间")
    private LocalDateTime occurredAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public static SharedRecordResp from(top.lifeassistant.sharedrecord.model.entity.SharedRecordDO record) {
        return SharedRecordResp.builder()
            .id(record.getId())
            .createdBy(record.getCreatedBy())
            .title(record.getTitle())
            .content(record.getContent())
            .occurredAt(record.getOccurredAt())
            .createdAt(record.getCreatedAt())
            .updateTime(record.getUpdateTime())
            .build();
    }
}
