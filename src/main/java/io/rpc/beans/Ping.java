package io.rpc.beans;

import java.io.Serializable;

public final class Ping implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Ping PING = new Ping();

    private Ping() {
    }
}
