package top.lifeassistant.sharedmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.base.component.OwnerValidator;
import top.lifeassistant.common.base.model.query.PageResult;
import top.lifeassistant.sharedmedia.mapper.SharedMediaMapper;
import top.lifeassistant.sharedmedia.model.entity.SharedMediaDO;
import top.lifeassistant.sharedmedia.model.query.SharedMediaPageQuery;
import top.lifeassistant.sharedmedia.model.req.SharedMediaCreateReq;
import top.lifeassistant.sharedmedia.model.req.SharedMediaUpdateReq;
import top.lifeassistant.sharedmedia.model.resp.SharedMediaResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SharedMediaService {

    private final SharedMediaMapper mapper;
    private final OwnerValidator ownerValidator;

    private void requirePartner(UserDO user) {
        if (user.getPartnerId() == null) {
            throw new BadRequestException("请先绑定伴侣");
        }
    }

    public SharedMediaResp create(UserDO user, SharedMediaCreateReq req, String coverPath) {
        requirePartner(user);
        SharedMediaDO media = new SharedMediaDO();
        media.setCreatedBy(user.getId());
        media.setTitle(req.getTitle());
        media.setMediaType(req.getMediaType());
        media.setCoverPath(coverPath);
        media.setDescription(req.getDescription());
        media.setIsFinished(false);
        mapper.insert(media);
        return SharedMediaResp.from(media);
    }

    public PageResult<SharedMediaResp> list(UserDO user, SharedMediaPageQuery query) {
        requirePartner(user);
        Page<SharedMediaDO> page = query.toPage();
        LambdaQueryWrapper<SharedMediaDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SharedMediaDO::getCreatedBy, List.of(user.getId(), user.getPartnerId()));

        if (query.getMediaType() != null && !query.getMediaType().isEmpty()) {
            wrapper.eq(SharedMediaDO::getMediaType, query.getMediaType());
        }

        if ("finished".equals(query.getStatus())) {
            wrapper.eq(SharedMediaDO::getIsFinished, true);
        } else if ("unfinished".equals(query.getStatus())) {
            wrapper.eq(SharedMediaDO::getIsFinished, false);
        }

        wrapper.orderByDesc(SharedMediaDO::getUpdateTime);

        Page<SharedMediaDO> result = mapper.selectPage(page, wrapper);
        Page<SharedMediaResp> respPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        respPage.setRecords(result.getRecords().stream().map(SharedMediaResp::from).toList());
        return PageResult.of(respPage);
    }

    public SharedMediaResp getById(UserDO user, String id) {
        requirePartner(user);
        SharedMediaDO media = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "媒体不存在",
            m -> user.getId().equals(m.getCreatedBy()) || user.getPartnerId().equals(m.getCreatedBy())
        );
        return SharedMediaResp.from(media);
    }

    public SharedMediaResp update(UserDO user, String id, SharedMediaUpdateReq req, String coverPath) {
        requirePartner(user);
        SharedMediaDO media = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());

        if (req.getTitle() != null) media.setTitle(req.getTitle());
        if (req.getMediaType() != null) media.setMediaType(req.getMediaType());
        if (req.getDescription() != null) media.setDescription(req.getDescription());
        if (coverPath != null) media.setCoverPath(coverPath);

        mapper.updateById(media);
        return SharedMediaResp.from(mapper.selectById(id));
    }

    public void delete(UserDO user, String id) {
        requirePartner(user);
        SharedMediaDO media = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        mapper.deleteById(id);
    }

    public void deleteByCreatedBy(String userId1, String userId2) {
        mapper.delete(new LambdaQueryWrapper<SharedMediaDO>()
            .in(SharedMediaDO::getCreatedBy, List.of(userId1, userId2)));
    }
}
