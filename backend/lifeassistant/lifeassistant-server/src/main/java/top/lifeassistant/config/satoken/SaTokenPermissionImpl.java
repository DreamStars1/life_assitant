package top.lifeassistant.config.satoken;

import cn.dev33.satoken.stp.StpInterface;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 权限认证实现 — 简化版
 */
public class SaTokenPermissionImpl implements StpInterface {

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // pony: single role model — superuser is the only "role"
        // The role check happens via @RequireSuperuser annotation and manual is_superuser checks
        return Collections.emptyList();
    }
}
