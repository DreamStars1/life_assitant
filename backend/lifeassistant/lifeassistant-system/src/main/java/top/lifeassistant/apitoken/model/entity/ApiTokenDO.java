package top.lifeassistant.apitoken.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.lifeassistant.common.base.model.entity.BaseDO;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_token")
public class ApiTokenDO extends BaseDO {

    private String userId;
    private String name;
    private String tokenHash;
    private String tokenPrefix;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
}
