package io.rpc;

public interface Future<V> {

    boolean isOneway();

    boolean isSuccess();

    Throwable cause();

    V obj();
}
