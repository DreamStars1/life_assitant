package top.lifeassistant.sharedrecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import top.lifeassistant.sharedrecord.model.entity.SharedRecordDO;

import java.util.List;

@Mapper
public interface SharedRecordMapper extends BaseMapper<SharedRecordDO> {

    @Select("SELECT * FROM shared_record WHERE created_by IN (#{userId}, #{partnerId}) ORDER BY occurred_at DESC")
    List<SharedRecordDO> listByPartners(@Param("userId") String userId, @Param("partnerId") String partnerId);
}
