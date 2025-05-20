package io.rpc.beans;

import io.rpc.RemoteException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;

public final class Response implements Serializable {

    private static final long serialVersionUID = 1L;

    public final int id;
    public final Object result;
    public final Throwable cause;

    public Response(int id, Object result, Throwable cause) {
        this.id = id;
        this.result = result;
        this.cause = cause != null ? wrapThrowable(cause) : null;
    }

    private static Throwable wrapThrowable(Throwable cause) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bos);
        cause.printStackTrace(ps);
        cause.setStackTrace(new StackTraceElement[0]);

        if (!(cause instanceof RemoteException)) {
            return new RemoteException(cause.getMessage())
                    .setStackTraceMessage(bos.toString());
        }
        return ((RemoteException) cause)
                .setStackTraceMessage(bos.toString());
    }
}