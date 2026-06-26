package top.lifeassistant.todo.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建确认模板请求")
public class TodoAckTemplateCreateReq {

    @Schema(description = "文案内容")
    @NotBlank
    @Size(max = 100)
    private String content;
}
