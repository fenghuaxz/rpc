package io.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
import io.rpc.codec.ProtocolDecoder;
import io.rpc.codec.ProtocolEncoder;
import io.rpc.codec.ProtocolFactory;
import io.rpc.remote.IOUtils;
import io.rpc.remote.RemoteBridge;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class ObjectClient {

    private final RemoteBridge bridge;

    private ObjectClient(Builder builder) {
        this.bridge = IOUtils.initBootstrap(builder.bootstrap, builder.headers, builder.protocolFactory, builder.executor);
    }

    public <T> T getMapper(Class<T> clazz) {
        return bridge.getMapper(clazz);
    }

    public void close() {
        bridge.close();
    }

    public void setOnCloseListener(Runnable runnable) {
        this.bridge.setOnCloseListener(runnable);
    }

    public static class Builder {

        private Executor executor = Runnable::run;
        private final Bootstrap bootstrap = new Bootstrap();
        private Map<String, String> headers;
        private ProtocolFactory<?> protocolFactory = ProtocolFactory.newFactory(ProtocolEncoder.class, ProtocolDecoder.class);

        public Builder() {
            option(ChannelOption.TCP_NODELAY, true);
            option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
            option(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT);
        }

        public Builder remoteAddress(String inetHost, int inetPort) {
            bootstrap.remoteAddress(inetHost, inetPort);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
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

        public <I, E extends MessageToByteEncoder<I>, D extends ByteToMessageDecoder> Builder codec(Class<E> encoder, Class<D> decoder) {
            this.protocolFactory = ProtocolFactory.newFactory(encoder, decoder);
            return this;
        }

        public ObjectClient build() {
            return new ObjectClient(this);
        }
    }
}
