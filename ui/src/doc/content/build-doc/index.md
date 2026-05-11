# 打包构建与部署

本文档介绍 Apboa 智能体平台的四种部署方案，从开发环境到生产环境的完整指南。


## 一、环境要求

### 后端环境

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| **JDK** | 21+ | Java 运行与编译环境 |
| **Maven** | 3.8+ | Java 项目构建工具 |
| **MySQL** | 8.0+ | 数据库 |
| **Redis** | 6.0+ | 缓存与消息中间件 |

### 前端环境

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| **Node.js** | ^20.19.0 或 >=22.12.0 | JavaScript 运行环境 |
| **pnpm** | 最新版 | 包管理器（项目使用 pnpm） |

### Docker 环境（方案三 / 四）

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| **Docker** | >= 20.10 | 容器引擎 |
| **Docker Compose** | >= 2.0 | 容器编排 |


## 二、数据库初始化

:::info 前提条件
确保 MySQL 服务已启动。
:::

```sql
-- 创建数据库（如尚未创建）
CREATE DATABASE IF NOT EXISTS `apboa` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

执行项目 `docs/once_db_init/` 下的初始化脚本：

```bash
mysql -u root -p apboa < docs/once_db_init/db_init.sql
```

:::warning 提醒
db_init.sql 已包含建库语句（`CREATE DATABASE IF NOT EXISTS`）和全量表结构及初始数据，一条命令即可完成初始化。
:::


## 三、部署方案对比

| 方案 | 适用场景 | 复杂度 | 依赖项 |
|------|---------|--------|--------|
| 方案一：前后端分离 | 传统部署，灵活可控 | 中等 | 自行安装 MySQL/Redis |
| 方案二：一体化 JAR | 单机快速部署 | 低 | 自行安装 MySQL/Redis |
| 方案三：Docker Compose | 一键部署，开箱即用 | 低 | Docker |
| 方案四：Dockerfile 自定义 | 自定义镜像，集成到已有平台 | 较高 | Docker |


## 四、方案一：前后端分离部署

手动构建前后端，分别部署到服务器。前端通过 Nginx 托管，API 请求反向代理到后端。

### 4.1 构建后端

```bash
# 在项目根目录执行
mvn clean package -DskipTests -pl console -am
```

构建产物：`console/target/console-1.0-SNAPSHOT.jar`

启动后端：

```bash
java -jar console/target/console-1.0-SNAPSHOT.jar
```

可指定 Profile：

```bash
java -jar console/target/console-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

### 4.2 构建前端

```bash
cd ui
pnpm install
```

生产构建：

| 构建命令 | 说明 | 产物 |
|----------|------|------|
| `pnpm build` | 构建主应用 + 文档子应用 | `dist/main.html` + `dist/doc.html` |
| `pnpm build:main` | 仅构建主应用 | `dist/main.html` |
| `pnpm build:doc` | 仅构建文档子应用 | `dist/doc.html` |

构建产物位于 `ui/dist/`，同时自动生成 `ui/dist-zip/dist.zip`。

### 4.3 Nginx 部署配置

将 `ui/dist/` 目录上传到服务器，参考以下 Nginx 配置：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源（主应用）
    location / {
        root /path/to/dist;
        try_files $uri $uri/ /main.html;
    }

    # 文档子应用
    location /doc {
        root /path/to/dist;
        try_files $uri $uri/ /doc.html;
    }

    # API 反向代理（后端 ApiPathRewriteFilter 自动剥离 /api 前缀）
    location /api/ {
        proxy_pass http://127.0.0.1:3060/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }
}
```

:::info 路径兼容说明
后端 `ApiPathRewriteFilter` 自动剥离 `/api/` 和 `/web/api/` 请求前缀，因此开发环境（Vite 代理）和生产环境（Nginx）均可使用 `/api` 前缀，无需额外配置。
:::


## 五、方案二：前后端一体化 JAR

将前端构建产物嵌入后端 JAR，一个文件即可运行完整系统。

### 5.1 启用 UI 模块

编辑 `console/pom.xml`，取消 `ui` 依赖的注释：

```xml
<dependency>
    <groupId>com.hxh.apboa</groupId>
    <artifactId>ui</artifactId>
</dependency>
```

### 5.2 打包

```bash
mvn clean package -DskipTests
```

构建产物：`console/target/console-1.0-SNAPSHOT.jar`（内含前端静态资源）。

### 5.3 启动

```bash
java -jar console/target/console-1.0-SNAPSHOT.jar
```

访问 `http://localhost:3060` 即可打开系统。

:::warning 注意
一体化 JAR 的前端资源每次打包都会重新构建，首次构建耗时较长。开发阶段建议使用方案一的前后端分离模式。
:::


## 六、方案三：Docker Compose 一键部署

一键启动 MySQL、Redis、pgvector（向量库）、后端、前端全部服务。

### 6.1 配置

编辑 `docker/.env`，按需修改密码等配置：

