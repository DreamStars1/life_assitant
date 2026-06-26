package top.lifeassistant.mcp.tool;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.McpTool;
import org.springframework.ai.tool.annotation.McpToolParam;
import org.springframework.stereotype.Component;
import top.lifeassistant.sharedrecord.model.req.SharedRecordCreateReq;
import top.lifeassistant.sharedrecord.model.req.SharedRecordUpdateReq;
import top.lifeassistant.sharedrecord.model.resp.SharedRecordResp;
import top.lifeassistant.sharedrecord.service.SharedRecordService;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SharedRecordMcpTools {

    private final SharedRecordService recordService;
    private final HttpServletRequest request;

    private UserDO getCurrentUser() {
        return (UserDO) request.getAttribute("currentUser");
    }

    @McpTool(description = "记录一起做过的事")
    public SharedRecordResp record_create(
            @McpToolParam(description = "标题") String title,
            @McpToolParam(description = "详细描述（可选）") String content,
            @McpToolParam(description = "事件发生时间 ISO 格式（可选，默认当前时间）") String occurredAt) {

        UserDO user = getCurrentUser();
        SharedRecordCreateReq req = new SharedRecordCreateReq();
        req.setTitle(title);
        req.setContent(content);
        if (occurredAt != null) req.setOccurredAt(LocalDateTime.parse(occurredAt));
        return recordService.create(user, req);
    }

    @McpTool(description = "查询共享记录列表，支持时间范围筛选")
    public List<SharedRecordResp> record_list(
            @McpToolParam(description = "开始时间 ISO 格式（可选）") String start,
            @McpToolParam(description = "结束时间 ISO 格式（可选）") String end) {

        UserDO user = getCurrentUser();
        LocalDateTime startTime = start != null ? LocalDateTime.parse(start) : null;
        LocalDateTime endTime = end != null ? LocalDateTime.parse(end) : null;
        return recordService.list(user, startTime, endTime);
    }

    @McpTool(description = "获取共享记录详情")
    public SharedRecordResp record_get(@McpToolParam(description = "记录 ID") String id) {
        return recordService.getById(getCurrentUser(), id);
    }

    @McpTool(description = "更新共享记录")
    public SharedRecordResp record_update(
            @McpToolParam(description = "记录 ID") String id,
            @McpToolParam(description = "新标题（可选）") String title,
            @McpToolParam(description = "新详细描述（可选）") String content,
            @McpToolParam(description = "新事件发生时间 ISO 格式（可选）") String occurredAt) {

        UserDO user = getCurrentUser();
        SharedRecordUpdateReq req = new SharedRecordUpdateReq();
        req.setTitle(title);
        req.setContent(content);
        if (occurredAt != null) req.setOccurredAt(LocalDateTime.parse(occurredAt));
        return recordService.update(user, id, req);
    }
}
