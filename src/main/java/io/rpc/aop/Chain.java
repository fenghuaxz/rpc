package io.rpc.aop;

import io.netty.channel.ChannelPromise;
import io.rpc.Context;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

public interface Chain {

    String objectName();

    Method method();

    Object[] args();

    Executor executor();

    Context session();

    ChannelPromise writePromise();

    Object proceed() throws Throwable;
}
