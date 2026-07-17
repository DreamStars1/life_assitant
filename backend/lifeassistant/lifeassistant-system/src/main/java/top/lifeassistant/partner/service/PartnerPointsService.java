package top.lifeassistant.partner.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.lifeassistant.partner.mapper.PartnerPointsMapper;
import top.lifeassistant.partner.model.entity.PartnerPointsDO;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.UserService;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerPointsService {
    private final PartnerPointsMapper mapper;
    private final UserService userService;

    private String getUserId() {
        return StpUtil.getLoginIdAsString();
    }

    private String getPartnerId(String userId) {
        UserDO user = userService.getById(userId);
        if (user == null || user.getPartnerId() == null)
            throw new RuntimeException("请先绑定伴侣");
        return user.getPartnerId();
    }

    public Integer getBalance(String userId) {
        String partnerId = getPartnerId(userId);
        Integer sum = mapper.sumPoints(userId, partnerId);
        return sum != null ? sum : 0;
    }

    public Page<PartnerPointsDO> getHistory(String userId, int page, int size) {
        String partnerId = getPartnerId(userId);
        LambdaQueryWrapper<PartnerPointsDO> wrapper = new LambdaQueryWrapper<PartnerPointsDO>()
            .in(PartnerPointsDO::getCreatedBy, userId, partnerId)
            .orderByDesc(PartnerPointsDO::getCreatedAt);
        return mapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public void addPoints(String userId, int pointsChange, String reason) {
        PartnerPointsDO record = new PartnerPointsDO();
        record.setId(UUID.randomUUID().toString());
        record.setCreatedBy(userId);
        record.setPointsChange(pointsChange);
        record.setReason(reason);
        record.setCreatedAt(LocalDateTime.now());
        mapper.insert(record);
    }

    public void deleteByUsers(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<PartnerPointsDO>()
            .in(PartnerPointsDO::getCreatedBy, userId1, userId2));
    }
}
