package top.lifeassistant.sharedmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.sharedmedia.mapper.MediaProgressMapper;
import top.lifeassistant.sharedmedia.model.entity.MediaProgressDO;
import top.lifeassistant.sharedmedia.model.req.MediaProgressUpdateReq;
import top.lifeassistant.sharedmedia.model.resp.MediaProgressResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaProgressService {

    private final MediaProgressMapper mapper;
    private final SharedMediaService sharedMediaService;

    public List<MediaProgressResp> list(UserDO user, String mediaId) {
        sharedMediaService.getById(user, mediaId);
        List<MediaProgressDO> list = mapper.selectByMediaId(mediaId);
        return list.stream().map(MediaProgressResp::from).toList();
    }

    public MediaProgressResp update(UserDO user, String mediaId, MediaProgressUpdateReq req) {
        sharedMediaService.getById(user, mediaId);

        if ("shared".equals(req.getScope())) {
            upsertProgress(mediaId, null, req.getProgressText());
            upsertProgress(mediaId, user.getId(), req.getProgressText());
            if (user.getPartnerId() != null) {
                upsertProgress(mediaId, user.getPartnerId(), req.getProgressText());
            }
        } else if ("personal".equals(req.getScope())) {
            upsertProgress(mediaId, user.getId(), req.getProgressText());
        } else {
            throw new BadRequestException("scope 必须是 shared 或 personal");
        }

        sharedMediaService.touchActivity(mediaId);

        if ("shared".equals(req.getScope())) {
            return MediaProgressResp.from(mapper.selectSharedByMediaId(mediaId));
        } else {
            return MediaProgressResp.from(mapper.selectByMediaIdAndUser(mediaId, user.getId()));
        }
    }

    private void upsertProgress(String mediaId, String userId, String progressText) {
        MediaProgressDO existing;
        if (userId == null) {
            existing = mapper.selectSharedByMediaId(mediaId);
        } else {
            existing = mapper.selectByMediaIdAndUser(mediaId, userId);
        }
        if (existing != null) {
            existing.setProgressText(progressText);
            mapper.updateById(existing);
        } else {
            MediaProgressDO p = new MediaProgressDO();
            p.setId(UUID.randomUUID().toString());
            p.setMediaId(mediaId);
            p.setUserId(userId);
            p.setProgressText(progressText);
            mapper.insert(p);
        }
    }

    public void deleteByMediaId(String mediaId) {
        mapper.delete(new LambdaQueryWrapper<MediaProgressDO>().eq(MediaProgressDO::getMediaId, mediaId));
    }
}
