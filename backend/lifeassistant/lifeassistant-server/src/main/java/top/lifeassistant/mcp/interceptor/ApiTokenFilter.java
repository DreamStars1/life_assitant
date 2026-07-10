package top.lifeassistant.mcp.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.lifeassistant.mcp.auth.ApiTokenAuthHelper;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.io.IOException;

/**
 * 前置过滤器：检查请求是否带 API Token（Authorization: Bearer la_xxx）。
 * 有则通过 ApiTokenAuthHelper 验证并建立 Sa-Token 会话，后续拦截器放行。
 * 无则放行（走常规登录态认证）。
 */
@Slf4j
@Component
@Order(-100)
@RequiredArgsConstructor
public class ApiTokenFilter extends OncePerRequestFilter {

    private static final String API_TOKEN_PREFIX = "Bearer la_";

    private final ApiTokenAuthHelper apiTokenAuthHelper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(API_TOKEN_PREFIX)) {
            String token = authHeader.substring(7); // "Bearer ".length()
            try {
                UserDO user = apiTokenAuthHelper.authenticate(token);
                // 用 Sa-Token 建立会话，后续 Controller 的 @CurrentUser / SaInterceptor 能正常工作
                StpUtil.login(user.getId());
                log.debug("API Token auth success: userId={}", user.getId());
            } catch (Exception e) {
                log.warn("API Token auth failed: {}", e.getMessage());
                response.setStatus(401);
                response.getWriter().write("{\"error\":\"Unauthorized: " + e.getMessage() + "\"}");
                response.setContentType("application/json");
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 如果是 API Token 建立的会话，清理掉避免影响线程复用
            if (StpUtil.isLogin() && authHeader != null && authHeader.startsWith(API_TOKEN_PREFIX)) {
                StpUtil.logout();
            }
        }
    }
}
