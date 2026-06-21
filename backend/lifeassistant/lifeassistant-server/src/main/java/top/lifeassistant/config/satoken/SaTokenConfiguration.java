package top.lifeassistant.config.satoken;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.continew.starter.auth.satoken.autoconfigure.SaTokenExtensionProperties;

import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SaTokenConfiguration {

    private final SaTokenExtensionProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Bean
    public StpInterface stpInterface() {
        return new SaTokenPermissionImpl();
    }

    @Bean
    public SaInterceptor saInterceptor() {
        return new SaInterceptor(handle -> {
            String[] excludes = properties.getSecurity().getExcludes();
            if (excludes != null && excludes.length > 0) {
                HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
                String uri = request.getRequestURI();
                for (String pattern : excludes) {
                    if (pathMatcher.match(pattern, uri)) {
                        return; // 放行
                    }
                }
            }
            StpUtil.checkLogin();
        });
    }
}
