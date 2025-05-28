package example;

import io.rpc.*;

public class HelloWorldImpl implements HelloWorld {


    @Override
    public Call<Void> sayHi(String text) {
        System.out.println(text);
        System.out.println("请求头: " + Context.headers());

        return Call.VOID;
    }
}
