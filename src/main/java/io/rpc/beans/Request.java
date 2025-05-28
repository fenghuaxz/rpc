package io.rpc.beans;

import io.netty.util.internal.LongCounter;
import io.netty.util.internal.PlatformDependent;
import io.rpc.annotations.Oneway;
import io.rpc.annotations.Rpc;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class Request implements Serializable {

    private static final long serialVersionUID = 1L;

    public final int id;
    public final String objectName;
    public final String methodName;
    public final String[] paramTypes;
    public final Object[] args;
    public final boolean oneway;

    public final Map<String, String> headers;

    private static final LongCounter ID_COUNTER = PlatformDependent.newLongCounter();

    public Request(String objectName, Map<String, String> headers, Method method, Object[] args) {
        ID_COUNTER.increment();
        this.id = (int) ID_COUNTER.value();
        this.objectName = isEmpty(objectName) ? objectName(method.getDeclaringClass()) : objectName;
        this.methodName = method.getName();
        this.paramTypes = getParamTypeNames(method.getParameterTypes());
        this.args = args;
        this.oneway = method.getAnnotation(Oneway.class) != null;
        this.headers = headers;
    }

    private static String[] getParamTypeNames(Class<?>[] paramTypes) {
        String[] paramTypeNames = new String[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypeNames[i] = paramTypes[i].getName();
        }
        return paramTypeNames;
    }

    public Class<?>[] getParamTypes() {
        return Arrays.stream(paramTypes)
                .map(className -> {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException("Not found class: " + className, e);
                    }
                })
                .toArray(Class<?>[]::new);
    }

    public Response wrapResult(Object result, Throwable cause) {
        return new Response(this.id, result, cause);
    }

    public Response wrapThrowable(Throwable cause) {
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