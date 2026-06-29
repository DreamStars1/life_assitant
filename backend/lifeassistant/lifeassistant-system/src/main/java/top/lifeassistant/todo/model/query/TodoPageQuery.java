package top.lifeassistant.todo.model.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.query.PageQuery;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "待办分页查询参数")
public class TodoPageQuery extends PageQuery {

    @Schema(description = "是否已完成")
    private Boolean isCompleted;

    @Schema(description = "优先级：low/medium/high/urgent")
    private String priority;

    @Schema(description = "截止时间范围（开始）")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDueDate;

    @Schema(description = "截止时间范围（结束）")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDueDate;
}
