package com.study.libraryManagement.service.impl;

import com.study.libraryManagement.service.RedisTestService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class RedisTestServiceImpl implements RedisTestService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String sendMessage(String message) {
        try{
            stringRedisTemplate.opsForValue().set("library:message", message, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            return "Redis连接失败，保存失败";
        }

        return "保存成功";
    }

    @Override
    public String receiveMessage() {
        try {
            String message = stringRedisTemplate.opsForValue().get("library:message");
            if (message == null) {
                return "暂无消息";
            }
            return message;
        } catch (Exception e) {
            return "Redis连接失败，获取失败";
        }
    }
}
