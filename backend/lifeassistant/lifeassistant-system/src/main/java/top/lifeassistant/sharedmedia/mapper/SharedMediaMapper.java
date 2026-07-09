package top.lifeassistant.sharedmedia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lifeassistant.sharedmedia.model.entity.SharedMediaDO;

import java.util.List;

@Mapper
public interface SharedMediaMapper extends BaseMapper<SharedMediaDO> {

    @Select("SELECT * FROM shared_media WHERE created_by IN (#{userId}, #{partnerId}) ORDER BY update_time DESC")
    List<SharedMediaDO> listByPartners(@Param("userId") String userId, @Param("partnerId") String partnerId);
}
