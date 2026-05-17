ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-21
ARG JRE_IMAGE=eclipse-temurin:21-jre
# ============================================================
# 阶段一：Maven 构建
# ============================================================
# 构建镜像（由 docker-compose.yml args 传入，支持私有仓库）
FROM ${MAVEN_IMAGE} AS build

WORKDIR /build

# 复制根 pom 及各模块 pom（保留目录结构），利用 Docker 缓存层加速
COPY pom.xml .
# 一级模块 pom
COPY biz/pom.xml ./biz/
COPY cluster/pom.xml ./cluster/
COPY common/pom.xml ./common/
COPY console/pom.xml ./console/
COPY core/pom.xml ./core/
COPY job/pom.xml ./job/
COPY ui/pom.xml ./ui/
COPY websocket/pom.xml ./websocket/
# 二级模块 pom（biz 子模块）
COPY biz/a2a/pom.xml ./biz/a2a/
COPY biz/account/pom.xml ./biz/account/
COPY biz/agent/pom.xml ./biz/agent/
COPY biz/hook/pom.xml ./biz/hook/
COPY biz/knowledge/pom.xml ./biz/knowledge/
COPY biz/mcp/pom.xml ./biz/mcp/
COPY biz/model/pom.xml ./biz/model/
COPY biz/params/pom.xml ./biz/params/
COPY biz/prompt/pom.xml ./biz/prompt/
COPY biz/rag/pom.xml ./biz/rag/
COPY biz/resource/pom.xml ./biz/resource/
COPY biz/sensitive/pom.xml ./biz/sensitive/
COPY biz/sk/pom.xml ./biz/sk/
COPY biz/skill/pom.xml ./biz/skill/
COPY biz/studio/pom.xml ./biz/studio/
COPY biz/tool/pom.xml ./biz/tool/

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
FROM ${JRE_IMAGE}

WORKDIR /app

# 以非root运行可缩小内核攻击面，配合cap_drop=ALL和no-new-privileges，阻断容器→宿主机逃逸路径
RUN groupadd -r apboa -g 1001 && \
    useradd -r -g apboa -u 1001 -m -s /sbin/nologin -d /app apboa && \
    chown -R apboa:apboa /app

# ---------- 安装 Python 3 + gosu（降权工具） ----------
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        python3 \
        python3-pip \
        python3-venv \
        gosu \
    && ln -sf /usr/bin/python3 /usr/bin/python \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# ---------- 安装 Node.js 22 ----------
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        ca-certificates \
    && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# 验证运行时安装
RUN python --version && node --version && npm --version

# 复制构建产物
COPY --from=build /build/console/target/console-*.jar app.jar

# 复制 Docker 环境配置文件到 classpath
COPY docker/backend/application-docker.yml /app/config/application-docker.yml

# 创建数据目录并将 /app 全部赋权给 apboa（logback 相对路径依赖 /app 可写）
RUN mkdir -p /app/data /app/logs && \
    chown -R apboa:apboa /app

EXPOSE 3060

ENV TZ=Asia/Shanghai
# ---------- 启动脚本 ----------
# 复制 entrypoint.sh（动态 JVM 堆计算 + 权限修复 + gosu 降权启动）
COPY docker/backend/entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

# 以 root 启动 → entrypoint.sh chown 修复 volume 挂载目录权限 → gosu 切到 apboa 用户启动 Java
# gosu 专为容器降权设计，不走 PAM 不弹密码；配合 cap_drop=ALL + no-new-privileges
ENTRYPOINT ["/app/entrypoint.sh"]