```bash
MYSQL_ROOT_PASSWORD=your_password
REDIS_PASSWORD=your_password
PG_PASSWORD=your_password
JWT_SECRET=your_secret
```

### 6.2 启动

```bash
# 构建并启动
cd docker
docker compose up -d --build
```


```bash
# 停止服务
docker compose down

# 停止服务，并清理数据（慎用！！！）
docker compose down -v
```


```bash
docker compose stop   # 停止所有
docker compose start  # 启动所有（不重新构建）
```


```bash
# 重新构建后端（不使用缓存）
docker compose build --no-cache apboa-backend

# 重新构建并启动
docker compose up -d --build apboa-backend
```


```bash
# 重新构建所有镜像
docker-compose build --no-cache

# 或构建并启动
docker-compose up -d --build
```

### 6.3 访问

- 主应用：`http://localhost/web/`
- 默认管理员：`admin` / `Admin@123.com`

### 6.4 服务架构

```
┌─────────────────────────────────────────┐
│              Nginx (:80)                │
│         apboa-frontend                  │
│    /web/   /web/doc/   /web/api/        │
└──────────────┬──────────────────────────┘
               │ proxy_pass
┌──────────────▼──────────────────────────┐
│        Spring Boot (:3060)              │
│         apboa-backend                   │
└──────┬──────────┬──────────┬────────────┘
       │          │          │
  ┌────▼──┐  ┌───▼───┐  ┌──▼──────┐
  │ MySQL │  │ Redis │  │ pgvector│
  │ :3306 │  │ :6379 │  │ :5432   │
  └───────┘  └───────┘  └─────────┘
```

### 6.5 使用外置服务

如果已有外部 MySQL / Redis / pgvector，修改 `.env` 中的 `*_HOST` 为外部地址，并注释 `docker-compose.yml` 中对应服务块。

### 6.6 离线部署

内网环境下需配置私有仓库，详见 `docker/README.md` 中的"离线/内网部署"章节。


## 七、方案四：Dockerfile 自定义部署

基于项目提供的 Dockerfile 自行构建镜像，适用于集成到已有容器平台的场景。

### 7.1 构建后端镜像

```bash
# 在项目根目录执行
docker build \
  -f docker/backend/Dockerfile \
  -t apboa-backend:latest \
  .
```

### 7.2 构建前端镜像

```bash
docker build \
  -f docker/frontend/Dockerfile \
  --build-arg VITE_APP_BASE_API=/web \
  --build-arg VITE_APP_CONTEXT_PATH=/web \
  -t apboa-frontend:latest \
  .
```

### 7.3 运行容器

```bash
# 创建网络
docker network create apboa_network

# 启动后端（确保 MySQL / Redis 已就绪）
docker run -d \
  --name apboa-backend \
  --network apboa_network \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e MYSQL_HOST=your-mysql-host \
  -e MYSQL_PASSWORD=your_password \
  -e REDIS_HOST=your-redis-host \
  -e REDIS_PASSWORD=your_password \
  -p 3060:3060 \
  apboa-backend:latest

# 启动前端
docker run -d \
  --name apboa-frontend \
  --network apboa_network \
  -p 80:80 \
  apboa-frontend:latest
```

### 7.4 Dockerfile 文件说明

| 文件 | 用途 |
|------|------|
| `docker/backend/Dockerfile` | Maven 多阶段构建后端 JAR，JRE 运行镜像 |
| `docker/frontend/Dockerfile` | Node / pnpm 多阶段构建前端，Nginx 运行镜像 |
| `docker/nginx/nginx.conf` | Nginx 反向代理配置模板 |
| `docker/maven/settings.xml` | Maven 私有仓库配置（离线部署） |
| `docker/npm/.npmrc` | NPM 私有仓库配置（离线部署） |


## 八、常见问题

### Maven 构建失败？

1. 检查 JDK 版本是否为 21+
2. 检查 Maven 版本是否为 3.8+
3. 执行 `mvn clean` 清理缓存后重试
4. 检查网络是否能访问 Maven 仓库

### 前端 pnpm install 失败？

1. 检查 Node.js 版本是否符合 `^20.19.0 || >=22.12.0`
2. 清除缓存：`pnpm store prune`
3. 删除 `node_modules` 后重新安装

### 前端构建后页面空白？

1. 检查 Nginx `try_files` 是否指向正确的 HTML 入口
2. 检查 `VITE_APP_TARGET` 构建模式是否正确
3. 检查 `VITE_APP_BASE_API` 和 Nginx 代理路径是否匹配

### 后端启动报数据库连接失败？

1. 确认 MySQL 服务已启动
2. 确认 `apboa` 数据库已通过 `db_init.sql` 初始化
3. 确认配置文件中的数据库连接信息正确
4. 确认 MySQL 用户有 `apboa` 库的读写权限

### Docker 构建镜像时下载依赖失败？

1. 在线环境：检查 `DOCKER_REGISTRY` 是否留空（默认 Docker Hub）
2. 离线环境：按 `docker/README.md` 配置私有仓库后重新构建
