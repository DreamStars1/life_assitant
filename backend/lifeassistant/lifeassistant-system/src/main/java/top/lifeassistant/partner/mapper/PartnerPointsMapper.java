package top.lifeassistant.partner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.lifeassistant.partner.model.entity.PartnerPointsDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PartnerPointsMapper extends BaseMapper<PartnerPointsDO> {
    @Select("SELECT COALESCE(SUM(points_change), 0) FROM partner_points WHERE created_by IN (#{userId}, #{partnerId})")
    Integer sumPoints(@Param("userId") String userId, @Param("partnerId") String partnerId);
}
