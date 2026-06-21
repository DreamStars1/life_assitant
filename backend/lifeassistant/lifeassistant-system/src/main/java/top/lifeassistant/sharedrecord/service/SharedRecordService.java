package top.lifeassistant.sharedrecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.lifeassistant.sharedrecord.mapper.SharedRecordMapper;
import top.lifeassistant.sharedrecord.model.entity.SharedRecordDO;
import top.lifeassistant.sharedrecord.model.req.SharedRecordCreateReq;
import top.lifeassistant.sharedrecord.model.req.SharedRecordUpdateReq;
import top.lifeassistant.sharedrecord.model.resp.SharedRecordResp;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.core.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedRecordService {

    private final SharedRecordMapper mapper;

    private void requirePartner(UserDO user) {
        if (user.getPartnerId() == null) {
            throw new BadRequestException("请先绑定伴侣");
        }
    }

    public SharedRecordResp create(UserDO user, SharedRecordCreateReq req) {
        requirePartner(user);
        SharedRecordDO record = new SharedRecordDO();
        record.setCreatedBy(user.getId());
        record.setTitle(req.getTitle());
        record.setContent(req.getContent());
        record.setOccurredAt(req.getOccurredAt() != null ? req.getOccurredAt() : LocalDateTime.now());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdateTime(LocalDateTime.now());
        mapper.insert(record);
        return SharedRecordResp.from(record);
    }

    public List<SharedRecordResp> list(UserDO user, LocalDateTime start, LocalDateTime end) {
        requirePartner(user);
        LambdaQueryWrapper<SharedRecordDO> qw = new LambdaQueryWrapper<>();
        qw.in(SharedRecordDO::getCreatedBy, List.of(user.getId(), user.getPartnerId()));
        if (start != null) qw.ge(SharedRecordDO::getOccurredAt, start);
        if (end != null) qw.le(SharedRecordDO::getOccurredAt, end);
        qw.orderByDesc(SharedRecordDO::getOccurredAt);
        return mapper.selectList(qw).stream().map(SharedRecordResp::from).toList();
    }

    public SharedRecordResp getById(UserDO user, String id) {
        requirePartner(user);
        SharedRecordDO record = mapper.selectById(id);
        if (record == null) throw new BusinessException("Record not found");
        if (!record.getCreatedBy().equals(user.getId()) && !record.getCreatedBy().equals(user.getPartnerId())) {
            throw new BusinessException("Record not found");
        }
        return SharedRecordResp.from(record);
    }

    public SharedRecordResp update(UserDO user, String id, SharedRecordUpdateReq req) {
        requirePartner(user);
        SharedRecordDO record = mapper.selectById(id);
        if (record == null) throw new BusinessException("Record not found");
        if (!record.getCreatedBy().equals(user.getId())) {
            throw new BadRequestException("只能修改自己的记录");
        }
        if (req.getTitle() != null) record.setTitle(req.getTitle());
        if (req.getContent() != null) record.setContent(req.getContent());
        if (req.getOccurredAt() != null) record.setOccurredAt(req.getOccurredAt());
        record.setUpdateTime(LocalDateTime.now());
        mapper.updateById(record);
        return SharedRecordResp.from(record);
    }

    public void delete(UserDO user, String id) {
        requirePartner(user);
        SharedRecordDO record = mapper.selectById(id);
        if (record == null) throw new BusinessException("Record not found");
        if (!record.getCreatedBy().equals(user.getId())) {
            throw new BadRequestException("只能删除自己的记录");
        }
        mapper.deleteById(id);
    }

    public void deleteByCreatedBy(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<SharedRecordDO>()
            .in(SharedRecordDO::getCreatedBy, List.of(userId1, userId2)));
    }
}
