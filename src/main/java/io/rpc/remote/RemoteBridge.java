package io.rpc.remote;

import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.util.Map;
import java.util.concurrent.Executor;

public interface RemoteBridge {

    void close();

    void setOnCloseListener(Runnable runnable);

    <T> T getMapper(Class<T> clazz);

    static RemoteBridge wrapBridge(Bootstrap bootstrap, Map<String, String> headers, Executor executor) {
        return new DefaultRemoteBridge(bootstrap, headers, executor);
    }
}
