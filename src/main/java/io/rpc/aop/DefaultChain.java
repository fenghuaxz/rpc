package io.rpc.aop;

import io.netty.channel.ChannelPromise;
import io.rpc.Context;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

final class DefaultChain implements Chain {

    private Node node;
    private final String objectName;
    private final Object object;
    private final Method method;
    private final Object[] args;
    private final Executor executor;
    private final ChannelPromise writePromise;

    DefaultChain(Node node, String objectName, Object object, Method method, Object[] args, Executor executor, ChannelPromise writePromise) {
        this.node = node;
        this.objectName = objectName;
        this.object = object;
        this.method = method;
        this.args = args;
        this.executor = executor;
        this.writePromise = writePromise;
    }

    @Override
    public Context session() {
        return (Context) writePromise.channel();
    }

    @Override
    public String objectName() {
        return objectName;
    }

    @Override
    public Method method() {
        return method;
    }

    @Override
    public Object[] args() {
        return args;
    }

    @Override
    public Executor executor() {
        return executor;
    }

    @Override
    public ChannelPromise writePromise() {
        return writePromise;
    }

    @Override
    public Object proceed() throws Throwable {
        if (node != null) {
            Node node = this.node;
            this.node = node.next;
            return node.aspect.proceed(this);
        }
        return method.invoke(object, args);
    }
}
