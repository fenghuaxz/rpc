package example;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.rpc.*;

public class AppMain {

    public static void main(String[] args) throws Exception {

        new ObjectServer.Builder()
                .port(8000)
                .threads(new DefaultEventExecutorGroup(8))
                .objects(new HelloWorldImpl(), new AccessLog())
                .build()
                .open();

        ObjectClient client = new ObjectClient.Builder()
                .remoteAddress("localhost", 8000)
                .build();

        client.getMapper(HelloWorld.class).sayHi("hi").enqueue(new Callback<Void>() {
            @Override
            public void onComplete(Future<Void> future) {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
                }
            }
        });
    }
}
