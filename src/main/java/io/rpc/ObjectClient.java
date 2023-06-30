package io.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.util.concurrent.Promise;
import io.rpc.codec.ProtocolDecoder;
import io.rpc.codec.ProtocolEncoder;
import io.rpc.codec.ProtocolFactory;
import io.rpc.remote.Bridge;
import io.rpc.remote.RemoteBridge;

import java.util.concurrent.Executor;

public class ObjectClient {

    private final RemoteBridge bridge;
    private final Bootstrap bootstrap;

    private ObjectClient(Builder builder) {
        this.bridge = Bridge.initBootstrap(builder.bootstrap, builder.protocolFactory, builder.executor);
        this.bootstrap = builder.bootstrap;
    }

    public <T> T getMapper(Class<T> clazz) {
        return bridge.getMapper(clazz);
    }

    public void close() {
        bootstrap.config().group().shutdownGracefully();
    }

    public Promise<Void> closeFuture() {
        return bridge.closeFuture();
    }

    public static class Builder {

        private Executor executor = Runnable::run;
        private final Bootstrap bootstrap = new Bootstrap();
        private ProtocolFactory protocolFactory = ProtocolFactory.newFactory(ProtocolEncoder.class, ProtocolDecoder.class);

        public Builder() {
            option(ChannelOption.TCP_NODELAY, true);
            option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
            option(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT);
        }

        public Builder remoteAddress(String inetHost, int inetPort) {
            bootstrap.remoteAddress(inetHost, inetPort);
            return this;
        }

        public <T> Builder option(ChannelOption<T> option, T value) {
            bootstrap.option(option, value);
            return this;
        }

        public Builder callbackExecutor(Executor executor) {
            if (executor == null) {
                throw new NullPointerException("executor == null");
            }
            this.executor = executor;
            return this;
        }

        public <E extends ProtocolEncoder, D extends ProtocolDecoder> Builder codec(Class<E> encoder, Class<D> decoder) {
            this.protocolFactory = ProtocolFactory.newFactory(encoder, decoder);
            return this;
        }

        public ObjectClient build() {
            return new ObjectClient(this);
        }
    }
}
