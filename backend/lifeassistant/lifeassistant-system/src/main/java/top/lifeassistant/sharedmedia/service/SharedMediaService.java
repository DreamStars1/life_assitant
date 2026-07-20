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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedMediaService {

    private final SharedMediaMapper mapper;
    private final OwnerValidator ownerValidator;
    private final MediaTypeLabels mediaTypeLabels;

    private void requirePartner(UserDO user) {
        if (user.getPartnerId() == null) {
            throw new BadRequestException("请先绑定伴侣");
        }
    }

    private LocalDate parseLastWatchedAt(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("上次一起看日期格式无效，请使用 yyyy-MM-dd");
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
        if (req.getLastWatchedAt() == null || req.getLastWatchedAt().isBlank()) {
            media.setLastWatchedAt(LocalDate.now());
        } else {
            media.setLastWatchedAt(parseLastWatchedAt(req.getLastWatchedAt()));
        }
        media.setIsPrivate(req.getIsPrivate() != null ? req.getIsPrivate() : false);
        mapper.insert(media);
        return mediaTypeLabels.enrich(media);
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

        wrapper.last("ORDER BY COALESCE(last_watched_at, DATE(update_time)) DESC, update_time DESC");

        Page<SharedMediaDO> result = mapper.selectPage(page, wrapper);
        Map<String, String> labelMap = mediaTypeLabels.batchResolve(
            result.getRecords().stream().map(SharedMediaDO::getMediaType).collect(Collectors.toSet()));
        Page<SharedMediaResp> respPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        respPage.setRecords(result.getRecords().stream()
            .map(media -> mediaTypeLabels.enrich(media, labelMap))
            .toList());
        return PageResult.of(respPage);
    }

    public SharedMediaResp getById(UserDO user, String id) {
        requirePartner(user);
        SharedMediaDO media = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "媒体不存在",
            m -> user.getId().equals(m.getCreatedBy()) || user.getPartnerId().equals(m.getCreatedBy())
        );
        return mediaTypeLabels.enrich(media);
    }

    public SharedMediaResp update(UserDO user, String id, SharedMediaUpdateReq req, String coverPath, Boolean isFinished) {
        requirePartner(user);
        SharedMediaDO media = ownerValidator.findAndCheck(
            () -> mapper.selectById(id), "媒体不存在",
            m -> user.getId().equals(m.getCreatedBy()) || user.getPartnerId().equals(m.getCreatedBy())
        );

        if (req.getTitle() != null) media.setTitle(req.getTitle());
        if (req.getMediaType() != null) media.setMediaType(req.getMediaType());
        if (req.getDescription() != null) media.setDescription(req.getDescription());
        if (coverPath != null) media.setCoverPath(coverPath);
        if (isFinished != null) {
            media.setIsFinished(isFinished);
            media.setFinishedAt(isFinished ? LocalDateTime.now() : null);
        }
        if (req.getIsPrivate() != null) {
            media.setIsPrivate(req.getIsPrivate());
        }

        String lw = req.getLastWatchedAt();
        if (lw != null) {
            if (lw.isBlank()) {
                mapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SharedMediaDO>()
                    .eq(SharedMediaDO::getId, id)
                    .set(SharedMediaDO::getLastWatchedAt, null));
                media.setLastWatchedAt(null);
            } else {
                media.setLastWatchedAt(parseLastWatchedAt(lw));
            }
        }

        mapper.updateById(media);
        return mediaTypeLabels.enrich(mapper.selectById(id));
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

    /** 共同进度更新时刷新「上次一起看」为当天 */
    public void markLastWatchedToday(String mediaId) {
        SharedMediaDO media = new SharedMediaDO();
        media.setId(mediaId);
        media.setLastWatchedAt(LocalDate.now());
        mapper.updateById(media);
    }

    /** 评论、进度等互动时刷新列表排序用的更新时间 */
    public void touchActivity(String mediaId) {
        SharedMediaDO media = new SharedMediaDO();
        media.setId(mediaId);
        mapper.updateById(media);
    }
}
