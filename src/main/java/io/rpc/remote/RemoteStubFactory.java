package io.rpc.remote;

import io.rpc.aop.Aspect;
import io.rpc.aop.Chain;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

final class RemoteStubFactory implements Aspect {

    private final Map<Method, Parameter[]> parameterCache = new ConcurrentHashMap<>();

    @Override
    public Object proceed(Chain chain) throws Exception {
        Method method = chain.method();
        Parameter[] parameters = parameterCache.computeIfAbsent(method, RemoteStubFactory::wrapParameters);
        for (Parameter parameter : parameters) {
            String objectName = Remote.parameterObjectName(chain.objectName(), method, parameter.index);
            chain.args()[parameter.index] = Remote.createProxyObject(objectName, parameter.parameterType, (Remote) chain.session(), chain.executor());
        }
        return chain.proceed();
    }

    private static Parameter[] wrapParameters(Method method) {
        return IntStream.range(0, method.getParameterCount())
                .mapToObj(i -> new Parameter(i, method.getParameterTypes()[i]))
                .filter(info -> info.parameterType.isInterface())
                .toArray(Parameter[]::new);
    }

    private static class Parameter {

        final int index;
        final Class<?> parameterType;

        private Parameter(int index, Class<?> parameterType) {
            this.index = index;
            this.parameterType = parameterType;
        }
    }
}
