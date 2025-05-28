package io.rpc.remote;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.rpc.Call;
import io.rpc.TimeoutException;
import io.rpc.annotations.Timeout;
import io.rpc.beans.Request;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;

final class DefaultContext implements Remote, Channel {

    private final Channel channel;
    private volatile RequestExecutor provider;

    public DefaultContext(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void setProvider(RequestExecutor provider) {
        if (provider != null) {
            this.provider = provider;
        }
    }

    private volatile SocketAddress haproxyAddress;

    @Override
    public void setHaproxyAddress(SocketAddress address) {
        this.haproxyAddress = address;
    }

    @Override
    public SocketAddress proxyAddress() {
        return this.haproxyAddress;
    }

    @Override
    public RequestExecutor provider() {
        if (provider == null) {
            provider = new DefaultProvider();
        }
        return this.provider;
    }

    public <V> Call<V> execute(String objectName, Map<String, String> headers, Method method, Object[] args, Executor executor) {
        DefaultCaller<V> call = new DefaultCaller<>(method, executor);

        if (!isWritable()) {
            call.tryFailure(new IOException("!isWritable"));
            return call;
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (parameterType.isInterface()) {
                String id = Remote.parameterObjectName(method, i);
                provider().put(id, args[i], executor);
                args[i] = "@interface";
            }
        }

        Request request = new Request(objectName, headers, method, args);
        if (!request.oneway) {
            //noinspection unchecked
            Remote.PendingTasks.join(request.id, this, (DefaultCaller<Object>) call);
        }

        writeAndFlush(request).addListener((ChannelFutureListener) f -> {
            if (!f.isSuccess()) {
                call.tryFailure(f.cause());
                return;
            }

            if (request.oneway) {
                call.trySuccess(null);
                return;
            }
            Timeout timeout = Remote.parseTimeout(method);
            ScheduledFuture<?> sf = eventLoop().schedule(() -> call.tryFailure(TimeoutException.wrapException(method)), timeout.value(), timeout.unit());
            call.enqueue(future -> sf.cancel(true));
        });
        return call;
    }


    @Override
    public ChannelId id() {
        return this.channel.id();
    }

    @Override
    public String contextId() {
        return id().asLongText();
    }

    @Override
    public EventLoop eventLoop() {
        return channel.eventLoop();
    }

    @Override
    public Channel parent() {
        return channel.parent();
    }

    @Override
    public ChannelConfig config() {
        return channel.config();
    }

    @Override
    public boolean isOpen() {
        return channel.isOpen();
    }

    @Override
    public boolean isRegistered() {
        return channel.isRegistered();
    }

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public ChannelMetadata metadata() {
        return channel.metadata();
    }

    @Override
    public SocketAddress localAddress() {
        return channel.localAddress();
    }

    @Override
    public ChannelFuture closeFuture() {
        return channel.closeFuture();
    }

    @Override
    public boolean isWritable() {
        return channel.isWritable();
    }

    @Override
    public long bytesBeforeUnwritable() {
        return channel.bytesBeforeUnwritable();
    }

    @Override
    public long bytesBeforeWritable() {
        return channel.bytesBeforeWritable();
    }

    @Override
    public Unsafe unsafe() {
        return channel.unsafe();
    }

    @Override
    public ChannelPipeline pipeline() {
        return channel.pipeline();
    }

    @Override
    public ByteBufAllocator alloc() {
        return channel.alloc();
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return channel.bind(localAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return channel.connect(remoteAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return channel.connect(remoteAddress, localAddress);
    }

    @Override
    public ChannelFuture disconnect() {
        return channel.disconnect();
    }

    @Override
    public ChannelFuture close() {
        return channel.close();
    }

    @Override
    public ChannelFuture deregister() {
        return channel.deregister();
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return channel.bind(localAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return channel.connect(remoteAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return channel.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return channel.disconnect(promise);
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return channel.close(promise);
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return channel.deregister(promise);
    }

    @Override
    public Channel read() {
        return channel.read();
    }

    @Override
    public ChannelFuture write(Object msg) {
        return channel.write(msg);
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return channel.write(msg, promise);
    }

    @Override
    public Channel flush() {
        return channel.flush();
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return channel.writeAndFlush(msg, promise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return channel.writeAndFlush(msg);
    }

    @Override
    public ChannelPromise newPromise() {
        return channel.newPromise();
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return channel.newProgressivePromise();
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return channel.newSucceededFuture();
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return channel.newFailedFuture(cause);
    }

    @Override
    public ChannelPromise voidPromise() {
        return channel.voidPromise();
    }

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {
        return channel.attr(key);
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {
        return channel.hasAttr(key);
    }

    @Override
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }

    @Override
    public int compareTo(Channel ch) {
        return this.channel.compareTo(ch);
    }

    @Override
    public String toString() {
        return this.channel.toString();
    }
}
