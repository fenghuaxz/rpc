package io.rpc.remote;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.rpc.Lifecycle;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;

final class ObjectInvocationHandler implements InvocationHandler {

    private final Remote remote;
    private final String objectName;
    private final Executor executor;

    private Lifecycle.DeathRecipient deathRecipient;
    private final Lifecycle lifecycle = deathRecipient -> this.deathRecipient = deathRecipient;

    ObjectInvocationHandler(String objectName, Remote remote, Executor executor) {
        this.remote = remote;
        this.objectName = objectName;
        this.executor = executor;
        ChannelFuture closeFuture = remote.closeFuture();
        if (closeFuture != null) {
            closeFuture.addListener((ChannelFutureListener) future -> {
                if (deathRecipient != null) {
                    deathRecipient.onDied();
                }
            });
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(this, args);
        }
        if (method.getDeclaringClass() == Lifecycle.class) {
            return method.invoke(lifecycle, args);
        }
        return remote.execute(objectName, method, args, executor);
    }
}