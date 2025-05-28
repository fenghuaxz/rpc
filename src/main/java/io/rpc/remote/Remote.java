package io.rpc.remote;

import io.netty.channel.*;
import io.rpc.Call;
import io.rpc.Destroy;
import io.rpc.Context;
import io.rpc.PrimitiveType;
import io.rpc.annotations.Rpc;
import io.rpc.annotations.Timeout;
import io.rpc.beans.Request;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

interface Remote extends Context {

    default boolean isActive() {
        throw new UnsupportedOperationException("Stub!");
    }

    default boolean isWritable() {
        throw new UnsupportedOperationException("Stub!");
    }

    default ChannelFuture writeAndFlush(Object msg) {
        throw new UnsupportedOperationException("Stub!");
    }

    default ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        throw new UnsupportedOperationException("Stub!");
    }

    default ChannelFuture close() {
        throw new UnsupportedOperationException("Stub!");
    }

    default EventLoop eventLoop() {
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

    default void setHaproxyAddress(SocketAddress address) {
        throw new UnsupportedOperationException("Stub!");
    }

    default <V> Call<V> execute(String objectName, Map<String, String> headers, Method method, Object[] args, Executor executor) {
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
        return method.getName() + "#" + Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .collect(Collectors.joining(", ", "[", "]"))
                .replaceAll("\\s+", "");
    }

    static String methodId(Request request) {
        return request.methodName + "#" + Arrays.toString(request.paramTypes).replaceAll("\\s+", "");
    }

    static <T> T createProxyObject(Class<T> clazz, Map<String, String> headers, Remote remote, Executor executor) {
        return createProxyObject(null, headers, clazz, remote, executor);
    }

    @SuppressWarnings("unchecked")
    static <T> T createProxyObject(String objectName, Map<String, String> headers, Class<T> clazz, Remote remote, Executor executor) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz, PrimitiveType.class, Destroy.class}
                , new ObjectInvocationHandler(objectName, headers, clazz, remote, executor));
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
                Remote remote = new DefaultContext(ch);
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

        private static final Map<Remote, Map<Integer, DefaultCaller<Object>>> PENDING_TASKS = new ConcurrentHashMap<>();

        static void join(int id, Remote remote, DefaultCaller<Object> call) {
            Map<Integer, DefaultCaller<Object>> callMap;
            if ((callMap = PENDING_TASKS.get(remote)) == null) {
                PENDING_TASKS.put(remote, callMap = new ConcurrentHashMap<>());
                remote.closeFuture().addListener(future -> PENDING_TASKS.remove(remote));
            }
            callMap.put(id, call);
        }

        static void processResult(int id, Remote remote, Throwable cause, Object result) {
            Map<Integer, DefaultCaller<Object>> callMap;
            if ((callMap = PENDING_TASKS.get(remote)) != null) {
                DefaultCaller<Object> call;
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
