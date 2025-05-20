package io.rpc;

import io.rpc.annotations.Timeout;
import io.rpc.remote.Bridge;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Timeout(5)
public interface Call<V> {

    Call<Void> VOID = of(null);

    default V get() throws ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Stub!");
    }

    default V get(long time, TimeUnit unit) throws ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("Stub!");
    }

    default Call<V> enqueue(Callback<V> callback) {
        throw new UnsupportedOperationException("Stub!");
    }

    static <V> Call<V> of(V val) {
        return new Call<V>() {
            @Override
            public V get() {
                return val;
            }
        };
    }
}
