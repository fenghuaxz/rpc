package io.rpc;

public class NoSuchMethodException extends RemoteException{

    public NoSuchMethodException(String message){
        super(message);
    }
}
