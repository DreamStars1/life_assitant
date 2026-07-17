package top.lifeassistant.partner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.partner.mapper.PartnerCheckinMapper;
import top.lifeassistant.partner.model.entity.PartnerCheckinDO;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerCheckinService {
    /** 作息日以凌晨 4 点为界：0:00–3:59 算前一天 */
    static final int DAY_BOUNDARY_HOUR = 4;

    private final PartnerCheckinMapper mapper;
    private final UserService userService;

    /**
     * 0:00–3:59 → 前一天；4:00 起 → 当天。
     */
    static LocalDate businessDate(LocalDateTime now) {
        LocalDate date = now.toLocalDate();
        return now.getHour() < DAY_BOUNDARY_HOUR ? date.minusDays(1) : date;
    }

    private String getPartnerId(String userId) {
        UserDO user = userService.getById(userId);
        if (user == null || user.getPartnerId() == null)
            throw new RuntimeException("请先绑定伴侣");
        return user.getPartnerId();
    }

    public void checkin(String userId, String checkinType) {
        if (!"wake".equals(checkinType) && !"sleep".equals(checkinType))
            throw new BadRequestException("打卡类型必须是 wake 或 sleep");

        LocalDateTime now = LocalDateTime.now();
        LocalDate day = businessDate(now);

        Long count = mapper.selectCount(new LambdaQueryWrapper<PartnerCheckinDO>()
            .eq(PartnerCheckinDO::getUserId, userId)
            .eq(PartnerCheckinDO::getCheckinDate, day)
            .eq(PartnerCheckinDO::getCheckinType, checkinType));
        if (count > 0)
            throw new BadRequestException("今天已经打过" + ("wake".equals(checkinType) ? "起床" : "睡觉") + "卡了");

        PartnerCheckinDO record = new PartnerCheckinDO();
        record.setId(UUID.randomUUID().toString());
        record.setUserId(userId);
        record.setCheckinType(checkinType);
        record.setCheckinTime(now);
        record.setCheckinDate(day);
        record.setCreatedAt(now);
        mapper.insert(record);
    }

    public List<PartnerCheckinDO> getToday(String userId) {
        String partnerId = getPartnerId(userId);
        return mapper.findToday(userId, partnerId, businessDate(LocalDateTime.now()));
    }

    public List<PartnerCheckinDO> getWeekly(String userId) {
        String partnerId = getPartnerId(userId);
        LocalDate end = businessDate(LocalDateTime.now());
        return mapper.findWeekly(userId, partnerId, end.minusDays(6));
    }

    public void deleteByUsers(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<PartnerCheckinDO>()
            .in(PartnerCheckinDO::getUserId, userId1, userId2));
    }
}
