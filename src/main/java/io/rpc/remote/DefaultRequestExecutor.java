package io.rpc.remote;

import io.netty.channel.ChannelPromise;
import io.rpc.Call;
import io.rpc.NoSuchMethodException;
import io.rpc.aop.Pipe;
import io.rpc.beans.Request;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

final class DefaultRequestExecutor implements RequestExecutor {

    private final Pipe pipe;
    private final Object object;
    private final Executor executor;
    private final RequestExecutor provider;
    private final Map<String, Method> callingMap = new ConcurrentHashMap<>();

    DefaultRequestExecutor(Pipe pipe, Object object, Executor executor, RequestExecutor provider) {
        this.pipe = pipe;
        this.object = object;
        this.executor = executor;
        this.provider = provider;

        Method[] methods = object.getClass().getDeclaredMethods();
        for (Method method : methods) {
            if (Call.class.isAssignableFrom(method.getReturnType())) {
                method.setAccessible(true);
                String id = Remote.methodId(method);
                callingMap.put(id, method);
            }
        }
    }

    public Executor executor() {
        return executor != null ? executor : provider.executor();
    }

    @Override
    public void execute(Remote remote, Request request) {
        final Executor executor = executor();
        executor.execute(() -> {
            String id = Remote.methodId(request);
            Method method;
            if ((method = callingMap.get(id)) == null) {
                remote.writeAndFlush(request.wrapThrowable(new NoSuchMethodException(id)));
                return;
            }

            ChannelPromise writePromise = remote.newPromise();

            Object result = null;
            Throwable cause = null;
            try {
                result = ((Call<?>) pipe.run(request.objectName, object, method, request.args, executor, writePromise)).get();
            } catch (Throwable t) {
                cause = unwrapThrowable(t);
            }

            if (!request.oneway) {
                remote.writeAndFlush(request.wrapResult(result, cause), writePromise);
            }
        });
    }

    private static Throwable unwrapThrowable(Throwable wrapped) {
        Throwable unwrapped = wrapped;
        while (true) {
            if (unwrapped instanceof InvocationTargetException) {
                unwrapped = ((InvocationTargetException) unwrapped).getTargetException();
            } else if (unwrapped instanceof UndeclaredThrowableException) {
                unwrapped = ((UndeclaredThrowableException) unwrapped).getUndeclaredThrowable();
            } else {
                return unwrapped;
            }
        }
    }
}