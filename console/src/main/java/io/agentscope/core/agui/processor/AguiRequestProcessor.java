
package io.agentscope.core.agui.processor;

import com.hxh.apboa.common.consts.TableConst;
import com.hxh.apboa.common.entity.AgentDefinition;
import com.hxh.apboa.common.entity.ChatSession;
import com.hxh.apboa.common.util.JsonUtils;
import com.hxh.apboa.common.vo.AccountVO;
import com.hxh.apboa.core.agui.AgentContext;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.adapter.AguiAgentAdapter;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.AguiMessage;
import io.agentscope.core.agui.model.RunAgentInput;

import java.util.List;
import java.util.Objects;

import io.agentscope.core.session.InMemorySession;
import io.agentscope.core.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import reactor.core.publisher.Flux;

public class AguiRequestProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AguiRequestProcessor.class);

    private final AgentResolver agentResolver;
    private final AguiAdapterConfig config;
    private final Session session;
    private final JdbcTemplate jdbcTemplate;

    private AguiRequestProcessor(Builder builder) {
        this.agentResolver =
                Objects.requireNonNull(builder.agentResolver, "agentResolver cannot be null");
        this.config = builder.config != null ? builder.config : AguiAdapterConfig.defaultConfig();
        this.session = builder.session != null ? builder.session : new InMemorySession();
        this.jdbcTemplate = builder.jdbcTemplate;
    }

    /**
     * Result of processing an AG-UI request.
     *
     * <p>Contains the resolved agent (for interrupt handling) and the event stream.
     *
     * @param agent The resolved agent instance
     * @param events The event stream
     */
    public record ProcessResult(Agent agent, Flux<AguiEvent> events) {}

    /**
     * Process an AG-UI request and return the result containing agent and event stream.
     *
     * @param input The run agent input
     * @param headerAgentId The agent ID from HTTP header (may be null)
     * @param pathAgentId The agent ID from URL path variable (may be null)
     * @return A ProcessResult containing the agent and event stream
     */
    public ProcessResult process(RunAgentInput input, String headerAgentId, String pathAgentId) {
        String threadId = input.getThreadId();

        // Resolve agent ID
        String agentId = resolveAgentId(input, headerAgentId, pathAgentId);

        // Resolve agent
        Agent agent = agentResolver.resolveAgent(agentId, threadId);

        // 获取是否开启记忆
        boolean memoryActive = input.getForwardedProp("memoryActive") != null
                ? (Boolean) input.getForwardedProp("memoryActive")
                : false;

        // Determine effective input based on server-side memory
        RunAgentInput effectiveInput = input;
        if (agentResolver.hasMemory(threadId)) {
            logger.debug(
                    "Using server-side memory for thread {}, extracting latest user message",
                    threadId);
            effectiveInput = extractLatestUserMessage(input);
        }

        // 加载历史记忆
        if (agent instanceof ReActAgent reActAgent && memoryActive) {
            try {
                AgentDefinition agentDefinition = getAgentDefinition(threadId, jdbcTemplate);
                if (agentDefinition != null && agentDefinition.getEnableMemory()) {
                    // 从session中加载历史会话
                    reActAgent.loadFrom(session, threadId);
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        // Create adapter and run
        AguiAgentAdapter adapter = new AguiAgentAdapter(agent, config);
        // 执行完成后保存session
        Flux<AguiEvent> events = adapter.run(effectiveInput)
                .doFinally(signalType -> {
                    if (agent instanceof ReActAgent reActAgent && memoryActive) {
                        try {
                            reActAgent.saveTo(session, threadId);
                        } catch (Exception ex) {
                            logger.error(ex.getMessage(), ex);
                        }
                    }
                });

        return new ProcessResult(agent, events);
    }

    /**
     * 通过 sessionId 获取 AgentDefinition
     * @param sessionId threadId
     * @param jdbcTemplate jdbcTemplate
     */
    private AgentDefinition getAgentDefinition(String sessionId, JdbcTemplate jdbcTemplate) {
        if (sessionId == null || sessionId.isEmpty()) {
            return null;
        }

        String chat_session_sql = String.format("SELECT * FROM %s WHERE id = %s", TableConst.CHAT_SESSION, sessionId);
        List<ChatSession> chatSessions = jdbcTemplate.query(chat_session_sql, (rs, rowNum) -> {
            ChatSession chatSession = new ChatSession();
            // 手动映射字段
            chatSession.setId(rs.getLong("id"));
            chatSession.setAgentId(rs.getLong("agent_id"));
            return chatSession;
        });

        if (chatSessions.isEmpty()) {
            return null;
        }

        String agent_definition_sql = String.format("SELECT * FROM %s WHERE id = %s", TableConst.AGENT, chatSessions.getFirst().getAgentId());
        List<AgentDefinition> AgentDefinitions = jdbcTemplate.query(agent_definition_sql, (rs, rowNum) -> {
            AgentDefinition agentDefinition = new AgentDefinition();
            // 手动映射字段
            agentDefinition.setId(rs.getLong("id"));
            agentDefinition.setEnableMemory(rs.getBoolean("enable_memory"));
            return agentDefinition;
        });

        if (AgentDefinitions.isEmpty()) {
            return null;
        }

        return AgentDefinitions.getFirst();
    }

    /**
     * Resolve the agent ID from multiple sources.
     *
     * <p>The agent ID is resolved in the following priority order:
     * <ol>
     *   <li>URL path variable (if provided)</li>
     *   <li>HTTP header (if provided)</li>
     *   <li>forwardedProps.agentId in request body</li>
     *   <li>config.defaultAgentId</li>
     *   <li>"default"</li>
     * </ol>
     *
     * @param input The request input
     * @param headerAgentId The agent ID from HTTP header (may be null)
     * @param pathAgentId The agent ID from URL path variable (may be null)
     * @return The resolved agent ID
     */
    public String resolveAgentId(RunAgentInput input, String headerAgentId, String pathAgentId) {
        // 1. URL path variable has highest priority
        if (pathAgentId != null && !pathAgentId.isEmpty()) {
            logger.debug("Using agent ID from path variable: {}", pathAgentId);
            return pathAgentId;
        }

        // 2. Check HTTP header
        if (headerAgentId != null && !headerAgentId.isEmpty()) {
            logger.debug("Using agent ID from header: {}", headerAgentId);
            return headerAgentId;
        }

        // 3. Check forwardedProps for agentId
        Object agentIdProp = input.getForwardedProp("agentId");
        if (agentIdProp != null) {
            String propsAgentId = agentIdProp.toString();
            logger.debug("Using agent ID from forwardedProps: {}", propsAgentId);
            return propsAgentId;
        }

        // 4. Use config default
        if (config.getDefaultAgentId() != null) {
            logger.debug("Using default agent ID from config: {}", config.getDefaultAgentId());
            return config.getDefaultAgentId();
        }

        // 5. Fall back to "default"
        logger.debug("Using fallback agent ID: default");
        return "default";
    }

    /**
     * Extract only the latest user message from the input.
     *
     * <p>This is used when server-side memory is enabled and the agent already
     * has conversation history. Only the latest user message needs to be passed.
     *
     * @param input The original input
     * @return A new input with only the latest user message
     */
    public RunAgentInput extractLatestUserMessage(RunAgentInput input) {
        List<AguiMessage> messages = input.getMessages();
        if (messages == null || messages.isEmpty()) {
            return input;
        }

        // Find the last user message
        AguiMessage lastUserMessage = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            AguiMessage msg = messages.get(i);
            if ("user".equalsIgnoreCase(msg.getRole())) {
                lastUserMessage = msg;
                break;
            }
        }

        if (lastUserMessage == null) {
            return input;
        }

        // Create new input with only the last user message
        return RunAgentInput.builder()
                .threadId(input.getThreadId())
                .runId(input.getRunId())
                .messages(List.of(lastUserMessage))
                .tools(input.getTools())
                .context(input.getContext())
                .forwardedProps(input.getForwardedProps())
                .build();
    }

    /**
     * Creates a new builder for AguiRequestProcessor.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for AguiRequestProcessor. */
    public static class Builder {

        private AgentResolver agentResolver;
        private AguiAdapterConfig config;
        private Session session;
        private JdbcTemplate jdbcTemplate;

        /**
         * Set the agent resolver.
         *
         * @param agentResolver The agent resolver
         * @return This builder
         */
        public Builder agentResolver(AgentResolver agentResolver) {
            this.agentResolver = agentResolver;
            return this;
        }

        /**
         * Set the adapter configuration.
         *
         * @param config The adapter configuration
         * @return This builder
         */
        public Builder config(AguiAdapterConfig config) {
            this.config = config;
            return this;
        }

        /**
         * Set the session storage.
         *
         * @param session The session storage (InMemorySession or MysqlSession)
         * @return This builder
         */
        public Builder session(Session session) {
            this.session = session;
            return this;
        }

        /**
         * Set the jdbcTemplate storage.
         *
         * @param jdbcTemplate jdbcTemplate
         * @return This builder
         */
        public Builder jdbcTemplate(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
            return this;
        }

        /**
         * Build the processor.
         *
         * @return The built processor
         * @throws NullPointerException if agentResolver is not set
         */
        public AguiRequestProcessor build() {
            return new AguiRequestProcessor(this);
        }
    }
}
