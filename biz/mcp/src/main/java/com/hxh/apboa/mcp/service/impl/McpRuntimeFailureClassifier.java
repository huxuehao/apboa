package com.hxh.apboa.mcp.service.impl;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import org.springframework.stereotype.Component;

/**
 * MCP 运行时失败分类器
 *
 * <p>首版采用保守白名单，仅把明确的连接/传输类异常计入自动降级。
 *
 * @author huxuehao
 */
@Component
public class McpRuntimeFailureClassifier {

    public boolean isTransportFailure(Throwable throwable) {
        Throwable current = unwrap(throwable);
        while (current != null) {
            if (matchesTransportFailure(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current != null
                && current.getCause() != null
                && current.getCause() != current
                && shouldUnwrap(current)) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }

    private boolean shouldUnwrap(Throwable throwable) {
        String name = throwable.getClass().getName();
        return name.startsWith("java.util.concurrent.CompletionException")
                || name.startsWith("java.util.concurrent.ExecutionException")
                || name.startsWith("reactor.core.Exceptions");
    }

    private boolean matchesTransportFailure(Throwable throwable) {
        if (throwable instanceof SocketTimeoutException
                || throwable instanceof TimeoutException
                || throwable instanceof ConnectException
                || throwable instanceof UnknownHostException
                || throwable instanceof EOFException
                || throwable instanceof SocketException
                || throwable instanceof IOException) {
            return true;
        }

        String className = throwable.getClass().getName().toLowerCase(Locale.ROOT);
        if (className.contains("timeout")
                || className.contains("prematureclose")
                || className.contains("connectionclosed")
                || className.contains("connectexception")) {
            return true;
        }

        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("transport closed")
                || normalized.contains("connection reset")
                || normalized.contains("connection refused")
                || normalized.contains("connection aborted")
                || normalized.contains("broken pipe")
                || normalized.contains("connection closed")
                || normalized.contains("connect timed out")
                || normalized.contains("read timed out")
                || normalized.contains("timeout")
                || normalized.contains("eof");
    }
}
