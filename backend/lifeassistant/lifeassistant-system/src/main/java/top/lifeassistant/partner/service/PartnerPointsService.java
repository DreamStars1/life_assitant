package top.lifeassistant.partner.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.partner.mapper.PartnerPointsMapper;
import top.lifeassistant.partner.model.entity.PartnerInfoDO;
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
    private final PartnerInfoService partnerInfoService;

    private String getUserId() {
        return StpUtil.getLoginIdAsString();
    }

    private String getPartnerId(String userId) {
        UserDO user = userService.getById(userId);
        if (user == null || user.getPartnerId() == null)
            throw new BadRequestException("请先绑定伴侣");
        return user.getPartnerId();
    }

    public Integer getBalance(String userId) {
        String partnerId = getPartnerId(userId);
        PartnerInfoDO info = partnerInfoService.requireByUser(userId, partnerId);
        return info.getPointsBalance() != null ? info.getPointsBalance() : 0;
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
        String partnerId = getPartnerId(userId);
        PartnerPointsDO record = new PartnerPointsDO();
        record.setId(UUID.randomUUID().toString());
        record.setCreatedBy(userId);
        record.setPointsChange(pointsChange);
        record.setReason(reason);
        record.setCreatedAt(LocalDateTime.now());
        mapper.insert(record);
        partnerInfoService.addPointsBalance(userId, partnerId, pointsChange);
    }

    public void deleteByUsers(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<PartnerPointsDO>()
            .in(PartnerPointsDO::getCreatedBy, userId1, userId2));
    }
}
