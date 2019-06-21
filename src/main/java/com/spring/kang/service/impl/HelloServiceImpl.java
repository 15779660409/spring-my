package com.spring.kang.service.impl;

import com.spring.kang.conf.anno.KService;
import com.spring.kang.service.HelloService;

/**
 * @author kanghaijun
 * @create 2019/6/21
 * @describe
 */
@KService("helloService")
public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        return "spring from helloService" + name;
    }
}
