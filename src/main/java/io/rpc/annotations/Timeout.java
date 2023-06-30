package io.rpc.annotations;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

@Inherited
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Timeout {

    long value();

    TimeUnit unit() default TimeUnit.SECONDS;
}
