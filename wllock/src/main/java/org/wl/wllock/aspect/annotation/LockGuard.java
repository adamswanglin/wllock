package org.wl.wllock.aspect.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by wanglin on 2017/3/28.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LockGuard {
    /**
     * 锁名称
     *
     * @return
     */
    String name() default "";
}

