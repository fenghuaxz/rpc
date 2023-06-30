package example;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.rpc.*;

public class AppMain {

    public static void main(String[] args) throws Exception {

        new ObjectServer.Builder()
                .port(8000)
                .threads(new DefaultEventExecutorGroup(8))
                .impl(new HelloWorldImpl(), new AccessLog())
                .build()
                .open();

        ObjectClient client = new ObjectClient.Builder()
                .remoteAddress("localhost", 8000)
                .build();

        client.getMapper(HelloWorld.class).sayHi("hi", new HelloWorld.Push() {
            @Override
            public Call<Void> push(String msg) {
                System.out.println("推送消息:" + msg);
                return Call.VOID;
            }
        }).enqueue(new Callback<Void>() {
            @Override
            public void onComplete(Future<Void> future) {
                if (!future.isSuccess()){
                    future.cause().printStackTrace();
                }
            }
        });
    }
}
