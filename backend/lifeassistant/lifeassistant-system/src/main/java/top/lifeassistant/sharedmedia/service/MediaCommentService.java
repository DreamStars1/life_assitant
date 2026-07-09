package top.lifeassistant.sharedmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.lifeassistant.sharedmedia.mapper.MediaCommentMapper;
import top.lifeassistant.sharedmedia.model.entity.MediaCommentDO;
import top.lifeassistant.sharedmedia.model.req.MediaCommentCreateReq;
import top.lifeassistant.sharedmedia.model.resp.MediaCommentResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaCommentService {

    private final MediaCommentMapper mapper;
    private final SharedMediaService sharedMediaService;

    public MediaCommentResp create(UserDO user, String mediaId, MediaCommentCreateReq req) {
        sharedMediaService.getById(user, mediaId);

        MediaCommentDO comment = new MediaCommentDO();
        comment.setId(UUID.randomUUID().toString());
        comment.setMediaId(mediaId);
        comment.setUserId(user.getId());
        comment.setContent(req.getContent());
        mapper.insert(comment);
        return MediaCommentResp.from(comment);
    }

    public List<MediaCommentResp> list(UserDO user, String mediaId) {
        sharedMediaService.getById(user, mediaId);

        List<MediaCommentDO> list = mapper.selectByMediaId(mediaId);
        return list.stream().map(MediaCommentResp::from).toList();
    }

    public void deleteByMediaId(String mediaId) {
        mapper.delete(new LambdaQueryWrapper<MediaCommentDO>().eq(MediaCommentDO::getMediaId, mediaId));
    }
}
