package io.rpc.remote;

import io.rpc.beans.Request;

import java.util.concurrent.Executor;

interface RequestExecutor {

    Executor executor();

    void execute(Remote remote, Request request);

    default void put(String objectName, Object object, Executor executor) {
        throw new UnsupportedOperationException();
    }
}