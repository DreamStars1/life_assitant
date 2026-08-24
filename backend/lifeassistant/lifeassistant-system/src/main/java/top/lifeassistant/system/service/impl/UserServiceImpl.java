package top.lifeassistant.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.lifeassistant.partner.mapper.PartnerMessageMapper;
import top.lifeassistant.partner.mapper.PartnerPointsMapper;
import top.lifeassistant.partner.model.entity.PartnerMessageDO;
import top.lifeassistant.partner.model.entity.PartnerPointsDO;
import top.lifeassistant.partner.service.PartnerInfoService;
import top.lifeassistant.sharedrecord.service.SharedRecordService;
import top.lifeassistant.system.mapper.user.UserMapper;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.UserService;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.core.exception.BusinessException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final SharedRecordService sharedRecordService;
    private final PartnerInfoService partnerInfoService;
    // ponytail: 直接用 Mapper，避免 UserService ↔ PartnerPointsService / PartnerMessageService 环
    private final PartnerPointsMapper partnerPointsMapper;
    private final PartnerMessageMapper partnerMessageMapper;

    @Override
    public UserDO getByEmail(String email) { return userMapper.selectByEmail(email); }

    @Override
    public UserDO getById(String id) {
        UserDO user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("User not found");
        return user;
    }

    @Override
    public UserDO create(UserDO user) { userMapper.insert(user); return user; }

    @Override
    public UserDO update(UserDO user) { user.setUpdateTime(LocalDateTime.now()); userMapper.updateById(user); return user; }

    @Override
    @Transactional
    public void delete(String id) {
        UserDO user = getById(id);
        if (user.getPartnerId() != null) {
            UserDO partner = getById(user.getPartnerId());
            partnerInfoService.deleteForPair(user.getId(), partner.getId());
            deletePartnerPoints(user.getId(), partner.getId());
            partner.setPartnerId(null);
            userMapper.updateById(partner);
        }
        userMapper.deleteById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<UserDO>().eq(UserDO::getEmail, email)) > 0;
    }

    @Override
    @Transactional
    public void unbindPartner(String userId) {
        UserDO me = getById(userId);
        if (me.getPartnerId() == null) {
            throw new BadRequestException("尚未绑定伴侣");
        }
        UserDO partner = getById(me.getPartnerId());
        partnerInfoService.deleteForPair(me.getId(), partner.getId());
        deletePartnerPoints(me.getId(), partner.getId());
        sharedRecordService.deleteByCreatedBy(me.getId(), partner.getId());
        deletePartnerMessages(me.getId(), partner.getId());
        // 双向解除
        me.setPartnerId(null);
        partner.setPartnerId(null);
        userMapper.updateById(me);
        userMapper.updateById(partner);
    }

    private void deletePartnerPoints(String userId1, String userId2) {
        partnerPointsMapper.delete(new LambdaQueryWrapper<PartnerPointsDO>()
            .in(PartnerPointsDO::getCreatedBy, userId1, userId2));
    }

    private void deletePartnerMessages(String userId1, String userId2) {
        partnerMessageMapper.delete(new LambdaQueryWrapper<PartnerMessageDO>()
            .in(PartnerMessageDO::getCreatedBy, userId1, userId2));
    }
}
