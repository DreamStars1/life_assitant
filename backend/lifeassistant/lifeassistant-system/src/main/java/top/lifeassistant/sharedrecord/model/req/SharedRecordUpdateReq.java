package top.lifeassistant.sharedrecord.model.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "更新共享记录请求")
public class SharedRecordUpdateReq {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "详细描述")
    private String content;

    @Schema(description = "事件发生时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime occurredAt;
}
