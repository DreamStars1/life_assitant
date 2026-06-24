package top.lifeassistant.todo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lifeassistant.todo.model.entity.TodoDO;

import java.util.List;

@Mapper
public interface TodoMapper extends BaseMapper<TodoDO> {

    @Select("SELECT * FROM todo WHERE (user_id = #{userId} OR assigned_to = #{userId}) " +
            "AND is_completed = 0 ORDER BY due_date ASC")
    List<TodoDO> selectIncompleteByDueDate(@Param("userId") String userId);
}
