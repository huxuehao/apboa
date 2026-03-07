//package io.a2a.client.transport.jsonrpc.sse;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import io.a2a.spec.JSONRPCError;
//import io.a2a.spec.StreamingEventKind;
//import io.a2a.spec.TaskStatusUpdateEvent;
//import io.a2a.util.Utils;
//import java.util.concurrent.Future;
//import java.util.function.Consumer;
//import java.util.logging.Logger;
//
//public class SSEEventListener {
//    private static final Logger log = Logger.getLogger(SSEEventListener.class.getName());
//    private final Consumer<StreamingEventKind> eventHandler;
//    private final Consumer<Throwable> errorHandler;
//    private volatile boolean completed = false;
//
//    public SSEEventListener(Consumer<StreamingEventKind> eventHandler, Consumer<Throwable> errorHandler) {
//        this.eventHandler = eventHandler;
//        this.errorHandler = errorHandler;
//    }
//
//    public void onMessage(String message, Future<Void> completableFuture) {
//        try {
//            this.handleMessage(Utils.OBJECT_MAPPER.readTree(message), completableFuture);
//        } catch (JsonProcessingException var4) {
//            log.warning("Failed to parse JSON message: " + message);
//        }
//
//    }
//
//    public void onError(Throwable throwable, Future<Void> future) {
//        if (this.errorHandler != null) {
//            this.errorHandler.accept(throwable);
//        }
//
//        future.cancel(true);
//    }
//
//    public void onComplete() {
//        if (this.completed) {
//            log.fine("SSEEventListener.onComplete() called again - ignoring (already completed)");
//        } else {
//            this.completed = true;
//            log.fine("SSEEventListener.onComplete() called - signaling successful stream completion");
//            if (this.errorHandler != null) {
//                log.fine("Calling errorHandler.accept(null) to signal successful completion");
//                this.errorHandler.accept(null);
//            } else {
//                log.warning("errorHandler is null, cannot signal completion");
//            }
//
//        }
//    }
//
//    private void handleMessage(JsonNode jsonNode, Future<Void> future) {
//        try {
//            if (jsonNode.has("error")) {
//                JSONRPCError error = (JSONRPCError)Utils.OBJECT_MAPPER.treeToValue(jsonNode.get("error"), JSONRPCError.class);
//                if (this.errorHandler != null) {
//                    this.errorHandler.accept(error);
//                }
//            } else {
//                // TODO 修改了对字符串的支持
//                if (jsonNode.isTextual()) {
//                    jsonNode = Utils.OBJECT_MAPPER.readTree(jsonNode.asText());
//                }
//                if (!jsonNode.has("result")) {
//                    throw new IllegalArgumentException("Unknown message type");
//                }
//
//                JsonNode result = jsonNode.path("result");
//                StreamingEventKind event = (StreamingEventKind)Utils.OBJECT_MAPPER.treeToValue(result, StreamingEventKind.class);
//                this.eventHandler.accept(event);
//                if (event instanceof TaskStatusUpdateEvent && ((TaskStatusUpdateEvent)event).isFinal()) {
//                    future.cancel(true);
//                }
//            }
//
//        } catch (JsonProcessingException e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
