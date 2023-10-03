package example;

import io.rpc.Call;

public interface HelloWorld {

    Call<Void> sayHi(String text);
}
