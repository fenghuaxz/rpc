package example;

import io.rpc.aop.Aspect;
import io.rpc.aop.Chain;

public class AccessLog implements Aspect {

    @Override
    public Object proceed(Chain chain) throws Exception {
        System.out.println("接口:" + chain.objectName());
        System.out.println("方法调用:" + chain.method().getName());
        return chain.proceed();
    }
}
