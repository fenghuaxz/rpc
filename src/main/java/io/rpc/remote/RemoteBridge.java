package io.rpc.remote;

import io.netty.bootstrap.Bootstrap;
import io.netty.util.concurrent.Promise;

import java.util.concurrent.Executor;

public interface RemoteBridge {

    Promise<Void> closeFuture();

    <T> T getMapper(Class<T> clazz);

    static RemoteBridge wrapRemoteBridge(Bootstrap bootstrap, Executor executor) {
        return new DefaultRemoteBridge(bootstrap, executor);
    }
}
