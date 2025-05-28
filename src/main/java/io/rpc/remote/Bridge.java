package io.rpc.remote;

import io.rpc.Context;

import java.util.HashMap;
import java.util.Map;

public interface Bridge {

    static Context context() {
        return DefaultHandler.CONTEXT_HOLDER.get();
    }

    static Map<String, String> contextHeaders() {
        Map<String, String> headers = DefaultHandler.CONTEXT_HEADERS_HOLDER.getIfExists();
        if (headers == null) {
            return new HashMap<>();
        }
        return headers;
    }
}