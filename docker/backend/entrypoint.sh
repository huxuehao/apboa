#!/bin/sh
set -e
# ============================================================
# Apboa Backend 容器启动脚本
# 功能：
#   1. 修复 volume 挂载目录的权限（宿主机挂载覆盖镜像内属主）
#   2. 通过 -XX:MaxRAMPercentage 让 JVM 自动按容器内存上限比例分配堆
#   3. 以非 root 用户 apboa 启动 Java 进程
# ============================================================

# 修复 volume 挂载目录权限（失败不阻塞启动）
chown -R apboa:apboa /app/logs /app/.apboa 2>/dev/null || true

# JVM 堆占容器内存上限的百分比，默认 75%
# -XX:+UseContainerSupport（Java 10+ 默认开启）让 JVM 自动读取 cgroup 限制
HEAP_PERCENTAGE=${BACKEND_JAVA_HEAP_PERCENTAGE:-75.0}

echo "JVM MaxRAMPercentage=${HEAP_PERCENTAGE}%"
exec gosu apboa java \
    -XX:MaxRAMPercentage=${HEAP_PERCENTAGE} \
    -XX:InitialRAMPercentage=${HEAP_PERCENTAGE} \
    -XX:+UseG1GC \
    -XX:+UseContainerSupport \
    -jar app.jar
