package io.rpc.aop;

import io.netty.channel.ChannelPromise;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executor;

public interface Pipe {

    Node head();

    default Object run(String objectName, Object object, Method method, Object[] args, Executor executor, ChannelPromise promise) throws Exception {
        return new DefaultChain(head(), objectName, object, method, args, executor, promise).proceed();
    }

    static Pipe wrap(List<Object> aspects, boolean removeIfAdded) {
        return new DefaultPipe(aspects, removeIfAdded);
    }
}
