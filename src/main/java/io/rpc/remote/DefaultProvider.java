package io.rpc.remote;

import io.rpc.aop.Pipe;
import io.rpc.beans.Request;
import io.rpc.ObjectNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

final class DefaultProvider implements RequestExecutor {

    private final Pipe pipe;
    private final Executor executor;
    private final Map<String, RequestExecutor> objectMap = new ConcurrentHashMap<>();

    public DefaultProvider() {
        this(new ArrayList<>(), null);
    }

    DefaultProvider(List<Object> objectList, Executor executor) {
        this.pipe = Pipe.wrap(objectList, true);
        this.executor = executor != null ? executor : Runnable::run;

        for (Object object : objectList) {
            RequestExecutor mapper = new DefaultRequestExecutor(pipe, object, executor, this);
            Class<?>[] interfaces = object.getClass().getInterfaces();
            for (Class<?> clazz : interfaces) {
                String objectName = Remote.objectName(clazz);
                objectMap.put(objectName, mapper);
            }
        }
    }

    @Override
    public Executor executor() {
        return executor;
    }

    @Override
    public void put(String objectName, Object object, Executor executor) {
        objectMap.put(objectName, new DefaultRequestExecutor(pipe, object, executor, this));
    }

    @Override
    public void execute(Remote remote, Request request) {
        RequestExecutor executor = objectMap.get(request.objectName);
        if (executor == null) {
            remote.writeAndFlush(request.wrapThrowable(new ObjectNotFoundException(request.objectName)));
            return;
        }
        executor.execute(remote, request);
    }
}
