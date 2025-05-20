package io.rpc;

import java.lang.reflect.Method;

public class TimeoutException extends RemoteException {

    public final Method method;

    private TimeoutException(Method method) {
        super("timeout: " + method.getName());
        this.method = method;
    }

    public static TimeoutException wrapException(Method method) {
        return new TimeoutException(method);
    }
}
