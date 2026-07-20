package top.lifeassistant.sharedmedia.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.lifeassistant.mediacategory.mapper.MediaCategoryMapper;
import top.lifeassistant.mediacategory.model.entity.MediaCategoryDO;
import top.lifeassistant.sharedmedia.model.entity.SharedMediaDO;
import top.lifeassistant.sharedmedia.model.resp.SharedMediaResp;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MediaTypeLabels {

    private static final Map<String, String> BUILTIN = Map.of(
        "movie", "电影",
        "book", "书籍",
        "tv", "漫剧",
        "uncategorized", "未分类"
    );

    private final MediaCategoryMapper categoryMapper;

    public String resolve(String mediaType) {
        if (mediaType == null || mediaType.isBlank()) {
            return "未分类";
        }
        String builtin = BUILTIN.get(mediaType);
        if (builtin != null) {
            return builtin;
        }
        MediaCategoryDO category = categoryMapper.selectById(mediaType);
        return category != null ? category.getName() : "未分类";
    }

    public Map<String, String> batchResolve(Collection<String> mediaTypes) {
        Map<String, String> result = new HashMap<>();
        Set<String> customIds = new HashSet<>();
        for (String mediaType : mediaTypes) {
            if (mediaType == null || mediaType.isBlank()) {
                result.put(mediaType, "未分类");
                continue;
            }
            String builtin = BUILTIN.get(mediaType);
            if (builtin != null) {
                result.put(mediaType, builtin);
            } else {
                customIds.add(mediaType);
            }
        }
        if (!customIds.isEmpty()) {
            Map<String, String> names = categoryMapper.selectList(
                    new LambdaQueryWrapper<MediaCategoryDO>().in(MediaCategoryDO::getId, customIds))
                .stream()
                .collect(java.util.stream.Collectors.toMap(MediaCategoryDO::getId, MediaCategoryDO::getName));
            for (String id : customIds) {
                result.put(id, names.getOrDefault(id, "未分类"));
            }
        }
        return result;
    }

    public SharedMediaResp enrich(SharedMediaDO media) {
        SharedMediaResp resp = SharedMediaResp.from(media);
        resp.setMediaTypeLabel(resolve(media.getMediaType()));
        return resp;
    }

    public SharedMediaResp enrich(SharedMediaDO media, Map<String, String> labelMap) {
        SharedMediaResp resp = SharedMediaResp.from(media);
        resp.setMediaTypeLabel(labelMap.getOrDefault(media.getMediaType(), resolve(media.getMediaType())));
        return resp;
    }
}
