package example;

import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.rpc.ObjectClient;
import io.rpc.ObjectServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AppMain {

    public static void main(String[] args) throws Exception {

        new ObjectServer.Builder()
                .port(8005)
                .threads(new DefaultEventExecutorGroup(8))
                .impls(new HelloWorldImpl(), new LoginManagerImpl(), new AccessLog())
                .build()
                .open();

        Map<String, String> headers = new HashMap<>();
        headers.put("version", "1.0.0");
        headers.put("uuid", UUID.randomUUID().toString());

        ObjectClient client = new ObjectClient.Builder()
                .remoteAddress("localhost", 8005)
                .headers(headers)
                .build();

        //同步调用
        client.getMapper(LoginManager.class).login("a", "b", "c", new PushNotifierImpl()).get();

        //异步调用
        client.getMapper(HelloWorld.class).sayHi("hi").enqueue(future -> {
            if (!future.isSuccess()) {
                System.err.println("Call failed: " + future.cause().getMessage());
                return;
            }
            System.out.println("Call success!");
        });
    }
}
