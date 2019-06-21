package com.spring.kang.service.impl;

import com.spring.kang.conf.anno.KService;
import com.spring.kang.service.UserService;

import java.util.HashMap;
import java.util.Map;

/**
 * @author kanghaijun
 * @create 2019/6/21
 * @describe
 */
@KService
public class UserServiceImpl implements UserService {

    @Override
    public Map<String,Object> get(Integer id) {
        Map<String,Object> user = new HashMap<>();
        user.put("id",id);
        user.put("name","jack");
        return user;
    }
}
