package top.lifeassistant.common.base.model.entity;

/**
 * 声明实体"所有者"的接口，供 OwnerValidator 进行所有权校验。
 *
 * <p>实现类只需返回所有者的用户 ID（如 userId、createdBy 等字段的值）。</p>
 */
public interface OwnedEntity {

    /** 返回所有者的用户 ID */
    String getOwnerId();
}
