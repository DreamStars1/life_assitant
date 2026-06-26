package top.lifeassistant.todo.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "确认模板响应")
public class TodoAckTemplateResp {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "文案内容")
    private String content;

    @Schema(description = "排序顺序")
    private Integer sortOrder;

    public static TodoAckTemplateResp from(top.lifeassistant.todo.model.entity.TodoAckTemplateDO template) {
        return TodoAckTemplateResp.builder()
            .id(template.getId())
            .content(template.getContent())
            .sortOrder(template.getSortOrder())
            .build();
    }
}
