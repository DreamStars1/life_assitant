package top.lifeassistant.apitoken.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.lifeassistant.apitoken.model.entity.ApiTokenDO;

@Mapper
public interface ApiTokenMapper extends BaseMapper<ApiTokenDO> {
}
