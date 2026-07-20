package top.lifeassistant.mediacategory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.lifeassistant.mediacategory.model.entity.MediaCategoryDO;

@Mapper
public interface MediaCategoryMapper extends BaseMapper<MediaCategoryDO> {
}
