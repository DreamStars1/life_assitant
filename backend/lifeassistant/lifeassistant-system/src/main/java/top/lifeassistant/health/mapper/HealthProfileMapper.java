package top.lifeassistant.health.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.lifeassistant.health.model.entity.HealthProfileDO;

@Mapper
public interface HealthProfileMapper extends BaseMapper<HealthProfileDO> {}
