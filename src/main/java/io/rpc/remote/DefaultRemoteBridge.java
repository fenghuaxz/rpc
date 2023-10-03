package io.rpc.remote;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.*;
import io.rpc.Call;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

class DefaultRemoteBridge implements RemoteBridge {

    private final Remote remote;
    private final Executor executor;
    private final CloseFuture closeFuture = new CloseFuture();

    DefaultRemoteBridge(Bootstrap bootstrap, Executor executor) {
        this.executor = executor;
        this.remote = new DefaultRemote(bootstrap, closeFuture);
    }

    @Override
    public Promise<Void> closeFuture() {
        return closeFuture;
    }

    @Override
    public <T> T getMapper(Class<T> mapper) {
        return Remote.createProxyObject(null, mapper, remote, executor);
    }

    private static class DefaultRemote implements Remote {

        private volatile Remote remote;
        private final Bootstrap bootstrap;
        private final CloseFuture closeFuture;

        private DefaultRemote(Bootstrap bootstrap, CloseFuture closeFuture) {
            this.bootstrap = bootstrap;
            this.closeFuture = closeFuture;
        }

        @Override
        public boolean isActive() {
            return remote != null && remote.isActive();
        }

        @Override
        public boolean isWritable() {
            return remote != null && remote.isWritable();
        }

        @Override
        public ChannelFuture closeFuture() {
            return null;
        }

        @Override
        public <V> Call<V> execute(String objectName, Method method, Object[] args, Executor executor) {
            if (isActive()) {
                return remote.execute(objectName, method, args, executor);
            }

            DefaultCall<V> call = new DefaultCall<>(method, executor);

            bootstrap.connect().addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    call.tryFailure(future.cause());
                    return;
                }
                remote = Remote.attach(future.channel());
                remote.closeFuture().addListener((ChannelFutureListener) f -> closeFuture.trySuccess(null));
                remote.execute(objectName, method, args, executor).enqueue(f -> {
                    if (!f.isSuccess()) {
                        call.tryFailure(f.cause());
                        return;
                    }
                    //noinspection unchecked
                    call.trySuccess((V) f.obj());
                });
            });
            return call;
        }
    }

    private static class CloseFuture extends DefaultPromise<Void> {
        CloseFuture() {
            super(new DefaultEventExecutor());
        }
    }
}
