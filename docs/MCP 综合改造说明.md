# MCP 综合改造说明

## 1. 改造背景

本次 MCP 改造不是一次性完成的，而是分几步逐步收口：

1. 先解决运行时稳定性问题，避免 MCP 不可用时拖垮 Agent 创建和工具调用。
2. 再把工具目录、全局治理、Agent 局部选择三层语义拆清楚。
3. 最后补上运行时自动降级闭环，让“上次可用、此刻不可用”的状态能够自动回写并传播。

最终目标是把以下几件事同时做好：

- MCP 工具注册不要因为连接时机不对而放大故障
- 同一 MCP 服务下几十个工具不要重复初始化连接
- 前端、数据库、运行时对“启用”“连接”“可用工具”的理解一致
- MCP 运行时出故障后，不只是单次报错，而是能自动收口到系统真相

## 2. 整体设计原则

本次改造统一遵循这些原则：

1. 显式优于隐式  
   连接、刷新工具、恢复都走显式入口，不把副作用偷偷埋进读路径。

2. 治理状态和运行状态分离  
   `enabled` 表示业务上是否允许参与系统，`activation_status` 表示技术上当前是否可用。

3. 目录真相和运行时连接分离  
   工具目录提前落库，运行时只在真正调用工具时建立连接。

4. 尽量按 MCP 服务级复用资源  
   同一 MCP 下所有工具共享连接上下文，而不是每个工具各建一份。

5. 自动降级只做收口，不做偷偷恢复  
   运行时失败可以触发自动降级，但恢复只能靠手动连接、刷新工具，或改配置后的自动重连。

## 3. 第一阶段：延迟初始化与稳定性止血

### 3.1 解决的问题

第一阶段重点解决两个问题：

1. `listTools()` 为空或异常时，读路径和启动路径可能反复刷新，存在死循环隐患。
2. 同一 MCP 服务下多个工具第一次被调用时，原先可能各自初始化一遍连接，造成重复建连。

### 3.2 主要改动

#### 3.2.1 工具注册改为惰性注册

Agent 创建阶段不再直接初始化 MCP client，而是只注册惰性工具：

- Agent 构建工具箱时只读取缓存目录
- 真正执行某个 MCP 工具时才初始化连接

这样即使某个 MCP 当前不可用，也不会影响普通 Agent 的创建和普通对话。

#### 3.2.2 去掉隐式刷新路径

刷新工具目录只保留显式入口：

- 手动连接
- 手动刷新工具
- 修改配置后的自动重连

不再允许：

- 启动期自动补刷新
- 读路径顺手触发刷新

#### 3.2.3 同一 MCP 服务共享连接上下文

连接上下文按以下维度复用：

- `mcpServerId`
- `activationRevision`
- `configHash`

含义是：

- 同一 MCP 服务、同一代次、同一配置只初始化一次
- 同一服务下多个工具共享一个已初始化 client
- 配置变化或重新连接成功后，旧上下文自动失效

### 3.3 结果

第一阶段完成后：

- Agent 创建不再因 MCP 不可用而中断
- 同一 MCP 下多个工具不会各自重复初始化连接
- 工具刷新行为收敛成可控的显式动作

## 4. 第二阶段：工具目录治理与前端语义统一

### 4.1 解决的问题

第二阶段主要解决这些语义混乱：

1. 运行时还在直接依赖 `mcp_server.tool_schemas`
2. 前端 Agent 侧容易按 `enabled` 看到 MCP，但它未必真的会产出可用工具
3. 缺少“全局工具治理 + Agent 局部缩小范围”的双层能力

### 4.2 主要改动

#### 4.2.1 工具目录真相源切到 `mcp_tool`

引入：

- `mcp_tool`
- `agent_mcp_tool`
- `agent_mcp_servers.exposure_mode`

其中：

- `mcp_tool` 记录某个 MCP 服务当前或历史发现过的工具目录
- `enabled` 表示该工具是否全局可用
- `missing` 表示该工具是否已从当前服务端目录中消失
- `agent_mcp_tool` 支持 Agent 局部缩小范围
- `exposure_mode` 区分“继承全部全局可用工具”还是“仅暴露局部选中的工具”

#### 4.2.2 运行时工具装配逻辑收口

运行时装配 MCP 工具时，会同时检查：

