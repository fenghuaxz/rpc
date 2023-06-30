package example;

import io.rpc.*;

import java.util.HashMap;
import java.util.Map;

public class HelloWorldImpl implements HelloWorld {

    private final Map<Integer, Push> map = new HashMap<>();

    @Override
    public Call<Void> sayHi(String text, Push push) {
        System.out.println(text);
        Session session = Session.contextSession();

        ((Lifecycle) push).linkToDeath(() -> map.remove(push));

        push.push("你好!" + session);

        return Call.VOID;
    }
}
