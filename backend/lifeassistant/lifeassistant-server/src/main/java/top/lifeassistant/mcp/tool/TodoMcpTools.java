package top.lifeassistant.mcp.tool;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.McpTool;
import org.springframework.ai.tool.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import top.lifeassistant.todo.model.req.TodoCreateReq;
import top.lifeassistant.todo.model.req.TodoUpdateReq;
import top.lifeassistant.todo.model.resp.TodoResp;
import top.lifeassistant.todo.service.TodoService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TodoMcpTools {

    private final TodoService todoService;
    private final HttpServletRequest request;

    private UserDO getCurrentUser() {
        return (UserDO) request.getAttribute("currentUser");
    }

    @McpTool(description = "创建待办事项")
    public TodoResp todo_create(
            @McpToolParam(description = "标题") String title,
            @McpToolParam(description = "详细描述（可选）") String description,
            @McpToolParam(description = "优先级：low / medium / high / urgent（可选，默认 medium）") String priority,
            @McpToolParam(description = "截止日期（ISO 格式，可选）") String dueDate,
            @McpToolParam(description = "是否自动指派给伴侣（可选，默认 false）") Boolean assignToPartner) {

        UserDO user = getCurrentUser();
        TodoCreateReq req = new TodoCreateReq();
        req.setTitle(title);
        req.setDescription(description);
        req.setPriority(priority != null ? priority : "medium");
        if (dueDate != null) req.setDueDate(LocalDateTime.parse(dueDate));
        if (Boolean.TRUE.equals(assignToPartner) && user.getPartnerId() != null) {
            req.setAssignedTo(user.getPartnerId());
        }
        return todoService.create(user, req);
    }

    @McpTool(description = "查询待办列表，支持筛选")
    public List<TodoResp> todo_list(
            @McpToolParam(description = "是否已完成（可选）") Boolean isCompleted,
            @McpToolParam(description = "优先级筛选：low / medium / high / urgent（可选）") String priority,
            @McpToolParam(description = "截止日期范围起始（ISO 格式，可选）") String startDueDate,
            @McpToolParam(description = "截止日期范围结束（ISO 格式，可选）") String endDueDate) {

        UserDO user = getCurrentUser();
        LocalDateTime start = startDueDate != null ? LocalDateTime.parse(startDueDate) : null;
        LocalDateTime end = endDueDate != null ? LocalDateTime.parse(endDueDate) : null;
        return todoService.list(user, isCompleted, priority, start, end);
    }

    @McpTool(description = "获取首页最近的未完成待办")
    public List<TodoResp> todo_upcoming() {
        return todoService.getUpcoming(getCurrentUser());
    }

    @McpTool(description = "获取待办详情")
    public TodoResp todo_get(@McpToolParam(description = "待办 ID") String id) {
        return todoService.getById(getCurrentUser(), id);
    }

    @McpTool(description = "更新待办事项")
    public TodoResp todo_update(
            @McpToolParam(description = "待办 ID") String id,
            @McpToolParam(description = "新标题（可选）") String title,
            @McpToolParam(description = "新详细描述（可选）") String description,
            @McpToolParam(description = "新优先级（可选）") String priority,
            @McpToolParam(description = "新截止日期 ISO 格式（可选）") String dueDate) {

        UserDO user = getCurrentUser();
        TodoUpdateReq req = new TodoUpdateReq();
        req.setTitle(title);
        req.setDescription(description);
        req.setPriority(priority);
        if (dueDate != null) req.setDueDate(LocalDateTime.parse(dueDate));
        return todoService.update(user, id, req);
    }

    @McpTool(description = "切换待办完成状态")
    public TodoResp todo_toggle(@McpToolParam(description = "待办 ID") String id) {
        return todoService.toggleComplete(getCurrentUser(), id);
    }

    @McpTool(description = "确认收到待办（仅被指派者可操作）")
    public TodoResp todo_acknowledge(
            @McpToolParam(description = "待办 ID") String id,
            @McpToolParam(description = "确认回复文案（可选）") String message) {
        return todoService.acknowledge(getCurrentUser(), id, message);
    }
}
