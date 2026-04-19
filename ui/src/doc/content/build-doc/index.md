# 打包构建

本文档介绍如何从源码构建 Apboa 智能体平台的前后端，包括环境准备、后端构建、前端构建和部署说明。


## 一、环境要求

### 后端环境

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| **JDK** | 21+ | Java 运行与编译环境 |
| **Maven** | 3.8+ | Java 项目构建工具 |
| **MySQL** | 8.0+ | 数据库，需提前创建 `apboa` 库 |
| **Redis** | 6.0+ | 缓存与消息中间件 |

### 前端环境

| 依赖 | 版本要求 | 说明 |
|------|----------|------|
| **Node.js** | ^20.19.0 或 >=22.12.0 | JavaScript 运行环境 |
| **pnpm** | 最新版 | 包管理器（项目使用 pnpm） |


## 二、后端构建

### 1. 初始化数据库

:::info 前提条件
确保 MySQL 服务已启动，并创建了 `apboa` 数据库。
:::

```sql
CREATE DATABASE apboa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

执行项目根目录下的建表脚本：

```bash
# 执行 schema.sql 和增量脚本
mysql -u root -p apboa < docs/schema.sql
mysql -u root -p apboa < docs/2026-02-23-01.sql
mysql -u root -p apboa < docs/2026-02-27-01.sql
# ... 按日期顺序执行后续增量脚本
```

:::warning 提醒
增量 SQL 脚本需按日期顺序依次执行，不可跳过。
:::

### 2. 配置应用参数

编辑 `console/src/main/resources/application-dev.yml`，根据实际环境修改以下配置：

- **服务端口**：`server.port`（默认 `3060`）
- **MySQL 连接**：`spring.datasource.url`、`username`、`password`
- **Redis 连接**：`spring.data.redis.host`、`port`、`password`
- **JWT 密钥**：`jwt.secret`（建议生产环境通过环境变量 `JWT_SECRET` 注入）

### 3. Maven 构建

在项目根目录执行：

```bash
mvn clean package -DskipTests
```

构建产物位于 `console/target/console-1.0-SNAPSHOT.jar`。

### 4. 启动后端

```bash
java -jar console/target/console-1.0-SNAPSHOT.jar
```

可指定 Profile 启动：

```bash
java -jar console/target/console-1.0-SNAPSHOT.jar --spring.profiles.active=prod
```

## 三、前端构建

### 1. 安装依赖

```bash
cd ui
pnpm install
```

### 2. 开发模式

```bash
# 启动主应用开发服务（端口 3000）
pnpm dev

# 仅启动文档子应用
pnpm dev:doc
```

开发模式下，前端通过 Vite 代理将 `/api` 请求转发到后端 `http://127.0.0.1:3060`。

### 3. 生产构建

前端支持按目标构建，通过环境变量 `VITE_APP_TARGET` 控制：

| 构建命令 | 说明 | 产物 |
|----------|------|------|
| `pnpm build` | 构建主应用 + 文档子应用 | `dist/main.html` + `dist/doc.html` |
| `pnpm build:main` | 仅构建主应用 | `dist/main.html` |
| `pnpm build:doc` | 仅构建文档子应用 | `dist/doc.html` |

:::info 提示
构建完成后会自动在 `dist-zip/` 目录生成 `dist.zip` 压缩包，便于部署传输。
:::

### 4. 构建产物

```
dist/
├── main.html          # 主应用入口
├── doc.html           # 文档子应用入口
├── assets/            # 静态资源（JS、CSS、图片）
dist-zip/
└── dist.zip           # 自动打包的压缩文件
```


## 四、部署说明

### 前后端分离部署

1. 将前端 `dist/` 目录部署到 Nginx 等 Web 服务器
2. 配置 Nginx 反向代理，将 `/api` 请求转发到后端服务
3. 后端 JAR 包独立运行

Nginx 配置参考：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        root /path/to/dist;
        try_files $uri $uri/ /main.html;
    }

    # 文档子应用
    location /doc {
        root /path/to/dist;
        try_files $uri $uri/ /doc.html;
    }

    # API 反向代理
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

### 一体化部署

将前端构建产物放入后端静态资源目录，由 Spring Boot 直接托管：

1. 将 `dist/` 下的文件复制到 `console/src/main/resources/static/` 目录
2. 重新构建后端 JAR 包
3. 启动后端服务，前端页面通过后端端口直接访问


## 五、常见构建问题

### Maven 构建失败？

1. 检查 JDK 版本是否为 21+
2. 检查 Maven 版本是否为 3.8+
3. 执行 `mvn clean` 清理缓存后重试
4. 检查网络是否能访问 Maven 中央仓库

### 前端 pnpm install 失败？

1. 检查 Node.js 版本是否符合 `^20.19.0 || >=22.12.0`
2. 清除缓存：`pnpm store prune`
3. 删除 `node_modules` 后重新安装

### 前端构建后页面空白？

1. 检查 Nginx 配置中 `try_files` 是否正确
2. 检查 Vite 构建模式是否正确（`VITE_APP_TARGET` 设置）
3. 检查浏览器控制台是否有 API 请求 404 错误

### 后端启动报数据库连接失败？

1. 确认 MySQL 服务已启动
2. 确认 `apboa` 数据库已创建
3. 确认 `application-dev.yml` 中的数据库连接信息正确
4. 确认 MySQL 用户有 `apboa` 库的读写权限
