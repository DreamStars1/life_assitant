package top.lifeassistant.mcp.auth;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.apitoken.mapper.ApiTokenMapper;
import top.lifeassistant.apitoken.model.entity.ApiTokenDO;
import top.lifeassistant.system.model.entity.user.UserDO;
import top.lifeassistant.system.service.UserService;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ApiTokenAuthHelper {

    private final ApiTokenMapper apiTokenMapper;
    private final UserService userService;

    /**
     * 通过 API Token 原文查找并校验，返回对应的用户
     */
    public UserDO authenticate(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException("缺少 API Token");
        }
        String hash = DigestUtil.sha256Hex(token);
        ApiTokenDO apiToken = apiTokenMapper.selectOne(
            new LambdaQueryWrapper<ApiTokenDO>()
                .eq(ApiTokenDO::getTokenHash, hash)
                .eq(ApiTokenDO::getIsActive, true));
        if (apiToken == null) {
            throw new BusinessException("API Token 无效");
        }
        if (apiToken.getExpiresAt() != null && apiToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("API Token 已过期");
        }
        apiToken.setLastUsedAt(LocalDateTime.now());
        apiTokenMapper.updateById(apiToken);

        return userService.getById(apiToken.getUserId());
    }
}
