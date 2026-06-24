package top.lifeassistant.todo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.base.component.OwnerValidator;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.todo.mapper.TodoAckTemplateMapper;
import top.lifeassistant.todo.model.entity.TodoAckTemplateDO;
import top.lifeassistant.todo.model.resp.TodoAckTemplateResp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TodoAckTemplateService {

    private static final int MAX_TEMPLATES = 5;
    private static final List<String> DEFAULT_TEMPLATES = List.of("收到", "朕知道了", "臣遵旨");

    private final TodoAckTemplateMapper mapper;
    private final OwnerValidator ownerValidator;

    public List<TodoAckTemplateResp> listByUser(UserDO user) {
        List<TodoAckTemplateDO> templates = mapper.selectList(
            new LambdaQueryWrapper<TodoAckTemplateDO>()
                .eq(TodoAckTemplateDO::getUserId, user.getId())
                .orderByAsc(TodoAckTemplateDO::getSortOrder));

        if (templates.isEmpty()) {
            templates = initDefaults(user);
        }

        return templates.stream().map(TodoAckTemplateResp::from).toList();
    }

    private List<TodoAckTemplateDO> initDefaults(UserDO user) {
        List<TodoAckTemplateDO> defaults = IntStream.range(0, DEFAULT_TEMPLATES.size())
            .mapToObj(i -> {
                TodoAckTemplateDO t = new TodoAckTemplateDO();
                t.setUserId(user.getId());
                t.setContent(DEFAULT_TEMPLATES.get(i));
                t.setSortOrder(i);
                t.setCreatedAt(LocalDateTime.now());
                t.setUpdateTime(LocalDateTime.now());
                mapper.insert(t);
                return t;
            })
            .toList();
        return defaults;
    }

    public TodoAckTemplateResp create(UserDO user, String content) {
        long count = mapper.selectCount(
            new LambdaQueryWrapper<TodoAckTemplateDO>().eq(TodoAckTemplateDO::getUserId, user.getId()));
        if (count >= MAX_TEMPLATES) {
            throw new BadRequestException("模板最多 " + MAX_TEMPLATES + " 条");
        }

        // 修复：selectOne 在无数据时返回 null，Optional 兜底
        int maxOrder = Optional.ofNullable(
            mapper.selectOne(new LambdaQueryWrapper<TodoAckTemplateDO>()
                .eq(TodoAckTemplateDO::getUserId, user.getId())
                .orderByDesc(TodoAckTemplateDO::getSortOrder)
                .last("LIMIT 1")))
            .map(TodoAckTemplateDO::getSortOrder)
            .orElse(-1);

        TodoAckTemplateDO template = new TodoAckTemplateDO();
        template.setUserId(user.getId());
        template.setContent(content);
        template.setSortOrder(maxOrder + 1);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        mapper.insert(template);
        return TodoAckTemplateResp.from(template);
    }

    public TodoAckTemplateResp update(UserDO user, String id, String content) {
        TodoAckTemplateDO template = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        template.setContent(content);
        template.setUpdateTime(LocalDateTime.now());
        mapper.updateById(template);
        return TodoAckTemplateResp.from(template);
    }

    public void delete(UserDO user, String id) {
        ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        mapper.deleteById(id);
    }

    public void reorder(UserDO user, List<String> ids) {
        for (int i = 0; i < ids.size(); i++) {
            final int order = i;
            TodoAckTemplateDO template = ownerValidator.requireOwner(
                () -> mapper.selectById(ids.get(order)), user.getId());
            template.setSortOrder(order);
            template.setUpdateTime(LocalDateTime.now());
            mapper.updateById(template);
        }
    }
}
