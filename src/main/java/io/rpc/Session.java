package io.rpc;

import io.netty.channel.ChannelFuture;
import io.rpc.remote.Bridge;

import java.net.SocketAddress;

public interface Session {

    default String sessionId() {
        throw new UnsupportedOperationException("Stub!");
    }

    default SocketAddress localAddress() {
        throw new UnsupportedOperationException("Stub!");
    }

    default SocketAddress remoteAddress() {
        throw new UnsupportedOperationException("Stub!");
    }

    default ChannelFuture closeFuture() {
        throw new UnsupportedOperationException("Stub!");
    }

    static Session contextSession() {
        return Bridge.contextSession();
    }
}