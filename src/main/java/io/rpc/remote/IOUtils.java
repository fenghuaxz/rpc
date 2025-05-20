package io.rpc.remote;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.ProtocolDetectionResult;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import io.rpc.codec.ProtocolFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class IOUtils {

    static EventLoopGroup createEventLoopGroup(int nThreads) {
        return Epoll.isAvailable() ? new EpollEventLoopGroup(nThreads) : new NioEventLoopGroup(nThreads);
    }

    static ChannelHandler createInjector(RequestExecutor provider) {
        return new ChannelInboundHandlerAdapter() {
            @Override
            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                Remote.attach(ctx.channel()).setProvider(provider);
                ctx.pipeline().remove(this);
                super.channelActive(ctx);
            }
        };
    }

    static ChannelHandler createProxySniffer() {
        return new ByteToMessageDecoder() {
            @Override
            protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
                ProtocolDetectionResult<HAProxyProtocolVersion> result = HAProxyMessageDecoder.detectProtocol(in);
                ChannelPipeline pipeline = ctx.pipeline();

                switch (result.state()) {
                    case DETECTED:
                        pipeline.addAfter(ctx.name(), "HAPROXY_DECODER_NAME", new HAProxyMessageDecoder());
                        pipeline.addAfter("HAPROXY_DECODER_NAME", "HAPROXY_INFO_HANDLER_NAME", new SimpleChannelInboundHandler<HAProxyMessage>() {
                            @Override
                            protected void channelRead0(ChannelHandlerContext ctx, HAProxyMessage message) {
                                ((Remote) ctx.channel()).setHaproxyAddress(new InetSocketAddress(message.sourceAddress(), message.sourcePort()));
                                ctx.pipeline().remove(this);
                            }
                        });
                        ctx.pipeline().remove(this);
                        break;

                    case NEEDS_MORE_DATA:
                        return;

                    case INVALID:
                        ctx.pipeline().remove(this);
                        break;

                    default:
                        break;
                }
            }
        };
    }

    public static <I> RemoteBridge initBootstrap(Bootstrap bootstrap, Map<String, String> headers, ProtocolFactory<I> protocolFactory, Executor executor) {
        bootstrap.group(createEventLoopGroup(0));
        bootstrap.channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class);
        bootstrap.handler(configHandler(null, protocolFactory, null, 0L, 5L));
        return RemoteBridge.wrapBridge(bootstrap, headers, executor);
    }

    public static <I> void initBootstrap(ServerBootstrap bootstrap, EventExecutorGroup group, List<Object> objectList, ProtocolFactory<I> protocolFactory, Executor executor) {
        objectList.add(0, new ParameterFilter());
        objectList.add(1, new StubFactory());
        bootstrap.group(createEventLoopGroup(1), createEventLoopGroup(0));
        bootstrap.channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class);
        bootstrap.childHandler(configHandler(new DefaultProvider(objectList, executor), protocolFactory, group, 15L, 0L));
    }

    private static <I> ChannelHandler configHandler(RequestExecutor provider, ProtocolFactory<I> protocolFactory
            , EventExecutorGroup group, long readerIdleTime, long writerIdleTime) {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(createInjector(provider));

                //仅为服务端嵌入HAProxy嗅探处理器
                if (provider != null) {
                    pipeline.addLast(createProxySniffer());
                }
                pipeline.addLast(new IdleStateHandler(readerIdleTime, writerIdleTime, 0L, TimeUnit.SECONDS));
                pipeline.addLast(group, protocolFactory.newEncoder(), protocolFactory.newDecoder());
                pipeline.addLast(group, DefaultHandler.INSTANCE);
            }
        };
    }
}
