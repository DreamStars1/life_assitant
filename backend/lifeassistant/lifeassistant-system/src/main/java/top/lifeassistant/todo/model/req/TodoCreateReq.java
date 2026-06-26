package top.lifeassistant.todo.model.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "创建待办请求")
public class TodoCreateReq {

    @Schema(description = "标题")
    @NotBlank
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "优先级：low/medium/high/urgent")
    @NotBlank
    private String priority;

    @Schema(description = "截止时间")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dueDate;

    @Schema(description = "被分配者 ID（开启「交给 TA」时传入伴侣 ID）")
    private String assignedTo;
}
