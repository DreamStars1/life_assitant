package top.lifeassistant.todo.model.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "更新待办请求")
public class TodoUpdateReq {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "优先级：low/medium/high/urgent")
    private String priority;

    @Schema(description = "截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDate;

    @Schema(description = "指派给（伴侣用户 ID）；传空串表示取消指派。仅创建者且对方未确认前可改")
    private String assignedTo;
}
