package example;

import io.rpc.Call;
import io.rpc.annotations.Oneway;

public interface HelloWorld {

    Call<Void> sayHi(String text, Push push);

    interface Push {

        @Oneway
        Call<Void> push(String msg);
    }
}
