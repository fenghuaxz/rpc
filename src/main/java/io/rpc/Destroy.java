package io.rpc;

public interface Destroy {

    void linkToDeath(DeathRecipient deathRecipient);

    static void linkToDeath(Object object, DeathRecipient deathRecipient) {
        if (object instanceof Destroy) {
            ((Destroy) object).linkToDeath(deathRecipient);
        }
    }

    interface DeathRecipient {

        void onDied();
    }
}
