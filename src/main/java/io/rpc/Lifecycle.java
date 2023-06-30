package io.rpc;

public interface Lifecycle {

    void linkToDeath(DeathRecipient deathRecipient);

    interface DeathRecipient {

        void onDied();
    }
}