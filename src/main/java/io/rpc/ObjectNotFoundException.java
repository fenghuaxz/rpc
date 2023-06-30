package io.rpc;

public class ObjectNotFoundException extends RemoteException {

    public ObjectNotFoundException(String message) {
        super(message);
    }
}
