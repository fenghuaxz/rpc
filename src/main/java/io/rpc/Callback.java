package io.rpc;

public interface Callback<V> {

    void onComplete(Future<V> future);
}
