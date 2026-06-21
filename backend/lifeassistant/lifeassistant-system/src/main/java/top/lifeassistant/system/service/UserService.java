package top.lifeassistant.system.service;

import top.lifeassistant.system.model.entity.user.UserDO;

public interface UserService {
    UserDO getByEmail(String email);
    UserDO getById(String id);
    UserDO create(UserDO user);
    UserDO update(UserDO user);
    void delete(String id);
    boolean existsByEmail(String email);
    void unbindPartner(String userId);
}
