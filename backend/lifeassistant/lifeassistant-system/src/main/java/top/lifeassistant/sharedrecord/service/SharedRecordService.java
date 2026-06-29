package top.lifeassistant.sharedrecord.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.base.component.OwnerValidator;
import top.lifeassistant.common.base.model.query.PageResult;
import top.lifeassistant.sharedrecord.mapper.SharedRecordMapper;
import top.lifeassistant.sharedrecord.model.entity.SharedRecordDO;
import top.lifeassistant.sharedrecord.model.query.SharedRecordPageQuery;
import top.lifeassistant.sharedrecord.model.req.SharedRecordCreateReq;
import top.lifeassistant.sharedrecord.model.req.SharedRecordUpdateReq;
import top.lifeassistant.sharedrecord.model.resp.SharedRecordResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 共享记录业务
 *
 * @author DreamStars1
 * @since 2026/6/23
 */
@Service
@RequiredArgsConstructor
public class SharedRecordService {

    private final SharedRecordMapper mapper;
    private final OwnerValidator ownerValidator;

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

    public PageResult<SharedRecordResp> list(UserDO user, SharedRecordPageQuery query) {
        requirePartner(user);
        Page<SharedRecordDO> page = query.toPage();
        LambdaQueryWrapper<SharedRecordDO> qw = new LambdaQueryWrapper<>();
        qw.in(SharedRecordDO::getCreatedBy, List.of(user.getId(), user.getPartnerId()));
        if (query.getKeyword() != null) {
            String kw = query.getKeyword();
            qw.and(w -> w.like(SharedRecordDO::getTitle, kw)
                          .or().like(SharedRecordDO::getContent, kw));
        }
        if (query.getStart() != null) qw.ge(SharedRecordDO::getOccurredAt, query.getStart());
        if (query.getEnd() != null) qw.le(SharedRecordDO::getOccurredAt, query.getEnd());
        qw.orderByDesc(SharedRecordDO::getOccurredAt);
        Page<SharedRecordDO> result = mapper.selectPage(page, qw);
        Page<SharedRecordResp> respPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        respPage.setRecords(result.getRecords().stream().map(SharedRecordResp::from).toList());
        return PageResult.of(respPage);
    }

    public SharedRecordResp getById(UserDO user, String id) {
        requirePartner(user);
        SharedRecordDO record = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "记录不存在",
            r -> user.getId().equals(r.getCreatedBy()) || user.getPartnerId().equals(r.getCreatedBy()));
        return SharedRecordResp.from(record);
    }

    public SharedRecordResp update(UserDO user, String id, SharedRecordUpdateReq req) {
        requirePartner(user);
        SharedRecordDO record = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        if (req.getTitle() != null) record.setTitle(req.getTitle());
        if (req.getContent() != null) record.setContent(req.getContent());
        if (req.getOccurredAt() != null) record.setOccurredAt(req.getOccurredAt());
        record.setUpdateTime(LocalDateTime.now());
        mapper.updateById(record);
        return SharedRecordResp.from(record);
    }

    public void delete(UserDO user, String id) {
        requirePartner(user);
        ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        mapper.deleteById(id);
    }

    public void deleteByCreatedBy(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<SharedRecordDO>()
            .in(SharedRecordDO::getCreatedBy, List.of(userId1, userId2)));
    }
}
