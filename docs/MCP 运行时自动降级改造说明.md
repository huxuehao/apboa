# MCP 运行时自动降级改造说明

## 1. 改造背景

在前两阶段的 MCP 改造完成后，系统已经具备了这些能力：

1. 显式区分 `启用` 和 `连接状态`
2. 同一 MCP 服务下多个工具共享连接初始化结果
3. Agent 运行时按 `mcp_tool` 和治理配置注册工具

但仍然存在一个空档：

- 某个 MCP 上一次连接成功，当前库里仍然是 `ACTIVE`
- 运行时 Agent 实际调用时，MCP 服务已经不可用
- 这时系统只能让单次工具调用报错，不能自动把“当前已不可用”收敛回系统真相

本次改造的目标，就是补上这一段闭环：

- 运行时连续失败后自动降级
- 自动把 `mcp_server` 状态回写为不可用
- 自动触发相关 Agent 重注册
- 恢复仍然只允许显式连接、刷新工具，或改配置后的自动重连

## 2. 设计原则

本次实现遵循以下原则：

1. 不在读路径里偷偷恢复  
   自动降级后，不做半开探测，不做运行时自动恢复。

2. 按 MCP 服务级收口  
   首版不做工具级自动降级，只要同一 MCP 的连接或传输类失败连续达到阈值，就降整个 MCP。

3. 只统计连接或传输类失败  
   工具业务错误、参数错误、上游正常返回的业务失败，不参与自动降级计数。

4. 集群共享连续失败计数  
   自动降级不按单节点局部判断，而是按集群共享状态统一判断。

5. 成功即可清零  
   运行时工具调用成功、手动连接成功、刷新工具成功，都会清零当前代次的连续失败计数。

## 3. 数据模型变更

本次在 `mcp_server` 上新增了 3 个字段：

- `failure_source`
  - 失败来源
  - 当前取值：
    - `NONE`
    - `RUNTIME_AUTO_DEGRADE`

- `activation_status_changed_at`
  - 最近一次连接状态变更时间

- `runtime_fail_threshold`
  - 运行时自动降级失败阈值
  - 默认值为 `3`
  - 当值为 `0` 时，表示关闭自动降级

相关代码位置：

- [McpServer.java](/D:/projects/ai-apboa/apboa/common/src/main/java/com/hxh/apboa/common/entity/McpServer.java)
- [McpServerVO.java](/D:/projects/ai-apboa/apboa/common/src/main/java/com/hxh/apboa/common/vo/McpServerVO.java)
- [McpFailureSource.java](/D:/projects/ai-apboa/apboa/common/src/main/java/com/hxh/apboa/common/enums/McpFailureSource.java)

## 4. 运行时闭环

### 4.1 成功路径

当 Agent 运行时调用 MCP 工具成功时：

1. `LazyMcpAgentTool` 调用真实 MCP 工具
2. 调用成功后，上报一次运行时成功事件
3. 自动降级计数器清零

相关代码位置：

- [LazyMcpAgentTool.java](/D:/projects/ai-apboa/apboa/core/src/main/java/com/hxh/apboa/core/mcp/LazyMcpAgentTool.java)
- [McpRuntimeDegradeService.java](/D:/projects/ai-apboa/apboa/biz/mcp/src/main/java/com/hxh/apboa/mcp/service/McpRuntimeDegradeService.java)
- [McpRuntimeDegradeServiceImpl.java](/D:/projects/ai-apboa/apboa/biz/mcp/src/main/java/com/hxh/apboa/mcp/service/impl/McpRuntimeDegradeServiceImpl.java)

### 4.2 失败路径

当运行时调用抛出异常时：

1. `LazyMcpAgentTool` 先把异常交给失败分类器
2. 只有连接类、传输类异常才参与自动降级计数
3. 如果未达到阈值：
   - 本次工具调用返回错误结果
   - Agent 整体不崩
4. 如果达到阈值：
   - 尝试把 `mcp_server.activation_status` 从 `ACTIVE` 原子更新为 `FAILED`
   - `failure_source` 写为 `RUNTIME_AUTO_DEGRADE`
   - `activation_status_changed_at` 写入当前时间
   - `health_status` 写为 `UNHEALTHY`
   - `needs_sync` 写为 `true`
   - 发布相关 Agent 重注册消息

这里有两个保护：

1. 只有 `ACTIVE -> FAILED` 这次成功落库时，才广播重注册
2. 只有当前 `activation_revision` 和 `config_hash` 仍然匹配失败事件时，才允许写回，避免旧失败覆盖新配置或新代次

### 4.3 恢复路径

自动降级后，不允许运行时偷偷恢复。允许的恢复路径只有：

1. 用户手动点击 `连接`
2. 用户手动点击 `刷新工具`
3. 用户修改 MCP 配置后，保存时自动重连成功

恢复成功后：

- `activation_status` 回到可用态
- `failure_source` 重置为 `NONE`
- 连续失败计数清零
- Agent 会按最新状态重新装配工具

