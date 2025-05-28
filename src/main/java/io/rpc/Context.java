package io.rpc;

import io.netty.channel.ChannelFuture;
import io.rpc.remote.Bridge;

import java.net.SocketAddress;
import java.util.Map;

public interface Context {

    default String contextId() {
        throw new UnsupportedOperationException("Stub!");
    }

    default SocketAddress localAddress() {
        throw new UnsupportedOperationException("Stub!");
    }

    default SocketAddress remoteAddress() {
        throw new UnsupportedOperationException("Stub!");
    }

    default SocketAddress proxyAddress() {
        throw new UnsupportedOperationException("Stub!");
    }

    default ChannelFuture closeFuture() {
        throw new UnsupportedOperationException("Stub!");
    }

    default ChannelFuture close() {
        throw new UnsupportedOperationException("Stub!");
    }

    static Context context() {
        return Bridge.context();
    }

    static Map<String, String> headers() {
        return Bridge.contextHeaders();
    }

}