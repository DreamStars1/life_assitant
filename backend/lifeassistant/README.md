# Life Assistant 后端服务

基于 ContiNew Starter 构建的智能生活助手后端服务。

## 技术栈

- **Java 17** + **Spring Boot 3.3**
- **Maven** 多模块项目
- **MySQL 8.4** + **Redis 7**
- **Sa-Token** 认证鉴权
- **MyBatis Plus** ORM
- **Liquibase** 数据库版本管理

## 快速开始

```bash
# 1. 复制环境变量模板
cp .env.example backend/.env
# 编辑 backend/.env 填入真实数据库密码等配置

# 2. 启动依赖服务（MySQL + Redis）
docker compose up -d db redis

# 3. 构建并启动后端
cd backend/lifeassistant
mvn package -DskipTests
cd lifeassistant-server/target/app
java -jar bin/lifeassistant.jar
```

## 项目结构

```
backend/
├── lifeassistant/
│   ├── lifeassistant-common/   # 公共模块（工具类、全局异常、注解等）
│   ├── lifeassistant-system/   # 系统管理模块（业务 API、Service、Entity）
│   ├── lifeassistant-server/   # 打包部署模块（启动类、配置文件）
│   └── pom.xml                 # 父 POM
├── nginx/                      # Nginx 反向代理配置
├── .env.example                # 环境变量模板
└── Dockerfile                  # 后端容器镜像
```
