package top.lifeassistant.todo.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新确认模板请求")
public class TodoAckTemplateUpdateReq {

    @Schema(description = "文案内容")
    @NotBlank
    @Size(max = 100)
    private String content;
}
