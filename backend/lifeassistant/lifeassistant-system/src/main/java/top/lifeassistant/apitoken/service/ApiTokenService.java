package top.lifeassistant.apitoken.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.apitoken.mapper.ApiTokenMapper;
import top.lifeassistant.apitoken.model.entity.ApiTokenDO;
import top.lifeassistant.apitoken.model.req.ApiTokenCreateReq;
import top.lifeassistant.apitoken.model.resp.ApiTokenResp;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApiTokenService {

    private final ApiTokenMapper mapper;

    private static final String TOKEN_PREFIX = "la_";

    public ApiTokenResp create(UserDO user, ApiTokenCreateReq req) {
        String rawToken = TOKEN_PREFIX + UUID.randomUUID().toString().replace("-", "");
        String hash = DigestUtil.sha256Hex(rawToken);
        String prefix = rawToken.substring(0, 12) + "...";

        ApiTokenDO token = new ApiTokenDO();
        token.setUserId(user.getId());
        token.setName(req.getName());
        token.setTokenHash(hash);
        token.setTokenPrefix(prefix);
        token.setExpiresAt(req.getExpiresAt());
        token.setIsActive(true);
        // ponytail: createdAt / updateTime / createBy / updateBy 由 MetaObjectHandler 自动填充
        mapper.insert(token);

        return ApiTokenResp.withFullToken(token, rawToken);
    }

    public List<ApiTokenResp> list(UserDO user) {
        return mapper.selectList(
            new LambdaQueryWrapper<ApiTokenDO>()
                .eq(ApiTokenDO::getUserId, user.getId())
                .orderByDesc(ApiTokenDO::getCreatedAt))
            .stream()
            .map(ApiTokenResp::from)
            .toList();
    }

    public void delete(UserDO user, String id) {
        ApiTokenDO token = mapper.selectById(id);
        if (token == null || !token.getUserId().equals(user.getId())) {
            throw new BusinessException("令牌不存在");
        }
        token.setIsActive(false);
        token.setUpdateTime(LocalDateTime.now());
        mapper.updateById(token);
    }
}