1. `mcp_server.enabled = true`
2. `mcp_server.activation_status = ACTIVE`
3. `mcp_tool.enabled = true`
4. `mcp_tool.missing = false`
5. Agent 当前 `exposure_mode`

只有同时满足这些条件的工具，才会真正注册到 Agent 运行时。

#### 4.2.3 前端语义改成“启用 / 连接 / 刷新工具”

前端不再把 `activation_status` 直接用“激活”表达，而是改成：

- `启用`：业务上是否允许参与系统
- `连接`：当前是否连通并成功加载工具目录
- `刷新工具`：在当前配置基础上重新获取工具目录

这样用户能明显分清：

- 我要不要让这个 MCP 参与系统
- 它现在到底能不能用
- 它的工具目录是不是最新的

### 4.3 结果

第二阶段完成后：

- MCP 工具目录有了独立真相源
- Agent 侧不再只按 `enabled` 判断 MCP 是否可选
- 全局治理和 Agent 局部选择都能落地
- 前端文案、状态展示和运行时判断基本统一

## 5. 第三阶段：运行时自动降级闭环

### 5.1 解决的问题

前两阶段完成后，系统已经能显式连接、显式刷新，并且运行时工具注册也变得可控。

但还有一个空档：

- 某个 MCP 曾经连接成功，库里仍然是 `ACTIVE`
- 此刻运行时真的去调用工具时，MCP 已经不可用了
- 系统只能让这一次工具调用报错，不能自动把“当前已不可用”回写成系统真相

第三阶段补的就是这条闭环。

### 5.2 主要改动

#### 5.2.1 新增自动降级相关字段

在 `mcp_server` 上新增：

- `failure_source`
- `activation_status_changed_at`
- `runtime_fail_threshold`

其中：

- `runtime_fail_threshold` 默认值为 `3`
- 当阈值为 `0` 时，表示关闭自动降级
- `failure_source=RUNTIME_AUTO_DEGRADE` 表示当前失败来源是运行时自动降级

#### 5.2.2 只统计连接或传输类失败

自动降级不会把所有工具异常都算进去，只统计：

- 初始化失败
- 连接超时
- transport closed
- EOF
- IO 异常
- 连接断开等链路类问题

工具业务错误、参数错误、上游正常返回的业务失败，不参与自动降级。

#### 5.2.3 集群共享连续失败计数

连续失败计数放在 Redis 中，按以下维度隔离：

- `serverId`
- `activationRevision`
- `configHash`

这样可以避免旧配置、旧代次的失败污染当前状态。

同时满足：

- 集群共享计数
- 成功即清零
- 达阈值才降级

#### 5.2.4 自动回写状态并触发 Agent 重注册

当连续失败达到阈值后：

1. 尝试把 `mcp_server.activation_status` 从 `ACTIVE` 原子更新为 `FAILED`
2. 写入：
   - `failure_source=RUNTIME_AUTO_DEGRADE`
   - `activation_status_changed_at=now`
   - `health_status=UNHEALTHY`
   - `needs_sync=true`
3. 仅在 `ACTIVE -> FAILED` 成功落库时广播一次 Agent 重注册

这样后续 Agent 工具箱会把该 MCP 工具摘掉，运行时和管理端的真相重新对齐。

#### 5.2.5 自动降级后保留“上次缓存”只读态

自动降级后不会立刻抹掉旧工具目录，而是：

- 管理端仍可查看上次缓存的工具列表
- 但重新连接成功前不允许继续修改工具治理

前端会明确提示：

- 当前展示的是“上次缓存”
- 当前处于只读态

后端也会做治理写保护，避免只靠前端按钮禁用。

### 5.3 恢复边界

自动降级后，不做运行时自动恢复。

允许的恢复路径只有：

1. 用户手动点击“连接”
2. 用户手动点击“刷新工具”
3. 用户修改 MCP 配置后，保存时自动重连成功

### 5.4 结果

第三阶段完成后，系统不再只是“单次工具调用失败”，而是具备：

- 工具级失败兜底
- 连续失败熔断
- 状态回写
- Agent 重注册
- 前端只读态提示

这条从运行时到管理面的闭环就补齐了。

## 6. 关键数据结构与状态

### 6.1 `mcp_server`

本次改造后，`mcp_server` 同时承担三类信息：

1. 治理开关
   - `enabled`

2. 连接与同步状态
   - `activation_status`
   - `activation_message`
   - `last_activation_time`
   - `last_tool_sync_time`
   - `tool_count`
   - `activation_revision`
   - `config_hash`
   - `needs_sync`

