package io.agentscope.spring.boot.agui.webflux;

import com.hxh.apboa.core.agui.AgentContext;
import io.agentscope.core.agui.AguiException;
import io.agentscope.core.agui.adapter.AguiAdapterConfig;
import io.agentscope.core.agui.encoder.AguiEventEncoder;
import io.agentscope.core.agui.event.AguiEvent;
import io.agentscope.core.agui.model.RunAgentInput;
import io.agentscope.core.agui.processor.AguiRequestProcessor;
import io.agentscope.core.agui.registry.AguiAgentRegistry;
import io.agentscope.core.session.Session;
import io.agentscope.spring.boot.agui.common.DefaultAgentResolver;
import io.agentscope.spring.boot.agui.common.ThreadSessionManager;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AguiWebFluxHandler {

    private static final Logger logger = LoggerFactory.getLogger(AguiWebFluxHandler.class);

    private static final String DEFAULT_AGENT_ID_HEADER = "X-Agent-Id";
    private static final String AGENT_ID_PATH_VARIABLE = "agentId";

    private final AguiRequestProcessor processor;
    private final AguiEventEncoder encoder;
    private final String agentIdHeader;

    private AguiWebFluxHandler(Builder builder) {
        Session session = builder.session;
        JdbcTemplate jdbcTemplate = builder.jdbcTemplate;
        this.processor =
                AguiRequestProcessor.builder()
                        .agentResolver(
                                DefaultAgentResolver.builder()
                                        .registry(builder.registry)
                                        .sessionManager(builder.sessionManager)
                                        .serverSideMemory(builder.serverSideMemory)
                                        .build())
                        .config(
                                builder.config != null
                                        ? builder.config
                                        : AguiAdapterConfig.defaultConfig())
                        .session(session)
                        .jdbcTemplate(jdbcTemplate)
                        .build();
        this.encoder = new AguiEventEncoder();
        this.agentIdHeader =
                builder.agentIdHeader != null ? builder.agentIdHeader : DEFAULT_AGENT_ID_HEADER;
    }

    /**
     * Handle an AG-UI run request.
     *
     * <p>This method parses the request body as {@link RunAgentInput}, resolves the
     * agent from the registry, and returns an SSE stream of AG-UI events.
     *
     * @param request The server request
     * @return A Mono containing the server response with SSE stream
     */
    public Mono<ServerResponse> handle(ServerRequest request) {
        return request.bodyToMono(RunAgentInput.class)
                .flatMap(input -> processInput(input, request, null))
                .onErrorResume(this::handleParseError);
    }

    /**
     * Handle an AG-UI run request with agent ID in the URL path.
     *
     * <p>This method handles requests to {@code /agui/run/{agentId}}.
     * The path variable takes highest priority for agent resolution.
     *
     * @param request The server request containing the agentId path variable
     * @return A Mono containing the server response with SSE stream
     */
    public Mono<ServerResponse> handleWithAgentId(ServerRequest request) {
        String pathAgentId = request.pathVariable(AGENT_ID_PATH_VARIABLE);
        return request.bodyToMono(RunAgentInput.class)
                .flatMap(input -> processInput(input, request, pathAgentId))
                .onErrorResume(this::handleParseError);
    }

    private Mono<ServerResponse> processInput(
            RunAgentInput input, ServerRequest request, String pathAgentId) {
        String threadId = input.getThreadId();
        String runId = input.getRunId();

        try {
            // Get header agent ID
            String headerAgentId = request.headers().firstHeader(agentIdHeader);

            // Process request - returns both agent and event stream
            AguiRequestProcessor.ProcessResult result =
                    processor.process(input, headerAgentId, pathAgentId);

            // Create SSE stream using ServerSentEvent for proper streaming behavior
            Flux<ServerSentEvent<String>> sseStream =
                    result.events()
                            .map(
                                    event ->
                                            ServerSentEvent.<String>builder()
                                                    .data(encoder.encodeToJson(event).trim())
                                                    .build())
                            // When client closes connection (cancels stream), interrupt the agent
                            .doOnCancel(
                                    () -> {
                                        logger.info(
                                                "SSE stream cancelled for run {}, interrupting"
                                                        + " agent",
                                                runId);
                                        AgentContext.clean();
                                        result.agent().interrupt();
                                    });

            return ServerResponse.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(sseStream, ServerSentEvent.class);

        } catch (AguiException.AgentNotFoundException e) {
            logger.error("Agent not found: {}", e.getMessage());
            AgentContext.clean();
            return createErrorResponse(threadId, runId, e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing AG-UI request: {}", e.getMessage());
            AgentContext.clean();
            return createErrorResponse(threadId, runId, e.getMessage());
        }
    }

    private Mono<ServerResponse> handleParseError(Throwable error) {
        logger.error("Error parsing AG-UI request: {}", error.getMessage());
        return ServerResponse.badRequest()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(
                        createErrorEventStream(
                                "unknown",
                                "unknown",
                                "Failed to parse request: " + error.getMessage()),
                        ServerSentEvent.class);
    }

    private Mono<ServerResponse> createErrorResponse(
            String threadId, String runId, String errorMessage) {
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(createErrorEventStream(threadId, runId, errorMessage), ServerSentEvent.class);
    }

    /**
     * Create an SSE stream containing error and finish events.
     *
     * @param threadId The thread ID
     * @param runId The run ID
     * @param errorMessage The error message
     * @return A Flux of ServerSentEvents
     */
    private Flux<ServerSentEvent<String>> createErrorEventStream(
            String threadId, String runId, String errorMessage) {
        String errorEvent =
                encoder.encodeToJson(
                                new AguiEvent.Raw(threadId, runId, Map.of("error", errorMessage)))
                        .trim();
        String finishEvent =
                encoder.encodeToJson(new AguiEvent.RunFinished(threadId, runId)).trim();
        return Flux.just(
                ServerSentEvent.<String>builder().data(errorEvent).build(),
                ServerSentEvent.<String>builder().data(finishEvent).build());
    }

    /**
     * Creates a new builder for AguiWebFluxHandler.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for AguiWebFluxHandler. */
    public static class Builder {

        private AguiAgentRegistry registry;
        private ThreadSessionManager sessionManager;
        private AguiAdapterConfig config;
        private boolean serverSideMemory = false;
        private String agentIdHeader;
        private Session session;
        private JdbcTemplate jdbcTemplate;

        /**
         * Set the agent registry.
         *
         * @param registry The agent registry
         * @return This builder
         */
        public Builder agentRegistry(AguiAgentRegistry registry) {
            this.registry = registry;
            return this;
        }

        /**
         * Set the thread session manager for server-side memory support.
         *
         * @param sessionManager The session manager
         * @return This builder
         */
        public Builder sessionManager(ThreadSessionManager sessionManager) {
            this.sessionManager = sessionManager;
            return this;
        }

        /**
         * Enable or disable server-side memory management.
         *
         * @param enabled Whether to enable server-side memory
         * @return This builder
         */
        public Builder serverSideMemory(boolean enabled) {
            this.serverSideMemory = enabled;
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
         * Set the HTTP header name to read agent ID from.
         *
         * @param agentIdHeader The header name (default: X-Agent-Id)
         * @return This builder
         */
        public Builder agentIdHeader(String agentIdHeader) {
            this.agentIdHeader = agentIdHeader;
            return this;
        }

        /**
         * Build the handler.
         *
         * @return The built handler
         * @throws IllegalStateException if registry is not set
         */
        public AguiWebFluxHandler build() {
            if (registry == null) {
                throw new IllegalStateException("Agent registry must be set");
            }
            return new AguiWebFluxHandler(this);
        }
    }
}
