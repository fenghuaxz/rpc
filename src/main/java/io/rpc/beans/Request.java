package io.rpc.beans;

import io.netty.util.internal.LongCounter;
import io.netty.util.internal.PlatformDependent;
import io.rpc.annotations.Oneway;
import io.rpc.annotations.Rpc;

import java.lang.reflect.Method;

public final class Request {

    public final int id;
    public final String objectName;
    public final String methodName;
    public final Class<?>[] paramTypes;
    public final Object[] args;
    public final boolean oneway;

    private static final LongCounter ID_COUNTER = PlatformDependent.newLongCounter();

    public Request(String objectName, Method method, Object[] args) {
        ID_COUNTER.increment();
        this.id = (int) ID_COUNTER.value();
        this.objectName = isEmpty(objectName) ? objectName(method.getDeclaringClass()) : objectName;
        this.methodName = method.getName();
        this.paramTypes = method.getParameterTypes();
        this.args = args;
        this.oneway = method.getAnnotation(Oneway.class) != null;
    }

    public Response wrapResult(Object result, Throwable cause) {
        return new Response(this.id, result, cause);
    }

    public Response failure(Throwable cause) {
        return new Response(this.id, null, cause);
    }

    private static boolean isEmpty(CharSequence text) {
        return text == null || text.length() == 0;
    }

    private static String objectName(Class<?> clazz) {
        Rpc rpc;
        if ((rpc = clazz.getAnnotation(Rpc.class)) != null) {
            return rpc.value();
        }
        return clazz.getName();
    }
}