3. 自动降级状态
   - `failure_source`
   - `activation_status_changed_at`
   - `runtime_fail_threshold`

### 6.2 `mcp_tool`

`mcp_tool` 是当前运行时工具目录真相源，核心字段包括：

- `tool_name`
- `enabled`
- `missing`
- `last_seen_at`
- `schema_hash`

### 6.3 `agent_mcp_tool`

`agent_mcp_tool` 用于 Agent 局部缩小可暴露工具范围，只在 `SELECTED_ONLY` 模式下生效。

## 7. SQL 变更落点

已运行项目的增量脚本：

- [2026-05-18-01.sql](/D:/projects/ai-apboa/apboa/docs/2026-05-18-01.sql)

初始化脚本：

- [db_init.sql](/D:/projects/ai-apboa/apboa/docs/once_db_init/db_init.sql)

说明：

- `docs/` 根目录下已有 SQL 均按增量脚本保留
- 本次 MCP 改造相关的增量 SQL 已统一收口到 `2026-05-18-01.sql`
- 初始化场景的完整表结构同步落到 `docs/once_db_init/db_init.sql`
- 本次没有修改 `docs/schema.sql`

## 8. 主要代码落点

后端：

- [ToolkitFactory.java](/D:/projects/ai-apboa/apboa/core/src/main/java/com/hxh/apboa/core/tool/ToolkitFactory.java)
- [McpClientFactory.java](/D:/projects/ai-apboa/apboa/core/src/main/java/com/hxh/apboa/core/mcp/McpClientFactory.java)
- [LazyMcpAgentTool.java](/D:/projects/ai-apboa/apboa/core/src/main/java/com/hxh/apboa/core/mcp/LazyMcpAgentTool.java)
- [ToolSchemaRefresherImpl.java](/D:/projects/ai-apboa/apboa/core/src/main/java/com/hxh/apboa/core/mcp/ToolSchemaRefresherImpl.java)
- [McpServerServiceImpl.java](/D:/projects/ai-apboa/apboa/biz/mcp/src/main/java/com/hxh/apboa/mcp/service/impl/McpServerServiceImpl.java)
- [AgentMcpServerServiceImpl.java](/D:/projects/ai-apboa/apboa/biz/mcp/src/main/java/com/hxh/apboa/mcp/service/impl/AgentMcpServerServiceImpl.java)
- [McpRuntimeDegradeServiceImpl.java](/D:/projects/ai-apboa/apboa/biz/mcp/src/main/java/com/hxh/apboa/mcp/service/impl/McpRuntimeDegradeServiceImpl.java)
- [McpRuntimeFailureClassifier.java](/D:/projects/ai-apboa/apboa/biz/mcp/src/main/java/com/hxh/apboa/mcp/service/impl/McpRuntimeFailureClassifier.java)

前端：

- [McpCard.vue](/D:/projects/ai-apboa/apboa/ui/src/components/mcp/McpCard.vue)
- [McpForm.vue](/D:/projects/ai-apboa/apboa/ui/src/components/mcp/McpForm.vue)
- [index.vue](/D:/projects/ai-apboa/apboa/ui/src/views/Mcp/index.vue)
- [useMcpPresentation.ts](/D:/projects/ai-apboa/apboa/ui/src/composables/useMcpPresentation.ts)
- [AgentFormKnowledge.vue](/D:/projects/ai-apboa/apboa/ui/src/components/agent/AgentFormKnowledge.vue)

## 9. 验证建议

当前 `docs/` 目录下与本次 MCP 改造相关的说明文档也已统一收口，只保留本文作为后续维护入口。

建议至少验证以下场景：

1. 未连接 MCP 时，普通 Agent 创建与普通聊天不受影响
2. 同一 MCP 下多个工具首次并发调用时，只初始化一次共享连接
3. MCP 工具目录刷新后，`mcp_tool` 和 Agent 可见工具同步变化
4. 全局禁用工具后，Agent 运行时不再注册该工具
5. `SELECTED_ONLY` 模式下，只注册 Agent 选中的工具
6. MCP 连续连接或传输失败达到阈值后，自动写回 `FAILED`
7. 自动降级后，管理端展示“上次缓存”并进入只读态
8. 手动连接或刷新工具成功后，自动降级状态恢复
