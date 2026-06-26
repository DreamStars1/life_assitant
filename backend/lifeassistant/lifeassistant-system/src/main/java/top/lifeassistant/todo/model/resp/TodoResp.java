package top.lifeassistant.todo.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "待办响应")
public class TodoResp {

    @Schema(description = "ID")
    private String id;

    @Schema(description = "创建者 ID")
    private String userId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "是否完成")
    private Boolean isCompleted;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "截止时间")
    private LocalDateTime dueDate;

    @Schema(description = "被分配者 ID")
    private String assignedTo;

    @Schema(description = "分配者 ID")
    private String assignedBy;

    @Schema(description = "确认状态")
    private String ackStatus;

    @Schema(description = "确认回复文案")
    private String ackMessage;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Schema(description = "取消时间")
    private LocalDateTime cancelledAt;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public static TodoResp from(top.lifeassistant.todo.model.entity.TodoDO todo) {
        return TodoResp.builder()
            .id(todo.getId())
            .userId(todo.getUserId())
            .title(todo.getTitle())
            .description(todo.getDescription())
            .isCompleted(todo.getIsCompleted())
            .priority(todo.getPriority())
            .category(todo.getCategory())
            .dueDate(todo.getDueDate())
            .assignedTo(todo.getAssignedTo())
            .assignedBy(todo.getAssignedBy())
            .ackStatus(todo.getAckStatus())
            .ackMessage(todo.getAckMessage())
            .completedAt(todo.getCompletedAt())
            .cancelledAt(todo.getCancelledAt())
            .createdAt(todo.getCreatedAt())
            .updateTime(todo.getUpdateTime())
            .build();
    }
}
