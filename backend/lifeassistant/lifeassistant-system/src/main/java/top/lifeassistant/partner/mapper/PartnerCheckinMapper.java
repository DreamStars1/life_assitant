package top.lifeassistant.partner.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.lifeassistant.partner.model.entity.PartnerCheckinDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface PartnerCheckinMapper extends BaseMapper<PartnerCheckinDO> {
    @Select("SELECT * FROM partner_checkin WHERE user_id IN (#{userId}, #{partnerId}) AND checkin_date = #{date}")
    List<PartnerCheckinDO> findToday(@Param("userId") String userId, @Param("partnerId") String partnerId, @Param("date") LocalDate date);

    @Select("SELECT * FROM partner_checkin WHERE user_id IN (#{userId}, #{partnerId}) AND checkin_date >= #{startDate} ORDER BY checkin_date, checkin_time")
    List<PartnerCheckinDO> findWeekly(@Param("userId") String userId, @Param("partnerId") String partnerId, @Param("startDate") LocalDate startDate);
}
