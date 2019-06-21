package com.spring.kang.conf.anno;

import java.lang.annotation.*;

/**
 * @author kanghaijun
 * @create 2019/6/21
 * @describe
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KRequestMapping {

    String value() default "";
}
