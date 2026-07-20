package top.lifeassistant.mediacategory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.exception.BadRequestException;
import top.lifeassistant.common.base.component.OwnerValidator;
import top.lifeassistant.mediacategory.mapper.MediaCategoryMapper;
import top.lifeassistant.mediacategory.model.entity.MediaCategoryDO;
import top.lifeassistant.mediacategory.model.resp.MediaCategoryResp;
import top.lifeassistant.sharedmedia.mapper.SharedMediaMapper;
import top.lifeassistant.sharedmedia.model.entity.SharedMediaDO;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MediaCategoryService {

    private final MediaCategoryMapper mapper;
    private final SharedMediaMapper sharedMediaMapper;
    private final OwnerValidator ownerValidator;

    public List<MediaCategoryResp> list(UserDO user) {
        return mapper.selectList(new LambdaQueryWrapper<MediaCategoryDO>()
                .eq(MediaCategoryDO::getUserId, user.getId())
                .orderByAsc(MediaCategoryDO::getCreatedAt))
            .stream()
            .map(MediaCategoryResp::from)
            .toList();
    }

    public MediaCategoryResp create(UserDO user, String name) {
        String trimmed = validateName(name);
        if (existsByName(user.getId(), trimmed, null)) {
            throw new BadRequestException("分类名已存在");
        }
        MediaCategoryDO category = new MediaCategoryDO();
        category.setId(UUID.randomUUID().toString());
        category.setUserId(user.getId());
        category.setName(trimmed);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        mapper.insert(category);
        return MediaCategoryResp.from(category);
    }

    public MediaCategoryResp rename(UserDO user, String id, String name) {
        String trimmed = validateName(name);
        MediaCategoryDO category = ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        if (existsByName(user.getId(), trimmed, id)) {
            throw new BadRequestException("分类名已存在");
        }
        category.setName(trimmed);
        category.setUpdateTime(LocalDateTime.now());
        mapper.updateById(category);
        return MediaCategoryResp.from(category);
    }

    @Transactional
    public void delete(UserDO user, String id) {
        ownerValidator.requireOwner(() -> mapper.selectById(id), user.getId());
        sharedMediaMapper.update(null, new LambdaUpdateWrapper<SharedMediaDO>()
            .eq(SharedMediaDO::getCreatedBy, user.getId())
            .eq(SharedMediaDO::getMediaType, id)
            .set(SharedMediaDO::getMediaType, "uncategorized"));
        mapper.deleteById(id);
    }

    public Map<String, String> nameMapByIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<MediaCategoryDO>()
                .in(MediaCategoryDO::getId, ids))
            .stream()
            .collect(Collectors.toMap(MediaCategoryDO::getId, MediaCategoryDO::getName));
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BadRequestException("分类名不能为空");
        }
        return name.trim();
    }

    private boolean existsByName(String userId, String name, String excludeId) {
        LambdaQueryWrapper<MediaCategoryDO> wrapper = new LambdaQueryWrapper<MediaCategoryDO>()
            .eq(MediaCategoryDO::getUserId, userId)
            .eq(MediaCategoryDO::getName, name);
        if (excludeId != null) {
            wrapper.ne(MediaCategoryDO::getId, excludeId);
        }
        return mapper.selectCount(wrapper) > 0;
    }
}
