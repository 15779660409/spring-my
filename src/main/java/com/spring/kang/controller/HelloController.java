package com.spring.kang.controller;

import com.spring.kang.conf.anno.KAutowrited;
import com.spring.kang.conf.anno.KController;
import com.spring.kang.conf.anno.KRequestMapping;
import com.spring.kang.conf.anno.KRequestParam;
import com.spring.kang.service.HelloService;
import com.spring.kang.service.UserService;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author kanghaijun
 * @create 2019/6/21
 * @describe
 */
@KController
@KRequestMapping("demo")
public class HelloController {

    @KAutowrited
    private HelloService helloService;

    @KAutowrited("userService")
    private UserService userService;

    @KRequestMapping("sayHello")
    public String sayHello(@KRequestParam(value = "name") String name, HttpServletRequest request,String value){
        return "spring is  " + name;
    }

    @KRequestMapping("helloService")
    public String hello(@KRequestParam(value = "name") String name){
        return helloService.sayHello(name);
    }

    @KRequestMapping("userService")
    public Map<String,Object> user(@KRequestParam(value = "id") Integer id){
        return userService.get(id);
    }

}
