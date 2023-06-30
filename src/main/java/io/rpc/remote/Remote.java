package io.rpc.remote;

import io.netty.channel.*;
import io.rpc.Call;
import io.rpc.Lifecycle;
import io.rpc.Session;
import io.rpc.annotations.Rpc;
import io.rpc.annotations.Timeout;
import io.rpc.beans.Request;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

interface Remote extends Session {

    default boolean isActive() {
        throw new UnsupportedOperationException("Stub!");
    }

    default ChannelFuture writeAndFlush(Object msg) {
        throw new UnsupportedOperationException("Stub!");
    }

    default ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        throw new UnsupportedOperationException("Stub!");
    }

    default ChannelPromise newPromise() {
        throw new UnsupportedOperationException("Stub!");
    }

    default RequestExecutor provider() {
        throw new UnsupportedOperationException("Stub!");
    }

    default void setProvider(RequestExecutor provider) {
        throw new UnsupportedOperationException("Stub!");
    }

    default <V> Call<V> execute(String objectName, Method method, Object[] args, Executor executor) {
        throw new UnsupportedOperationException("Stub!");
    }

    static String objectName(Class<?> clazz) {
        Rpc rpc;
        if ((rpc = clazz.getAnnotation(Rpc.class)) != null) {
            return rpc.value();
        }
        return clazz.getName();
    }

    static String parameterObjectName(String objectName, Method method, int index) {
        return objectName + "." + method.getName() + "#p" + index;
    }

    static String parameterObjectName(Method method, int index) {
        return objectName(method.getDeclaringClass()) + "." + method.getName() + "#p" + index;
    }

    static String methodId(Method method) {
        return method.getName() + "#" + Arrays.toString(method.getParameterTypes()).replaceAll("\\s+", "");
    }

    static String methodId(Request request) {
        return request.methodName + "#" + Arrays.toString(request.paramTypes).replaceAll("\\s+", "");
    }

    static <T> T createProxyObject(String objectName, Class<T> clazz, Remote remote, Executor executor) {
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz, Lifecycle.class}, new ObjectInvocationHandler(objectName, remote, executor));
    }

    static Timeout parseTimeout(Method method) {
        Timeout timeout;
        if ((timeout = method.getAnnotation(Timeout.class)) == null) {
            if ((timeout = method.getDeclaringClass().getAnnotation(Timeout.class)) == null) {
                timeout = Call.class.getAnnotation(Timeout.class);
            }
        }
        return timeout;
    }

    static Remote attach(Channel ch) {
        try {
            if (ch != null && !(ch instanceof Remote)) {
                Field field;
                Remote remote = new DefaultSession(ch);
                field = DefaultChannelPipeline.class.getDeclaredField("channel");
                field.setAccessible(true);
                field.set(ch.pipeline(), remote);
                field = DefaultChannelPromise.class.getDeclaredField("channel");
                field.setAccessible(true);
                field.set(ch.closeFuture(), remote);
                return remote;
            }
            return (Remote) ch;
        } catch (Exception e) {
            throw new ChannelException("Attach failed.", e);
        }
    }

    final class PendingTasks {

        private static final Map<Remote, Map<Integer, DefaultCall<Object>>> PENDING_TASKS = new ConcurrentHashMap<>();
        private static final ChannelFutureListener DISCONNECT_LISTENER = future -> PENDING_TASKS.remove((Remote) future.channel());

        static void join(int id, Remote remote, DefaultCall<Object> call) {
            Map<Integer, DefaultCall<Object>> callMap;
            if ((callMap = PENDING_TASKS.get(remote)) == null) {
                PENDING_TASKS.put(remote, callMap = new ConcurrentHashMap<>());
                remote.closeFuture().addListener(DISCONNECT_LISTENER);
            }
            callMap.put(id, call);
        }

        static void processResult(int id, Remote remote, Throwable cause, Object result) {
            Map<Integer, DefaultCall<Object>> callMap;
            if ((callMap = PENDING_TASKS.get(remote)) != null) {
                DefaultCall<Object> call;
                if ((call = callMap.remove(id)) != null) {
                    if (cause != null) {
                        call.tryFailure(cause);
                        return;
                    }
                    call.trySuccess(result);
                }
            }
        }
    }
}