## 5. Redis 连续失败计数

本次连续失败计数使用 Redis 维护，目的是：

1. 在集群内共享状态
2. 避免把高频短期计数直接写数据库
3. 满足“中间任意成功即清零”的要求

计数粒度为当前 MCP 的当前运行代次，关键维度包括：

- `serverId`
- `activationRevision`
- `configHash`

这样可以避免：

- 旧版本配置下的失败，影响新配置
- 旧连接代次的失败，污染新连接代次

本次实现里，成功和失败都通过 Redis 脚本更新状态，再由服务端根据返回结果决定是否触发自动降级。

## 6. 管理端行为变化

### 6.1 MCP 配置

MCP 配置页新增字段：

- `自动降级失败次数`

交互规则：

- 默认值为 `3`
- 允许设置为 `0`
- `0` 表示关闭自动降级

相关代码位置：

- [McpForm.vue](/D:/projects/ai-apboa/apboa/ui/src/components/mcp/McpForm.vue)

### 6.2 状态展示

当 MCP 因运行时自动降级进入 `FAILED` 时，管理端会明确展示：

- 当前是 `连接失败`
- 失败来源为 `运行时自动降级`
- 最近连接状态变更时间

相关代码位置：

- [useMcpPresentation.ts](/D:/projects/ai-apboa/apboa/ui/src/composables/useMcpPresentation.ts)
- [McpCard.vue](/D:/projects/ai-apboa/apboa/ui/src/components/mcp/McpCard.vue)
- [index.vue](/D:/projects/ai-apboa/apboa/ui/src/views/Mcp/index.vue)

### 6.3 上次缓存与只读态

自动降级后，系统保留旧工具目录用于查看，但不允许继续修改工具治理。

也就是说：

1. 管理端仍然可以看到 `toolCount` 和工具列表
2. 页面会提示当前展示的是“上次缓存”
3. 在重新连接成功前，工具开关和治理写操作都进入只读态

后端也做了保护，避免只靠前端禁用按钮：

- 如果当前 MCP 处于 `FAILED + RUNTIME_AUTO_DEGRADE`
- 则拒绝修改工具治理，要求先重新连接成功

## 7. 运行时表现

自动降级前：

- Agent 仍然可以看到当前已经注册的 MCP 工具
- 单次失败只会变成工具级错误结果

自动降级成功后：

1. 当前库中 MCP 状态变为不可用
2. 相关 Agent 收到重注册消息
3. 后续 Agent 工具箱会把该 MCP 工具摘掉
4. 旧共享连接上下文会在后续运行时门控中被关闭

这意味着系统不再只是“这次调用失败”，而是会把运行时真相逐步收回到：

- 数据库状态
- Agent 工具注册结果
- 前端管理界面展示

## 8. SQL 变更

已运行项目使用增量脚本：

- [2026-05-18-01.sql](/D:/projects/ai-apboa/apboa/docs/2026-05-18-01.sql)

初始化脚本同步更新：

- [db_init.sql](/D:/projects/ai-apboa/apboa/docs/once_db_init/db_init.sql)

本次没有修改 `docs/schema.sql`。

## 9. 验证结果

本次改造完成后，已做以下验证：

1. 后端编译通过  
   `mvn -pl common,core,biz/mcp,biz/agent -am -DskipTests compile`

2. 前端类型检查通过  
   `ui` 目录下执行 `npm run type-check`

## 10. 相关文件

后端：

- [McpServerServiceImpl.java](/D:/projects/ai-apboa/apboa/biz/mcp/src/main/java/com/hxh/apboa/mcp/service/impl/McpServerServiceImpl.java)
- [McpRuntimeDegradeServiceImpl.java](/D:/projects/ai-apboa/apboa/biz/mcp/src/main/java/com/hxh/apboa/mcp/service/impl/McpRuntimeDegradeServiceImpl.java)
- [McpRuntimeFailureClassifier.java](/D:/projects/ai-apboa/apboa/biz/mcp/src/main/java/com/hxh/apboa/mcp/service/impl/McpRuntimeFailureClassifier.java)
- [LazyMcpAgentTool.java](/D:/projects/ai-apboa/apboa/core/src/main/java/com/hxh/apboa/core/mcp/LazyMcpAgentTool.java)
- [McpClientFactory.java](/D:/projects/ai-apboa/apboa/core/src/main/java/com/hxh/apboa/core/mcp/McpClientFactory.java)

前端：

- [McpForm.vue](/D:/projects/ai-apboa/apboa/ui/src/components/mcp/McpForm.vue)
- [McpCard.vue](/D:/projects/ai-apboa/apboa/ui/src/components/mcp/McpCard.vue)
- [index.vue](/D:/projects/ai-apboa/apboa/ui/src/views/Mcp/index.vue)
- [useMcpPresentation.ts](/D:/projects/ai-apboa/apboa/ui/src/composables/useMcpPresentation.ts)
