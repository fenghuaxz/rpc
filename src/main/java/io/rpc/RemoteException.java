package io.rpc;

public class RemoteException extends RuntimeException {

    private String stackTraceMessage;

    public RemoteException setStackTraceMessage(String stackTraceMessage) {
        this.stackTraceMessage = stackTraceMessage;
        return this;
    }

    public String getStackTraceMessage() {
        return stackTraceMessage;
    }

    public RemoteException() {
        super();
    }

    public RemoteException(String message) {
        super(message);
    }

    public RemoteException(String message, Throwable cause) {
        super(message, cause);
    }

    public RemoteException(Throwable cause) {
        super(cause);
    }
}
