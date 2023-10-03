package io.rpc.beans;

public final class Response {

    public final int id;
    public final Object result;
    public final Throwable cause;

    public Response(int id, Object result, Throwable cause) {
        this.id = id;
        this.result = result;
        this.cause = cause;
    }

//    private static Throwable wrapThrowable(Throwable cause) {
//        if (cause != null && !(cause instanceof RemoteException)) {
//            return new RemoteException(cause.getMessage());
//        }
//        return cause;
//    }
}