package io.rpc.remote;

import io.rpc.Call;
import io.rpc.Callback;
import io.rpc.Future;
import io.rpc.annotations.Oneway;
import io.rpc.annotations.Timeout;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

final class DefaultCaller<V> implements Call<V>, Future<V> {

    private final Executor executor;
    private final List<Callback<V>> callbacks = new CopyOnWriteArrayList<>();
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private volatile Object result;

    private final Method method;

    DefaultCaller(Method method, Executor executor) {
        this.method = method;
        this.executor = executor == null ? Runnable::run : executor;
    }

    private boolean isDone() {
        return result != null;
    }

    @Override
    public boolean isOneway() {
        return method.getAnnotation(Oneway.class) != null;
    }

    @Override
    public boolean isSuccess() {
        return isDone() && !(result instanceof CauseHolder);
    }

    @Override
    public Throwable cause() {
        return result instanceof CauseHolder ? ((CauseHolder) result).cause : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V obj() {
        return result instanceof Signal || result instanceof CauseHolder ? null : (V) result;
    }

    @Override
    public V get() throws ExecutionException, TimeoutException {
        Timeout timeout = Remote.parseTimeout(method);
        return get(timeout.value(), timeout.unit());
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws ExecutionException, TimeoutException {
        if (isDone()) {
            return obj();
        }

        lock.lock();
        try {
            try {
                if (!condition.await(timeout, unit)) {
                    throw io.rpc.TimeoutException.wrapException(method);
                }

                Throwable ex;
                if ((ex = cause()) != null) {
                    throw new ExecutionException(ex.getMessage(), ex);
                }

                return obj();
            } catch (InterruptedException e) {
                throw new ExecutionException(e.getMessage(), e);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Call<V> enqueue(Callback<V> callback) {
        if (callback == null) {
            throw new NullPointerException("callback == null");
        }

        if (isDone()) {
            callback.onComplete(this);
            return this;
        }
        callbacks.add(callback);
        return this;
    }

    void trySuccess(V val) {
        if (trySuccess0(val)) {
            executor.execute(() -> {
                for (Callback<V> callback : callbacks) {
                    callback.onComplete(this);
                }
            });
        }
    }

    private boolean trySuccess0(V val) {
        if (isDone()) {
            return false;
        }

        lock.lock();
        try {
            this.result = val != null ? val : Signal.SIGNAL;
            condition.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    void tryFailure(Throwable cause) {
        if (tryFailure0(cause)) {
            executor.execute(() -> {
                for (Callback<V> callback : callbacks) {
                    callback.onComplete(this);
                }
            });
        }
    }

    private boolean tryFailure0(Throwable cause) {
        if (isDone()) {
            return false;
        }
        lock.lock();
        try {
            this.result = new CauseHolder(cause);
            condition.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    private static class Signal {

        @SuppressWarnings("InstantiationOfUtilityClass")
        static final Signal SIGNAL = new Signal();
    }

    private static class CauseHolder {

        private final Throwable cause;

        private CauseHolder(Throwable cause) {
            this.cause = cause;
        }
    }
}
