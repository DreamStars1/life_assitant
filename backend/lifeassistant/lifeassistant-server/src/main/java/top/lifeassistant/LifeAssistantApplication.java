package top.lifeassistant;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.URLUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import top.continew.starter.core.ContiNewStarterVersion;
import top.continew.starter.core.autoconfigure.application.ApplicationProperties;

@Slf4j
@SpringBootApplication
public class LifeAssistantApplication implements ApplicationRunner {

    private final ApplicationProperties applicationProperties;
    private final ServerProperties serverProperties;

    public LifeAssistantApplication(ApplicationProperties ap, ServerProperties sp) {
        this.applicationProperties = ap;
        this.serverProperties = sp;
    }

    public static void main(String[] args) {
        System.setProperty("aws.java.v1.disableDeprecationAnnouncement", "true");
        SpringApplication.run(LifeAssistantApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) {
        String host = NetUtil.getLocalhost().getHostAddress();
        Integer port = serverProperties.getPort();
        String ctx = serverProperties.getServlet().getContextPath();
        String url = URLUtil.normalize("%s:%s%s".formatted(host, port, ctx));
        log.info("""
                ----------------------------------------------------------
                \tLife Assistant {} started successfully!
                \tSpring Boot: {} | ContiNew Starter: {}
                \tLocal:   http://localhost:{}{}
                \tNetwork: http://{}{}
                ----------------------------------------------------------""",
            applicationProperties.getVersion(), SpringBootVersion.getVersion(),
            ContiNewStarterVersion.getVersion(), port, ctx, url, ctx);
    }
}
