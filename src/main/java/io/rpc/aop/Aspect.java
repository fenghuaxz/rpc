package io.rpc.aop;

public interface Aspect {

    Object proceed(Chain chain) throws Throwable;
}