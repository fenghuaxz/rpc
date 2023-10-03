package example;

import io.rpc.*;

import java.util.HashMap;
import java.util.Map;

public class HelloWorldImpl implements HelloWorld {


    @Override
    public Call<Void> sayHi(String text) {
        System.out.println(text);
        return Call.VOID;
    }
}
