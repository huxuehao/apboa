ARG NODE_IMAGE=node:22-alpine
ARG NGINX_IMAGE=nginx:alpine
# ============================================================
# 阶段一：Node 构建前端
# ============================================================
# 构建镜像（由 docker-compose.yml args 传入，支持私有仓库）
FROM ${NODE_IMAGE} AS build

WORKDIR /build

# 接收构建参数（由 docker-compose.yml 的 args 传入，值来自 .env）
ARG VITE_APP_BASE_API=/web
ARG VITE_APP_CONTEXT_PATH=/web
ENV VITE_APP_BASE_API=${VITE_APP_BASE_API}
ENV VITE_APP_CONTEXT_PATH=${VITE_APP_CONTEXT_PATH}
# 安装 pnpm
RUN corepack enable && corepack prepare pnpm@9.4.0 --activate

# 复制 NPM 私有仓库配置（在线模式为空壳，离线模式用户编辑）
COPY docker/npm/.npmrc ./

# 复制依赖文件
COPY ui/package.json ui/pnpm-lock.yaml ./

# 安装依赖
RUN pnpm install --frozen-lockfile

# 复制前端源码
COPY ui/ ./

# 构建主应用（输出到 dist-main）
RUN VITE_APP_TARGET=main pnpm run build-only:main

# 构建文档应用（输出到 dist-doc）
RUN VITE_APP_TARGET=doc pnpm run build-only:doc

# ============================================================
# 阶段二：Nginx 运行
# ============================================================
# 运行镜像（由 docker-compose.yml args 传入，支持私有仓库）
FROM ${NGINX_IMAGE}

# 复制构建产物：dist-main 内容映射到 /usr/share/nginx/html/web/
COPY --from=build /build/dist-main /usr/share/nginx/html/web

# 复制构建产物：dist-doc 内容映射到 /usr/share/nginx/html/web/doc
COPY --from=build /build/dist-doc /usr/share/nginx/html/web/doc

# 复制 Nginx 配置
COPY docker/nginx/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
