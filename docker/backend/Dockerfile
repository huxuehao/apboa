# ============================================================
# 阶段一：Maven 构建
# ============================================================
# 构建镜像（由 docker-compose.yml args 传入，支持私有仓库）
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
FROM ${MAVEN_IMAGE} AS build

WORKDIR /build

# 复制根 pom 及各模块 pom（保留目录结构），利用 Docker 缓存层加速
COPY pom.xml .
COPY --parents */pom.xml ./
COPY --parents */*/pom.xml ./

# 复制 Maven 私有仓库配置（在线模式为空壳，离线模式用户编辑）
COPY docker/maven/settings.xml /root/.m2/settings.xml

# 预下载依赖
RUN mvn dependency:go-offline -pl console -am -DskipTests -q

# 复制源码并构建
COPY . .
RUN mvn package -pl console -am -DskipTests -q

# ============================================================
# 阶段二：运行镜像
# ============================================================
# 运行镜像（由 docker-compose.yml args 传入，支持私有仓库）
ARG JRE_IMAGE=eclipse-temurin:21-jre
FROM ${JRE_IMAGE}

WORKDIR /app

# 复制构建产物
COPY --from=build /build/console/target/console-*.jar app.jar

# 复制 Docker 环境配置文件到 classpath
COPY docker/backend/application-docker.yml /app/config/application-docker.yml

# 创建数据目录
RUN mkdir -p /app/data /app/logs

EXPOSE 3060

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
