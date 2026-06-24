package top.lifeassistant.todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.lifeassistant.todo.model.entity.TodoAckTemplateDO;

/**
 * 确认文案模板 Mapper
 *
 * @author DreamStars1
 * @since 2026/6/24
 */
@Mapper
public interface TodoAckTemplateMapper extends BaseMapper<TodoAckTemplateDO> {
}
