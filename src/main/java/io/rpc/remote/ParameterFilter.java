package io.rpc.remote;

import io.rpc.annotations.Nullable;
import io.rpc.aop.Aspect;
import io.rpc.aop.Chain;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

final class ParameterFilter implements Aspect {

    private final Map<Method, NotNull[]> parameterCache = new ConcurrentHashMap<>();

    @Override
    public Object proceed(Chain chain) throws Throwable {
        Method method = chain.method();
        Object[] args = chain.args();

        NotNull[] parameters = parameterCache.computeIfAbsent(method, ParameterFilter::wrapParameters);

        for (NotNull parameter : parameters) {
            int index = parameter.index;
            Object arg = args[index];
            if (arg == null) {
                throw new IllegalArgumentException(String.format("Parameter %d of method %s cannot be null", (index + 1), method.getName()));
            }
        }
        return chain.proceed();
    }

    private static NotNull[] wrapParameters(Method method) {
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        return IntStream.range(0, parameterAnnotations.length)
                .filter(i -> Arrays.stream(parameterAnnotations[i])
                        .noneMatch(annotation -> annotation instanceof Nullable))
                .mapToObj(NotNull::new)
                .toArray(NotNull[]::new);
    }

    private static class NotNull {

        final int index;

        private NotNull(int index) {
            this.index = index;
        }
    }
}
