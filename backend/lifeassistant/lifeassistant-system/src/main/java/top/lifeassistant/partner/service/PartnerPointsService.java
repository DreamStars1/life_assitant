package top.lifeassistant.partner.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.partner.mapper.PartnerPointsMapper;
import top.lifeassistant.partner.model.entity.PartnerInfoDO;
import top.lifeassistant.partner.model.entity.PartnerPointsDO;
import top.lifeassistant.partner.model.resp.PartnerPointsHistoryResp;
import top.lifeassistant.partner.model.resp.PointsDateChangeResult;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    public PartnerPointsHistoryResp getHistoryResp(String userId, int page, int size) {
        String partnerId = getPartnerId(userId);
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<PartnerPointsDO> wrapper = new LambdaQueryWrapper<PartnerPointsDO>()
            .in(PartnerPointsDO::getCreatedBy, userId, partnerId)
            .orderByDesc(PartnerPointsDO::getCreatedAt);
        Page<PartnerPointsDO> pageResult = mapper.selectPage(new Page<>(page, size), wrapper);
        for (PartnerPointsDO record : pageResult.getRecords()) {
            if (record.getPendingRecordDate() != null
                && PartnerPointsRules.isPendingExpired(record.getPendingRequestedAt(), now)) {
                persistClearPending(record.getId());
                PartnerPointsRules.clearPendingFields(record);
            }
        }

        List<PartnerPointsDO> pendingForMe = mapper.selectList(new LambdaQueryWrapper<PartnerPointsDO>()
            .eq(PartnerPointsDO::getPendingRequestedBy, partnerId)
            .isNotNull(PartnerPointsDO::getPendingRecordDate));
        int pendingConfirmCount = 0;
        for (PartnerPointsDO record : pendingForMe) {
            if (PartnerPointsRules.isPendingExpired(record.getPendingRequestedAt(), now)) {
                persistClearPending(record.getId());
                continue;
            }
            pendingConfirmCount++;
        }

        PartnerPointsHistoryResp resp = new PartnerPointsHistoryResp();
        resp.setRecords(pageResult.getRecords());
        resp.setTotal(pageResult.getTotal());
        resp.setSize(pageResult.getSize());
        resp.setCurrent(pageResult.getCurrent());
        resp.setPages(pageResult.getPages());
        resp.setPendingConfirmCount(pendingConfirmCount);
        return resp;
    }

    @Transactional
    public void addPoints(String userId, int pointsChange, String reason, LocalDate recordDate) {
        String partnerId = getPartnerId(userId);
        LocalDateTime now = LocalDateTime.now();
        PartnerPointsDO record = new PartnerPointsDO();
        record.setId(UUID.randomUUID().toString());
        record.setCreatedBy(userId);
        record.setPointsChange(pointsChange);
        record.setReason(reason);
        record.setCreatedAt(PartnerPointsRules.resolveCreatedAt(recordDate, now.toLocalDate(), now));
        mapper.insert(record);
        partnerInfoService.addPointsBalance(userId, partnerId, pointsChange);
    }

    @Transactional
    public PointsDateChangeResult updateRecordDate(String userId, String recordId, LocalDate recordDate) {
        if (recordDate == null) {
            throw new BadRequestException("记录日期不能为空");
        }
        String partnerId = getPartnerId(userId);
        requirePairRecord(userId, partnerId, recordId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime resolved = PartnerPointsRules.resolveCreatedAt(recordDate, now.toLocalDate(), now);

        UserDO me = userService.getById(userId);
        if (me != null && PartnerPointsRules.isCcSkipConfirm(me.getFullName())) {
            mapper.update(null, new LambdaUpdateWrapper<PartnerPointsDO>()
                .eq(PartnerPointsDO::getId, recordId)
                .set(PartnerPointsDO::getCreatedAt, resolved)
                .set(PartnerPointsDO::getPendingRecordDate, null)
                .set(PartnerPointsDO::getPendingRequestedBy, null)
                .set(PartnerPointsDO::getPendingRequestedAt, null));
            return PointsDateChangeResult.applied();
        }

        mapper.update(null, new LambdaUpdateWrapper<PartnerPointsDO>()
            .eq(PartnerPointsDO::getId, recordId)
            .set(PartnerPointsDO::getPendingRecordDate, recordDate)
            .set(PartnerPointsDO::getPendingRequestedBy, userId)
            .set(PartnerPointsDO::getPendingRequestedAt, now));
        return PointsDateChangeResult.pending();
    }

    // noRollbackFor: requireConfirmablePending() may persist an expired-pending clear and then throw
    // BadRequestException("申请已过期") in the same call; the clear must commit, not roll back with the 400.
    @Transactional(noRollbackFor = BadRequestException.class)
    public void approveDateChange(String userId, String recordId) {
        String partnerId = getPartnerId(userId);
        PartnerPointsDO record = requirePairRecord(userId, partnerId, recordId);
        approveOneUnlocked(userId, record, LocalDateTime.now());
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public void rejectDateChange(String userId, String recordId) {
        String partnerId = getPartnerId(userId);
        PartnerPointsDO record = requirePairRecord(userId, partnerId, recordId);
        requireConfirmablePending(userId, record, LocalDateTime.now());
        persistClearPending(recordId);
    }

    @Transactional
    public int approveAllDateChanges(String userId) {
        String partnerId = getPartnerId(userId);
        LocalDateTime now = LocalDateTime.now();
        List<PartnerPointsDO> list = mapper.selectList(new LambdaQueryWrapper<PartnerPointsDO>()
            .eq(PartnerPointsDO::getPendingRequestedBy, partnerId)
            .isNotNull(PartnerPointsDO::getPendingRecordDate));
        int approvedCount = 0;
        for (PartnerPointsDO record : list) {
            if (PartnerPointsRules.isPendingExpired(record.getPendingRequestedAt(), now)) {
                persistClearPending(record.getId());
                continue;
            }
            try {
                approveOneUnlocked(userId, record, now);
                approvedCount++;
            } catch (BadRequestException ignored) {
                // e.g. pending date became a future date across day boundary: skip, keep pending, continue with rest
            }
        }
        return approvedCount;
    }

    public void deleteByUsers(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<PartnerPointsDO>()
            .in(PartnerPointsDO::getCreatedBy, userId1, userId2));
    }

    private void approveOneUnlocked(String confirmerId, PartnerPointsDO record, LocalDateTime now) {
        requireConfirmablePending(confirmerId, record, now);
        LocalDateTime createdAt = PartnerPointsRules.resolveCreatedAt(record.getPendingRecordDate(), now.toLocalDate(), now);
        mapper.update(null, new LambdaUpdateWrapper<PartnerPointsDO>()
            .eq(PartnerPointsDO::getId, record.getId())
            .set(PartnerPointsDO::getCreatedAt, createdAt)
            .set(PartnerPointsDO::getPendingRecordDate, null)
            .set(PartnerPointsDO::getPendingRequestedBy, null)
            .set(PartnerPointsDO::getPendingRequestedAt, null));
    }

    private void persistClearPending(String recordId) {
        mapper.update(null, new LambdaUpdateWrapper<PartnerPointsDO>()
            .eq(PartnerPointsDO::getId, recordId)
            .set(PartnerPointsDO::getPendingRecordDate, null)
            .set(PartnerPointsDO::getPendingRequestedBy, null)
            .set(PartnerPointsDO::getPendingRequestedAt, null));
    }

    private PartnerPointsDO requirePairRecord(String userId, String partnerId, String recordId) {
        PartnerPointsDO record = mapper.selectById(recordId);
        if (record == null) {
            throw new BadRequestException("积分记录不存在");
        }
        String by = record.getCreatedBy();
        if (!userId.equals(by) && !partnerId.equals(by)) {
            throw new BadRequestException("无权修改该积分记录");
        }
        return record;
    }

    private void requireConfirmablePending(String confirmerId, PartnerPointsDO record, LocalDateTime now) {
        if (record.getPendingRecordDate() == null || record.getPendingRequestedBy() == null) {
            throw new BadRequestException("没有待确认的日期修改");
        }
        if (PartnerPointsRules.isPendingExpired(record.getPendingRequestedAt(), now)) {
            persistClearPending(record.getId());
            throw new BadRequestException("申请已过期");
        }
        String requester = record.getPendingRequestedBy();
        if (!confirmerId.equals(getPartnerId(requester))) {
            throw new BadRequestException("无权确认该申请");
        }
    }
}
