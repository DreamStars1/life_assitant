package top.lifeassistant.todo.model.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "排序确认模板请求")
public class TodoAckTemplateReorderReq {

    @Schema(description = "按顺序排列的模板 ID 列表")
    @NotEmpty
    private List<String> ids;
}
