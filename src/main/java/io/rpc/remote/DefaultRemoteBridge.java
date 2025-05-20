package io.rpc.remote;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.concurrent.*;
import io.rpc.Call;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

class DefaultRemoteBridge implements RemoteBridge {

    private final Remote remote;
    private final Executor executor;
    private final CloseFuture closeFuture = new CloseFuture();
    private final Map<String, String> headers;
    private final Map<Class<?>, Object> objects = new ConcurrentHashMap<>();

    DefaultRemoteBridge(Bootstrap bootstrap, Map<String, String> headers, Executor executor) {
        this.executor = executor;
        this.headers = headers;
        this.remote = new DefaultRemote(bootstrap, closeFuture);
    }

    @Override
    public void close() {
        remote.close();
    }

    @Override
    public void setOnCloseListener(Runnable runnable) {
        this.closeFuture.setOnCloseListener(runnable);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMapper(Class<T> mapper) {
        return (T) objects.computeIfAbsent(mapper, k -> Remote.createProxyObject(k, headers, remote, executor));
    }

    private static class DefaultRemote implements Remote {

        private volatile Remote remote;
        private final Bootstrap bootstrap;
        private final GenericFutureListener<Future<? super Void>> closeFutureListener;

        private DefaultRemote(Bootstrap bootstrap, GenericFutureListener<Future<? super Void>> closeFutureListener) {
            this.bootstrap = bootstrap;
            this.closeFutureListener = closeFutureListener;
        }

        @Override
        public ChannelFuture close() {
            return remote != null ? remote.close() : null;
        }

        @Override
        public boolean isActive() {
            return remote != null && remote.isActive();
        }

        @Override
        public ChannelFuture closeFuture() {
            return null;
        }

        @Override
        public <V> Call<V> execute(String objectName, Map<String, String> headers, Method method, Object[] args, Executor executor) {
            if (isActive()) {
                return remote.execute(objectName, headers, method, args, executor);
            }

            DefaultCaller<V> call = new DefaultCaller<>(method, executor);

            bootstrap.connect().addListener((ChannelFutureListener) future -> {
                if (!future.isSuccess()) {
                    call.tryFailure(future.cause());
                    return;
                }
                remote = Remote.attach(future.channel());
                remote.closeFuture().addListener(closeFutureListener);
                remote.execute(objectName, headers, method, args, executor).enqueue(f -> {
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

    private static class CloseFuture implements GenericFutureListener<Future<? super Void>> {

        private volatile Runnable runnable;

        private void setOnCloseListener(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void operationComplete(Future<? super Void> future) {
            if (runnable != null) runnable.run();
        }
    }
}
