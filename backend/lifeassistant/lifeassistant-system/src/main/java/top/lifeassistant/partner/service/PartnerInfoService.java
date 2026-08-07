package top.lifeassistant.partner.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.partner.mapper.PartnerInfoMapper;
import top.lifeassistant.partner.model.entity.PartnerInfoDO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PartnerInfoService {

    private final PartnerInfoMapper mapper;

    public PartnerInfoDO findByUser(String userId, String partnerId) {
        String[] pair = PartnerInfoRules.orderedPair(userId, partnerId);
        return mapper.selectOne(pairWrapper(pair[0], pair[1]));
    }

    public PartnerInfoDO requireByUser(String userId, String partnerId) {
        PartnerInfoDO info = findByUser(userId, partnerId);
        if (info == null) {
            throw new BadRequestException("伴侣信息不存在");
        }
        return info;
    }

    @Transactional
    public PartnerInfoDO createForPair(String userId, String partnerId, LocalDate since) {
        PartnerInfoRules.requireNotFuture(since, LocalDate.now());
        String[] pair = PartnerInfoRules.orderedPair(userId, partnerId);
        LocalDateTime now = LocalDateTime.now();
        PartnerInfoDO row = new PartnerInfoDO();
        row.setId(UUID.randomUUID().toString());
        row.setUserAId(pair[0]);
        row.setUserBId(pair[1]);
        row.setPartnerSince(since);
        row.setPointsBalance(0);
        row.setCreatedAt(now);
        row.setUpdateTime(now);
        mapper.insert(row);
        return row;
    }

    @Transactional
    public void updatePartnerSince(String userId, String partnerId, LocalDate since) {
        PartnerInfoRules.requireNotFuture(since, LocalDate.now());
        PartnerInfoDO info = requireByUser(userId, partnerId);
        info.setPartnerSince(since);
        info.setUpdateTime(LocalDateTime.now());
        mapper.updateById(info);
    }

    @Transactional
    public void addPointsBalance(String userId, String partnerId, int delta) {
        PartnerInfoDO info = requireByUser(userId, partnerId);
        // ponytail: 无并发锁，两人 App 可接受
        info.setPointsBalance(info.getPointsBalance() + delta);
        info.setUpdateTime(LocalDateTime.now());
        mapper.updateById(info);
    }

    @Transactional
    public void deleteForPair(String userId, String partnerId) {
        String[] pair = PartnerInfoRules.orderedPair(userId, partnerId);
        mapper.delete(pairWrapper(pair[0], pair[1]));
    }

    private static LambdaQueryWrapper<PartnerInfoDO> pairWrapper(String userAId, String userBId) {
        return new LambdaQueryWrapper<PartnerInfoDO>()
            .eq(PartnerInfoDO::getUserAId, userAId)
            .eq(PartnerInfoDO::getUserBId, userBId);
    }
}
