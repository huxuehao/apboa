# MCP 延迟初始化改造说明

## 背景

原链路在 Agent 创建时通过 `toolkit.registerMcpClient(mcpClient).block()` 注册 MCP 客户端。AgentScope 的默认 MCP 注册流程会先执行 `initialize()`，再执行 `listTools()` 发现工具。

这会导致一个问题：只要某个 Agent 关联的 MCP 服务不可达，即使用户当前问题完全不需要 MCP 工具，Agent 创建也会因为 MCP 初始化失败而中断，最终通过 AG-UI SSE 错误事件终止整轮对话。

## 改造目标

本次改造将 MCP 从“Agent 创建时同步连接”改为“两阶段使用”：

1. Agent 创建阶段只注册 MCP 工具签名，不连接 MCP 服务。
2. LLM 首次实际调用某个 MCP 工具时，才创建并初始化 MCP client。
3. MCP 调用失败时返回工具错误结果给 LLM，由 LLM 自然告知用户。
4. 非 MCP 话题不再受 MCP 服务状态影响。

## 改造后链路

### 工具签名发现

MCP 工具签名缓存到 `mcp_server.tool_schemas` 字段中，内容为 MCP `listTools()` 返回的工具列表 JSON。

签名刷新入口：

- 新增 MCP 服务后触发刷新。
- 更新 MCP 服务后触发刷新。
- 应用启动后自动扫描启用中但 `tool_schemas` 为空的 MCP 服务并异步刷新。
- Agent 创建时如果发现关联 MCP 的 `tool_schemas` 为空，也会异步触发刷新。

刷新成功后，系统会发布关联 Agent 的重注册消息，使 Agent 重新加载最新 MCP 工具签名。

### Agent 创建

`ToolkitFactory` 不再调用 `toolkit.registerMcpClient(...).block()`。

新链路为：

```text
ToolkitFactory
  -> McpClientFactory.getLazyMcpTools(agentDefinition)
  -> 读取 mcp_server.tool_schemas
  -> 为每个缓存工具注册 LazyMcpAgentTool
```

因此 Agent 创建阶段不会连接 MCP 服务，也不会因为 MCP 服务不可达而中断普通对话。

### 工具调用

当 LLM 选择调用某个 MCP 工具时：

```text
LazyMcpAgentTool.callAsync()
  -> 创建真实 McpClientWrapper
  -> initialize()
  -> callTool()
  -> 将 MCP 结果转换为 ToolResultBlock
```

如果 MCP 服务不可达或调用失败，异常会被捕获并转换为 `ToolResultBlock.error(...)`，返回给 LLM。SSE 对话链路不会因此被外层异常打断。

## 关键代码改动

- `core/src/main/java/com/hxh/apboa/core/tool/ToolkitFactory.java`
  Agent 创建时改为注册懒加载 MCP 工具，不再同步注册真实 MCP client。

- `core/src/main/java/com/hxh/apboa/core/mcp/McpClientFactory.java`
  新增基于缓存工具签名构造 `LazyMcpAgentTool` 的逻辑；缓存为空时触发异步签名刷新。

- `core/src/main/java/com/hxh/apboa/core/mcp/LazyMcpAgentTool.java`
  新增懒加载 MCP 工具实现。工具签名来自缓存，真实 MCP 连接延迟到首次调用。

- `core/src/main/java/com/hxh/apboa/core/mcp/ToolSchemaRefresherImpl.java`
  新增 MCP 工具签名刷新实现；刷新成功后写入 `tool_schemas` 并发布 Agent 重注册消息。

- `common/src/main/java/com/hxh/apboa/common/mcp/ToolSchemaRefresher.java`
  新增签名刷新接口，用于打断 biz mcp 模块与 core 模块之间的循环依赖。

- `biz/mcp/src/main/java/com/hxh/apboa/mcp/service/impl/McpServerServiceImpl.java`
  新增和更新 MCP 服务时触发工具签名刷新。

- `common/src/main/java/com/hxh/apboa/common/entity/McpServer.java`
  新增 `toolSchemas` 字段映射。

## 数据库变更

老库增量脚本：

```sql
ALTER TABLE mcp_server ADD COLUMN tool_schemas TEXT COMMENT '缓存的上次成功获取的MCP工具列表（JSON格式）';
```

脚本位置：

```text
docs/2026-05-13-01.sql
```

全新初始化脚本也需要包含该字段，已同步到：

```text
docs/once_db_init/db_init.sql
```

本次不修改 `docs/schema.sql`。

## 行为说明

如果 MCP 服务已有 `tool_schemas` 缓存：

- Agent 能看到 MCP 工具。
- MCP 服务当前不可达时，只有实际调用该工具才会返回工具错误。
- 非 MCP 对话不受影响。

如果 MCP 服务从未成功生成过 `tool_schemas`：

- Agent 暂时无法看到具体 MCP 工具，因为缺少工具名称、描述和参数。
- 系统会异步刷新签名。
- 刷新成功后自动重注册关联 Agent，下一轮即可看到工具。

如果 MCP 服务一直不可达且没有历史缓存：

- Agent 无法凭空发现工具签名。
- 普通对话仍可继续。

## 注意事项

- MCP 工具签名依赖上一次成功 `listTools()` 的缓存。
- MCP 服务配置变更后，需要等待异步刷新成功和 Agent 重注册完成。
- 刷新逻辑使用去重集合，避免多个 Agent 创建请求同时重复刷新同一个 MCP。
- 刷新器直接通过协议配置创建临时 MCP client，并通过 mapper 更新缓存字段，避免与 MCP service/factory 形成 Spring 循环依赖。
