package io.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.bootstrap.ServerBootstrapConfig;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.util.concurrent.EventExecutorGroup;
import io.rpc.codec.ProtocolDecoder;
import io.rpc.codec.ProtocolEncoder;
import io.rpc.codec.ProtocolFactory;
import io.rpc.remote.Bridge;

import java.io.IOException;
import java.net.BindException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;

public class ObjectServer {

    private final ServerBootstrap bootstrap;
    private final EventExecutorGroup businessGroup;

    private ObjectServer(Builder builder) {
        this.bootstrap = builder.bootstrap;
        this.businessGroup = builder.businessGroup;
        Bridge.initBootstrap(builder.bootstrap, builder.businessGroup, builder.objectList, builder.protocolFactory, builder.executor);
    }

    public void open() throws IOException {
        IOException[] exceptions = {null};
        Thread thread = Thread.currentThread();
        bootstrap.bind().addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                exceptions[0] = (BindException) f.cause();
            }
            LockSupport.unpark(thread);
        });

        LockSupport.park();
        IOException cause;
        if ((cause = exceptions[0]) != null) {
            throw cause;
        }
    }

    public void close() {
        ServerBootstrapConfig config = bootstrap.config();
        config.group().shutdownGracefully();
        config.childGroup().shutdownGracefully();
        if (businessGroup != null) {
            businessGroup.shutdownGracefully();
        }
    }

    public static class Builder {

        private Executor executor = Runnable::run;
        private final ServerBootstrap bootstrap = new ServerBootstrap();
        private EventExecutorGroup businessGroup;
        private final List<Object> objectList = new CopyOnWriteArrayList<>();
        private ProtocolFactory protocolFactory = ProtocolFactory.newFactory(ProtocolEncoder.class, ProtocolDecoder.class);

        public Builder() {
            option(ChannelOption.SO_BACKLOG, 1024);
            option(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
            childOption(ChannelOption.TCP_NODELAY, true);
            childOption(ChannelOption.ALLOCATOR, ByteBufAllocator.DEFAULT);
            childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT);
        }

        public Builder port(int inetPort) {
            bootstrap.localAddress(inetPort);
            return this;
        }

        public Builder executor(Executor executor) {
            if (executor == null) {
                throw new NullPointerException("executor == null");
            }
            this.executor = executor;
            return this;
        }

        public <T> Builder option(ChannelOption<T> option, T value) {
            bootstrap.option(option, value);
            return this;
        }

        public <T> Builder childOption(ChannelOption<T> option, T value) {
            bootstrap.childOption(option, value);
            return this;
        }

        public Builder threads(EventExecutorGroup group) {
            this.businessGroup = group;
            return this;
        }

        public Builder impl(Object... impls) {
            objectList.addAll(Arrays.asList(impls));
            return this;
        }

        public <E extends ProtocolEncoder, D extends ProtocolDecoder> Builder codec(Class<E> encoder, Class<D> decoder) {
            this.protocolFactory = ProtocolFactory.newFactory(encoder, decoder);
            return this;
        }

        public ObjectServer build() {
            return new ObjectServer(this);
        }
    }
}
