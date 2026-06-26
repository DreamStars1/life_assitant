package top.lifeassistant.mcp.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import top.lifeassistant.mcp.auth.ApiTokenAuthHelper;
import top.lifeassistant.system.model.entity.user.UserDO;

import java.io.IOException;

/**
 * 前置过滤器：检查 HTTP Header 中是否有 API Token（Authorization: Bearer la_xxx）。
 * 有则走 API Token 认证并注入用户到 request attribute；无则放行交给 Sa-Token。
 */
@Component
@RequiredArgsConstructor
public class ApiTokenFilter extends OncePerRequestFilter {

    private final ApiTokenAuthHelper apiTokenAuthHelper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (!path.startsWith("/mcp")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                UserDO user = apiTokenAuthHelper.authenticate(token);
                request.setAttribute("currentUser", user);
            } catch (Exception e) {
                response.setStatus(401);
                response.getWriter().write("{\"error\":\"Unauthorized: " + e.getMessage() + "\"}");
                response.setContentType("application/json");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
