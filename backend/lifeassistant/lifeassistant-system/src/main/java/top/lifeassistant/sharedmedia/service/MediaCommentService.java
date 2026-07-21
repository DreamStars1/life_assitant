package top.lifeassistant.sharedmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.lifeassistant.common.component.UploadStorage;
import top.lifeassistant.sharedmedia.mapper.MediaCommentMapper;
import top.lifeassistant.sharedmedia.model.entity.MediaCommentDO;
import top.lifeassistant.sharedmedia.model.req.MediaCommentCreateReq;
import top.lifeassistant.sharedmedia.model.resp.CommentImagesUploadResp;
import top.lifeassistant.sharedmedia.model.resp.MediaCommentResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaCommentService {

    private final MediaCommentMapper mapper;
    private final SharedMediaService sharedMediaService;
    private final UploadStorage uploadStorage;

    public CommentImagesUploadResp uploadImages(UserDO user, String mediaId, MultipartFile[] files) throws IOException {
        sharedMediaService.getById(user, mediaId);
        MediaCommentImageRules.validateUploadFiles(files);

        Path dir = uploadStorage.mediaCommentsDir();
        Files.createDirectories(dir);

        List<String> urls = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            String filename = UUID.randomUUID() + "_" + MediaCommentImageRules.safeFilename(file);
            Path filePath = dir.resolve(filename);
            file.transferTo(filePath.toFile());
            urls.add(uploadStorage.mediaCommentUrl(filename));
        }
        return CommentImagesUploadResp.builder().urls(urls).build();
    }

    public MediaCommentResp create(UserDO user, String mediaId, MediaCommentCreateReq req) {
        sharedMediaService.getById(user, mediaId);
        MediaCommentImageRules.validateCreateComment(req.getContent(), req.getImageUrls());

        MediaCommentDO comment = new MediaCommentDO();
        comment.setMediaId(mediaId);
        comment.setUserId(user.getId());
        comment.setContent(MediaCommentImageRules.resolveContentForSave(req.getContent()));
        comment.setImageUrls(MediaCommentImageRules.serializeImageUrls(req.getImageUrls()));
        mapper.insert(comment);
        sharedMediaService.touchActivity(mediaId);
        MediaCommentDO saved = mapper.selectById(comment.getId());
        return MediaCommentResp.from(saved != null ? saved : comment);
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
