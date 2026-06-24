package top.lifeassistant.common.base.component;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.continew.starter.core.exception.BusinessException;
import top.lifeassistant.common.base.model.entity.OwnedEntity;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * 轻量所有者校验组件。
 *
 * <p>消除"查 → 判空 → 鉴权"重复模式。支持自定义谓词校验和快捷所有者校验。</p>
 */
@Component
@RequiredArgsConstructor
public class OwnerValidator {

    /**
     * 查 → 判空 → 谓词校验，不通过统一抛 BusinessException。
     *
     * @param fetcher   实体获取函数（通常是 mapper::selectById）
     * @param errMsg    校验失败时返回的消息
     * @param predicate 校验逻辑：接收实体，返回 true=通过
     * @param <T>       实体类型（从 fetcher 返回值自动推断）
     * @return 校验通过的实体
     */
    public <T> T findAndCheck(Supplier<T> fetcher, String errMsg, Predicate<T> predicate) {
        T entity = fetcher.get();
        if (entity == null || !predicate.test(entity)) {
            throw new BusinessException(errMsg);
        }
        return entity;
    }

    /**
     * 快捷方法：查 → 判空 → 校验所有者身份。
     * 不存在或非本人，统抛 BusinessException("资源不存在")。
     */
    public <T extends OwnedEntity> T requireOwner(Supplier<T> fetcher, String ownerId) {
        return findAndCheck(fetcher, "资源不存在", e -> ownerId.equals(e.getOwnerId()));
    }
}
