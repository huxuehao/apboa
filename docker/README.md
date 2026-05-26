# Apboa Docker 部署指南

## 目录结构

```
docker/
├── .env                          # 环境变量配置
├── docker-compose.yml            # 服务编排
├── maven/
│   └── settings.xml              # Maven 私有仓库配置（离线部署）
├── npm/
│   └── .npmrc                    # NPM 私有仓库配置（离线部署）
├── backend/
│   ├── Dockerfile                # 后端镜像构建
│   └── application-docker.yml    # Docker 环境配置
├── frontend/
│   └── Dockerfile                # 前端镜像构建
├── nginx/
│   └── nginx.conf                # Nginx 配置
└── README.md                     # 本文档
```

## 快速开始

### 前置要求

- Docker >= 20.10
- Docker Compose >= 2.0
- 构建内存建议 >= 4GB

### 1. 配置环境变量

编辑 `docker/.env` 文件，按需修改密码和端口：

```bash
# 必改项（生产环境）
MYSQL_ROOT_PASSWORD=your_strong_password
REDIS_PASSWORD=your_redis_password
PG_PASSWORD=your_pg_password
JWT_SECRET=your_jwt_secret
```

### 2. 构建并启动

```bash
cd docker
docker compose up -d --build
```

### 3. 访问系统

- 主应用：http://localhost/web/
- 默认管理员账号：`admin` / 密码：`Admin@123.com`（MD5 加密存储）

### 4. 查看日志

```bash
# 所有服务
docker compose logs -f

# 指定服务
docker compose logs -f backend
```

### 5. 重启所有服务

```bash
docker compose restart
```

### 6. 重启单个服务

```bash
# 例如只重启后端
docker compose restart apboa-backend
```

### 7. 停止后再启动

```bash
docker compose stop   # 停止所有
docker compose start  # 启动所有（不重新构建）
```

### 8. 重新构建特定服务

```bash
# 重新构建后端（不使用缓存）
docker compose build --no-cache apboa-backend

# 重新构建并启动
docker compose up -d --build apboa-backend
```

### 9. 重新构建所有服务

```bash
# 重新构建所有镜像
docker-compose build --no-cache

# 或构建并启动
docker-compose up -d --build
```

### 10. 停止服务

```bash
docker compose down
```

### 11. 清理数据（慎用）

```bash
docker compose down -v
```

## 离线/内网部署

在内网环境（服务器不联网）下，需要将三类依赖指向私有仓库：

| 来源 | 配置文件 | 在线（默认） | 离线操作 |
|------|---------|-------------|---------|
| Docker 镜像 | `.env` → `DOCKER_REGISTRY` | 留空 → Docker Hub | 设为私有 Registry 前缀 |
| Maven 依赖 | `maven/settings.xml` | 注释状态 → Maven Central | 取消注释，填写 Nexus 地址 |
| NPM 依赖 | `npm/.npmrc` | 注释状态 → npmjs.org | 取消注释 registry 行 |

### 操作步骤

1. **配置 Docker 私有仓库**  
   编辑 `.env`，填写镜像仓库前缀：
   ```bash
   DOCKER_REGISTRY=harbor.your-company.com/apboa/
   ```

2. **配置 Maven 私有仓库**  
   编辑 `maven/settings.xml`，取消 `<mirrors>` 和 `<servers>` 注释，填写私有仓库地址和凭证：
   ```xml
   <mirrors>
     <mirror>
       <id>private-maven</id>
       <mirrorOf>*</mirrorOf>
       <url>https://nexus.your-company.com/repository/maven-public/</url>
     </mirror>
   </mirrors>
   ```

3. **配置 NPM 私有仓库**  
   编辑 `npm/.npmrc`，取消注释并填写地址：
   ```
   registry=https://nexus.your-company.com/repository/npm-public/
   ```

4. **构建并启动**
   ```bash
   docker compose build --no-cache
   docker compose up -d
   ```

## 使用外置服务

如果已有外部 MySQL / Redis / pgvector，按以下步骤配置：

### 外置 MySQL

1. 修改 `.env`：
   ```bash
   MYSQL_HOST=127.0.0.1
   MYSQL_PORT=3306
   MYSQL_USER=your_user
   MYSQL_PASSWORD=your_password
   ```

2. 手动初始化数据库：
   ```bash
   mysql -h 127.0.0.1 -u root -p < ../docs/once_db_init/db_init.sql
   ```

3. 在 `docker-compose.yml` 中注释掉 `mysql` 服务块

4. 在 `docker-compose.yml` 中删除 `backend.depends_on` 中的 `mysql` 项

### 外置 Redis

1. 修改 `.env` 中 `REDIS_HOST` 指向外部地址
2. 在 `docker-compose.yml` 中注释掉 `redis` 服务块
3. 删除 `backend.depends_on` 中的 `redis` 项

### 外置向量库

1. 修改 `.env` 中 `VECTOR_STORE_TYPE` 及对应连接信息
2. 不启用对应向量库 profile 时，Docker Compose 不会启动内置向量库服务

### 切换向量库类型

修改 `.env` 中的 `VECTOR_STORE_TYPE`：

```bash
# 使用 pgvector（默认）
VECTOR_STORE_TYPE=pgvector
docker compose --profile pgvector up -d apboa-pgvector

# 使用 Milvus
VECTOR_STORE_TYPE=milvus

# 使用 Qdrant
VECTOR_STORE_TYPE=qdrant

# 使用 Elasticsearch
VECTOR_STORE_TYPE=elasticsearch
ELASTICSEARCH_IMAGE=docker.elastic.co/elasticsearch/elasticsearch:8.15.0
ELASTICSEARCH_URIS=http://apboa-elasticsearch:9200

# 启动 Docker 内置 Elasticsearch
docker compose --profile elasticsearch up -d apboa-elasticsearch

# 禁用向量库
VECTOR_STORE_TYPE=
```

## 自定义前端配置

修改 `.env` 中的前端参数（需重新构建前端镜像）：

```bash
# 前端 API 基础路径
VITE_APP_BASE_API=/web

# 前端上下文路径
VITE_APP_CONTEXT_PATH=/web
```

修改后重新构建：
```bash
docker compose build frontend --no-cache
docker compose up -d frontend
```

## 端口说明

| 服务 | 默认端口 | 说明 |
|------|---------|------|
| Nginx（前端） | 80 | 浏览器访问入口 |
| Backend | 3060 | API 服务 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| pgvector | 5432 | 向量库 |
| Elasticsearch | 9200 | 可选向量库（`--profile elasticsearch`） |

## 故障排查

### 后端启动失败

```bash
# 查看后端日志
docker compose logs backend

# 常见原因：
# 1. MySQL 未就绪 → 等待 mysql 健康检查通过
# 2. 数据库未初始化 → 检查 db_init.sql 是否正确挂载
# 3. Redis 连接失败 → 检查密码和网络
```

### 前端无法访问 API

```bash
# 检查 Nginx 日志
docker compose logs frontend

# 确认 Nginx 能解析 backend 主机名
docker compose exec frontend ping backend
```

### 重新初始化数据库

```bash
# 停止并删除 mysql 数据卷
docker compose down -v mysql
# 重新启动
docker compose up -d mysql
```
