# ===== 构建阶段 =====
FROM maven:3.9-eclipse-temurin-17-alpine AS build

WORKDIR /build

# 先复制全量 pom.xml，利用 Docker 层缓存（依赖不常变）
COPY lifeassistant/pom.xml ./lifeassistant/
COPY lifeassistant/lifeassistant-common/pom.xml ./lifeassistant/lifeassistant-common/
COPY lifeassistant/lifeassistant-system/pom.xml ./lifeassistant/lifeassistant-system/
COPY lifeassistant/lifeassistant-server/pom.xml ./lifeassistant/lifeassistant-server/

# 下载依赖
RUN cd lifeassistant && mvn dependency:go-offline -B -q

# 复制源码
COPY lifeassistant/ ./lifeassistant/

# fat-jar 打包（跳过测试以加速）
RUN cd lifeassistant && mvn package -P fat-jar -DskipTests -B -q && \
    find lifeassistant-server/target -name 'lifeassistant.jar' -exec cp {} /build/app.jar \;

# ===== 运行阶段 =====
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -s /bin/sh -D appuser

WORKDIR /app
COPY --from=build /build/app.jar ./app.jar

RUN mkdir -p /app/logs && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8000

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=60s \
    CMD wget -qO- http://localhost:8000/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
