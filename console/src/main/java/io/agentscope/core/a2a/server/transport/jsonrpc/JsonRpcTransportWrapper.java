//package io.agentscope.core.a2a.server.transport.jsonrpc;
//
//import com.fasterxml.jackson.core.JsonParseException;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import io.a2a.server.ServerCallContext;
//import io.a2a.server.auth.User;
//import io.a2a.spec.CancelTaskRequest;
//import io.a2a.spec.DeleteTaskPushNotificationConfigRequest;
//import io.a2a.spec.GetTaskPushNotificationConfigRequest;
//import io.a2a.spec.GetTaskRequest;
//import io.a2a.spec.IdJsonMappingException;
//import io.a2a.spec.InternalError;
//import io.a2a.spec.InvalidParamsError;
//import io.a2a.spec.InvalidParamsJsonMappingException;
//import io.a2a.spec.InvalidRequestError;
//import io.a2a.spec.JSONParseError;
//import io.a2a.spec.JSONRPCError;
//import io.a2a.spec.JSONRPCErrorResponse;
//import io.a2a.spec.JSONRPCRequest;
//import io.a2a.spec.JSONRPCResponse;
//import io.a2a.spec.ListTaskPushNotificationConfigRequest;
//import io.a2a.spec.MethodNotFoundError;
//import io.a2a.spec.MethodNotFoundJsonMappingException;
//import io.a2a.spec.NonStreamingJSONRPCRequest;
//import io.a2a.spec.SendMessageRequest;
//import io.a2a.spec.SendStreamingMessageRequest;
//import io.a2a.spec.SetTaskPushNotificationConfigRequest;
//import io.a2a.spec.StreamingJSONRPCRequest;
//import io.a2a.spec.TaskResubscriptionRequest;
//import io.a2a.spec.TransportProtocol;
//import io.a2a.spec.UnsupportedOperationError;
//import io.a2a.transport.jsonrpc.handler.JSONRPCHandler;
//import io.a2a.util.Utils;
//import io.agentscope.core.a2a.server.transport.TransportWrapper;
//import java.time.Duration;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.concurrent.Flow;
//import org.reactivestreams.FlowAdapters;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import reactor.core.publisher.Flux;
//
//public class JsonRpcTransportWrapper implements TransportWrapper<String, Object> {
//    private static final Logger log = LoggerFactory.getLogger(JsonRpcTransportWrapper.class);
//    private final JSONRPCHandler jsonRpcHandler;
//
//    public JsonRpcTransportWrapper(JSONRPCHandler jsonrpcHandler) {
//        this.jsonRpcHandler = jsonrpcHandler;
//    }
//
//    public String getTransportType() {
//        return TransportProtocol.JSONRPC.asString();
//    }
//
//    public Object handleRequest(String body, Map<String, String> headers, Map<String, Object> metadata) {
//        ServerCallContext context = this.buildServerCallContext(headers, metadata);
//        boolean streaming = this.isStreamingRequest(body, context);
//        context.getState().put("isStream", streaming);
//
//        Object result;
//        try {
//            if (streaming) {
//                result = this.handleStreamRequest(body, context);
//                log.info("Handling streaming request, returning SSE Flux");
//            } else {
//                result = this.handleNonStreamRequest(body, context);
//                log.info("Handling non-streaming request, returning JSON response");
//            }
//        } catch (JsonProcessingException e) {
//            log.error("JSON parsing error: ", e);
//            result = this.handleError(e);
//        } catch (Throwable t) {
//            log.error("Handle JSON-RPC request error:", t);
//            result = new JSONRPCErrorResponse(new InternalError(t.getMessage()));
//        }
//
//        return result;
//    }
//
//    private ServerCallContext buildServerCallContext(Map<String, String> headers, Map<String, Object> metadata) {
//        Map<String, Object> state = new HashMap();
//        state.put("headers", headers);
//        return new ServerCallContext((User)null, state, new HashSet());
//    }
//
//    private boolean isStreamingRequest(String requestBody, ServerCallContext context) {
//        try {
//            JsonNode node = Utils.OBJECT_MAPPER.readTree(requestBody);
//            JsonNode method = node != null ? node.get("method") : null;
//            String methodName = method != null ? method.asText() : null;
//            if (methodName == null) {
//                return false;
//            } else {
//                context.getState().put("method", methodName);
//                return "message/stream".equals(methodName) || "tasks/resubscribe".equals(methodName);
//            }
//        } catch (Exception var6) {
//            return false;
//        }
//    }
//
//    private Flux<? extends JSONRPCResponse<?>> handleStreamRequest(String body, ServerCallContext context) throws JsonProcessingException {
//        StreamingJSONRPCRequest<?> request = (StreamingJSONRPCRequest)Utils.OBJECT_MAPPER.readValue(body, StreamingJSONRPCRequest.class);
//        Flow.Publisher<? extends JSONRPCResponse<?>> publisher;
//        if (request instanceof SendStreamingMessageRequest req) {
//            publisher = this.jsonRpcHandler.onMessageSendStream(req, context);
//        } else {
//            if (!(request instanceof TaskResubscriptionRequest)) {
//                return Flux.just(this.generateErrorResponse(request, new UnsupportedOperationError()));
//            }
//
//            TaskResubscriptionRequest req = (TaskResubscriptionRequest)request;
//            publisher = this.jsonRpcHandler.onResubscribeToTask(req, context);
//        }
//
//        return Flux.from(FlowAdapters.toPublisher(publisher)).delaySubscription(Duration.ofMillis(10L));
//    }
//
//    private JSONRPCResponse<?> handleNonStreamRequest(String body, ServerCallContext context) throws JsonProcessingException {
//        NonStreamingJSONRPCRequest<?> request = (NonStreamingJSONRPCRequest)Utils.OBJECT_MAPPER.readValue(body, NonStreamingJSONRPCRequest.class);
//        if (request instanceof GetTaskRequest req) {
//            return this.jsonRpcHandler.onGetTask(req, context);
//        } else if (request instanceof SendMessageRequest req) {
//            return this.jsonRpcHandler.onMessageSend(req, context);
//        } else if (request instanceof CancelTaskRequest req) {
//            return this.jsonRpcHandler.onCancelTask(req, context);
//        } else if (request instanceof GetTaskPushNotificationConfigRequest req) {
//            return this.jsonRpcHandler.getPushNotificationConfig(req, context);
//        } else if (request instanceof SetTaskPushNotificationConfigRequest req) {
//            return this.jsonRpcHandler.setPushNotificationConfig(req, context);
//        } else if (request instanceof ListTaskPushNotificationConfigRequest req) {
//            return this.jsonRpcHandler.listPushNotificationConfig(req, context);
//        } else if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
//            return this.jsonRpcHandler.deletePushNotificationConfig(req, context);
//        } else {
//            return this.generateErrorResponse(request, new UnsupportedOperationError());
//        }
//    }
//
//    private JSONRPCErrorResponse handleError(JsonProcessingException exception) {
//        Object id = null;
//        JSONRPCError jsonRpcError = null;
//        Object var7;
//        if (exception instanceof JsonParseException) {
//            var7 = new JSONParseError(exception.getMessage());
//        } else if (exception instanceof MethodNotFoundJsonMappingException) {
//            MethodNotFoundJsonMappingException err = (MethodNotFoundJsonMappingException)exception;
//            id = err.getId();
//            var7 = new MethodNotFoundError();
//        } else if (exception instanceof InvalidParamsJsonMappingException) {
//            InvalidParamsJsonMappingException err = (InvalidParamsJsonMappingException)exception;
//            id = err.getId();
//            var7 = new InvalidParamsError();
//        } else if (exception instanceof IdJsonMappingException) {
//            IdJsonMappingException err = (IdJsonMappingException)exception;
//            id = err.getId();
//            var7 = new InvalidRequestError();
//        } else {
//            var7 = new InvalidRequestError();
//        }
//
//        return new JSONRPCErrorResponse(id, (JSONRPCError)var7);
//    }
//
//    private JSONRPCErrorResponse generateErrorResponse(JSONRPCRequest<?> request, JSONRPCError error) {
//        return new JSONRPCErrorResponse(request.getId(), error);
//    }
//}
