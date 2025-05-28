package io.rpc.remote;

import io.netty.channel.ChannelFuture;
import io.rpc.Destroy;
import io.rpc.PrimitiveType;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executor;

final class ObjectInvocationHandler implements InvocationHandler, PrimitiveType, Destroy {

    private final Remote remote;
    private final String objectName;
    private final Map<String, String> headers;

    private final Executor executor;
    private final Class<?> type;
    private Destroy.DeathRecipient deathRecipient;

    ObjectInvocationHandler(String objectName, Map<String, String> headers, Class<?> type, Remote remote, Executor executor) {
        this.remote = remote;
        this.objectName = objectName;
        this.headers = headers;
        this.type = type;
        this.executor = executor;
        ChannelFuture closeFuture = remote.closeFuture();
        if (closeFuture != null) closeFuture.addListener(f -> {
            if (deathRecipient != null) {
                deathRecipient.onDied();
            }
        });
    }

    @Override
    public Class<?> type() {
        return type;
    }

    @Override
    public void linkToDeath(DeathRecipient deathRecipient) {
        this.deathRecipient = deathRecipient;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getDeclaringClass() == Object.class || method.getDeclaringClass() == PrimitiveType.class || method.getDeclaringClass() == Destroy.class) {
            return method.invoke(this, args);
        }
        return remote.execute(objectName, headers, method, args, executor);
    }

    @Override
    public String toString() {
        return type.getName() + "@" + Integer.toHexString(hashCode());
    }
}