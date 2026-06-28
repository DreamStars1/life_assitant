package top.lifeassistant.config;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器。
 * <p>
 * 填充 {@code BaseDO} 中的 createdAt / updateTime / createBy / updateBy 字段。
 * 不覆盖 ContiNew 自带的 {@code BaseCreateDO/BaseUpdateDO} 填充逻辑（Long 类型字段）。
 * </p>
 */
@Component
public class MyBatisPlusMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        try {
            String loginId = (String) StpUtil.getLoginId();
            strictInsertFill(metaObject, "createBy", String.class, loginId);
            strictInsertFill(metaObject, "updateBy", String.class, loginId);
        } catch (Exception e) {
            // ponytail: 注册等无登录上下文的场景，不填充
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        try {
            String loginId = (String) StpUtil.getLoginId();
            strictUpdateFill(metaObject, "updateBy", String.class, loginId);
        } catch (Exception e) {
            // 同 insertFill
        }
    }
}
