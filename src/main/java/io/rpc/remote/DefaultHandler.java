package io.rpc.remote;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.FastThreadLocal;
import io.rpc.Context;
import io.rpc.beans.Ping;
import io.rpc.beans.Request;
import io.rpc.beans.Response;

import java.util.Map;

@ChannelHandler.Sharable
final class DefaultHandler extends ChannelInboundHandlerAdapter {

    static final DefaultHandler INSTANCE = new DefaultHandler();
    static final FastThreadLocal<Context> CONTEXT_HOLDER = new FastThreadLocal<>();
    static final FastThreadLocal<Map<String, String>> CONTEXT_HEADERS_HOLDER = new FastThreadLocal<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Remote remote = (Remote) ctx.channel();
        CONTEXT_HOLDER.set(remote);

        if (msg instanceof Request) {
            Request request = (Request) msg;
            CONTEXT_HEADERS_HOLDER.set(request.headers);
            remote.provider().execute(remote, request);
        } else if (msg instanceof Response) {
            Response response = (Response) msg;
            Remote.PendingTasks.processResult(response.id, remote, response.cause, response.result);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.WRITER_IDLE) ctx.writeAndFlush(Ping.PING);
            else if (state == IdleState.READER_IDLE) {
                ctx.close();
            }
        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
        Thread.UncaughtExceptionHandler ueh = Thread.getDefaultUncaughtExceptionHandler();
        if (ueh != null) {
            ueh.uncaughtException(Thread.currentThread(), cause);
            return;
        }
        super.exceptionCaught(ctx, cause);
    }
}