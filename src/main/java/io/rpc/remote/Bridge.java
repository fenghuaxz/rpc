package io.rpc.remote;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.rpc.Session;
import io.rpc.codec.ProtocolFactory;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public interface Bridge {

    static Session contextSession() {
        return DefaultHandler.CONTEXT_SESSION_HOLDER.get();
    }

    private static EventLoopGroup createEventLoopGroup() {
        return Epoll.isAvailable() ? new EpollEventLoopGroup() : new NioEventLoopGroup();
    }

    private static ChannelHandler createInjector(RequestExecutor provider) {
        return new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                Remote.attach(ctx.channel()).setProvider(provider);
                ctx.pipeline().remove(this);
                super.channelActive(ctx);
            }
        };
    }

    private static ChannelHandler configHandler(RequestExecutor provider, ProtocolFactory protocolFactory, EventExecutorGroup group, long readerIdleTime) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(protocolFactory.newEncoder(), protocolFactory.newDecoder());
                pipeline.addLast(new IdleStateHandler(readerIdleTime, 0, 0, TimeUnit.SECONDS));
                pipeline.addLast(createInjector(provider));
                pipeline.addLast(group, DefaultHandler.INSTANCE);
            }
        };
    }

    static void initBootstrap(ServerBootstrap bootstrap, EventExecutorGroup group, List<Object> objectList, ProtocolFactory protocolFactory, Executor executor) {
        objectList.add(0, new EmptyParameterFilter());
        objectList.add(1, new RemoteStubFactory());
        bootstrap.group(createEventLoopGroup(), createEventLoopGroup());
        bootstrap.channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class);
        bootstrap.childHandler(configHandler(new DefaultProvider(objectList, executor), protocolFactory, group, 15L));
    }

    static RemoteBridge initBootstrap(Bootstrap bootstrap, ProtocolFactory protocolFactory, Executor executor) {
        bootstrap.group(createEventLoopGroup());
        bootstrap.channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class);
        bootstrap.handler(configHandler(null, protocolFactory, null, 5L));
        return RemoteBridge.wrapRemoteBridge(bootstrap, executor);
    }
}
