package top.lifeassistant.todo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.base.component.OwnerValidator;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.todo.mapper.TodoMapper;
import top.lifeassistant.todo.model.entity.TodoDO;
import top.lifeassistant.todo.model.req.TodoCreateReq;
import top.lifeassistant.todo.model.req.TodoUpdateReq;
import top.lifeassistant.todo.model.resp.TodoResp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待办业务
 *
 * @author DreamStars1
 * @since 2026/6/24
 */
@Service
@RequiredArgsConstructor
public class TodoService {

    private final TodoMapper mapper;
    private final OwnerValidator ownerValidator;

    public TodoResp create(UserDO user, TodoCreateReq req) {
        TodoDO todo = new TodoDO();
        todo.setUserId(user.getId());
        todo.setTitle(req.getTitle());
        todo.setDescription(req.getDescription());
        todo.setPriority(req.getPriority());
        todo.setIsCompleted(false);
        todo.setDueDate(req.getDueDate());
        todo.setCreatedAt(LocalDateTime.now());
        todo.setUpdateTime(LocalDateTime.now());

        if (req.getAssignedTo() != null) {
            if (user.getPartnerId() == null) {
                throw new BadRequestException("请先绑定伴侣");
            }
            todo.setAssignedTo(req.getAssignedTo());
            todo.setAssignedBy(user.getId());
            todo.setAckStatus("unconfirmed");
        } else {
            todo.setAckStatus("none");
        }

        mapper.insert(todo);
        return TodoResp.from(todo);
    }

    public List<TodoResp> list(UserDO user, Boolean isCompleted, String priority,
                                LocalDateTime startDueDate, LocalDateTime endDueDate) {
        LambdaQueryWrapper<TodoDO> qw = new LambdaQueryWrapper<>();
        qw.and(w -> w.eq(TodoDO::getUserId, user.getId())
                     .or().eq(TodoDO::getAssignedTo, user.getId()));
        if (isCompleted != null) qw.eq(TodoDO::getIsCompleted, isCompleted);
        if (priority != null) qw.eq(TodoDO::getPriority, priority);
        if (startDueDate != null) qw.ge(TodoDO::getDueDate, startDueDate);
        if (endDueDate != null) qw.le(TodoDO::getDueDate, endDueDate);
        qw.orderByDesc(TodoDO::getCreatedAt);
        return mapper.selectList(qw).stream().map(TodoResp::from).toList();
    }

    public TodoResp getById(UserDO user, String id) {
        TodoDO todo = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "资源不存在",
            t -> user.getId().equals(t.getUserId()) || user.getId().equals(t.getAssignedTo()));
        return TodoResp.from(todo);
    }

    public TodoResp update(UserDO user, String id, TodoUpdateReq req) {
        TodoDO todo = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        if (req.getTitle() != null) todo.setTitle(req.getTitle());
        if (req.getDescription() != null) todo.setDescription(req.getDescription());
        if (req.getPriority() != null) todo.setPriority(req.getPriority());
        if (req.getDueDate() != null) todo.setDueDate(req.getDueDate());
        todo.setUpdateTime(LocalDateTime.now());
        mapper.updateById(todo);
        return TodoResp.from(todo);
    }

    public void delete(UserDO user, String id) {
        ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        mapper.deleteById(id);
    }

    public TodoResp toggleComplete(UserDO user, String id) {
        TodoDO todo = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "资源不存在",
            t -> user.getId().equals(t.getUserId()) || user.getId().equals(t.getAssignedTo()));
        todo.setIsCompleted(!Boolean.TRUE.equals(todo.getIsCompleted()));
        todo.setCompletedAt(Boolean.TRUE.equals(todo.getIsCompleted()) ? LocalDateTime.now() : null);
        todo.setUpdateTime(LocalDateTime.now());
        mapper.updateById(todo);
        return TodoResp.from(todo);
    }

    public TodoResp acknowledge(UserDO user, String id, String message) {
        TodoDO todo = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "只有被指派者才能确认",
            t -> user.getId().equals(t.getAssignedTo()));
        if (!"unconfirmed".equals(todo.getAckStatus())) {
            throw new BadRequestException("待办无需确认或已确认");
        }
        todo.setAckStatus("confirmed");
        todo.setAckMessage(message);
        todo.setUpdateTime(LocalDateTime.now());
        mapper.updateById(todo);
        return TodoResp.from(todo);
    }

    public List<TodoResp> getUpcoming(UserDO user) {
        List<TodoDO> todos = mapper.selectIncompleteByDueDate(user.getId());
        return todos.stream().limit(3).map(TodoResp::from).toList();
    }
}
