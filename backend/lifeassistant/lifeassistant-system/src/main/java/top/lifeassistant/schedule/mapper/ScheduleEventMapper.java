package top.lifeassistant.schedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.lifeassistant.schedule.model.entity.ScheduleEventDO;

@Mapper
public interface ScheduleEventMapper extends BaseMapper<ScheduleEventDO> {}
