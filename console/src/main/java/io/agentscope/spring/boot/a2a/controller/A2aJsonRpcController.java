//package io.agentscope.spring.boot.a2a.controller;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import io.a2a.spec.JSONRPCResponse;
//import io.a2a.spec.TransportProtocol;
//import io.a2a.util.Utils;
//import io.agentscope.core.a2a.server.AgentScopeA2aServer;
//import io.agentscope.core.a2a.server.transport.jsonrpc.JsonRpcTransportWrapper;
//import jakarta.servlet.http.HttpServletRequest;
//
//import java.util.Enumeration;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.logging.Logger;
//import org.springframework.http.MediaType;
//import org.springframework.http.codec.ServerSentEvent;
//import org.springframework.web.bind.annotation.*;
//import reactor.core.publisher.Flux;
//
//@RestController
//@RequestMapping("/")
//public class A2aJsonRpcController {
//
//    Logger logger = Logger.getLogger(A2aJsonRpcController.class.getName());
//
//    private final AgentScopeA2aServer agentScopeA2aServer;
//
//    private JsonRpcTransportWrapper jsonRpcHandler;
//
//    public A2aJsonRpcController(AgentScopeA2aServer agentScopeA2aServer) {
//        this.agentScopeA2aServer = agentScopeA2aServer;
//    }
//
//    @PostMapping(
//            value = "",
//            consumes = MediaType.APPLICATION_JSON_VALUE,
//            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
//    @ResponseBody
//    // TODO 将 @RequestBody String body 改为了 @RequestBody JsonNode body
//    public Object handleRequest(@RequestBody JsonNode body, HttpServletRequest httpRequest) {
//        Map<String, String> header = getHeaders(httpRequest);
//        Object result = getJsonRpcHandler().handleRequest(body.toString(), header, Map.of());
//        if (result instanceof Flux<?> fluxResult) {
//            return fluxResult
//                    .filter(each -> each instanceof JSONRPCResponse)
//                    .map(each -> (JSONRPCResponse<?>) each)
//                    .map(this::convertToSse);
//        } else {
//            return result;
//        }
//    }
//
//    private JsonRpcTransportWrapper getJsonRpcHandler() {
//        if (jsonRpcHandler == null) {
//            jsonRpcHandler =
//                    agentScopeA2aServer.getTransportWrapper(
//                            TransportProtocol.JSONRPC.asString(), JsonRpcTransportWrapper.class);
//        }
//        return jsonRpcHandler;
//    }
//
//    private Map<String, String> getHeaders(HttpServletRequest request) {
//        Map<String, String> headers = new HashMap<>();
//        Enumeration<String> headerNames = request.getHeaderNames();
//        while (headerNames.hasMoreElements()) {
//            String headerName = headerNames.nextElement();
//            String headerValue = request.getHeader(headerName);
//            headers.put(headerName, headerValue);
//        }
//        return headers;
//    }
//
//    private ServerSentEvent<String> convertToSse(JSONRPCResponse<?> response) {
//        try {
//            String data = Utils.OBJECT_MAPPER.writeValueAsString(response);
//            ServerSentEvent.Builder<String> builder =
//                    ServerSentEvent.<String>builder().data(data).event("jsonrpc");
//            if (response.getId() != null) {
//                builder.id(response.getId().toString());
//            }
//            return builder.build();
//        } catch (Exception e) {
//            logger.severe("Error converting response to SSE: " + e.getMessage());
//            return ServerSentEvent.<String>builder()
//                    .data("{\"error\":\"Internal conversion error\"}")
//                    .event("error")
//                    .build();
//        }
//    }
//}
