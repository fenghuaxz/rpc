package io.rpc.remote;

import io.rpc.Session;

import java.util.HashMap;
import java.util.Map;

public interface Bridge {

    static Session session() {
        return DefaultHandler.CONTEXT_SESSION_HOLDER.get();
    }

    static Map<String, String> headers() {
        Map<String, String> headers = DefaultHandler.CONTEXT_HEADERS_HOLDER.getIfExists();
        if (headers == null) {
            return new HashMap<>();
        }
        return headers;
    }
}