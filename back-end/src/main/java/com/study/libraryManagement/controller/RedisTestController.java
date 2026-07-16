package com.study.libraryManagement.controller;

import com.study.libraryManagement.common.Result;
import com.study.libraryManagement.service.RedisTestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/redis-test")
public class RedisTestController {

    @Resource
    private RedisTestService redisTestService;

    @PostMapping("/send/{message}")
    public ResponseEntity<Result<String>> sendMessage(@PathVariable String message){
        String result = redisTestService.sendMessage(message);
        if("保存成功".equals(result)){
            return ResponseEntity.ok(Result.success(result));
        }
        return ResponseEntity.status(400).body(Result.badRequest(result));
    }

    @GetMapping("/receive")
    public ResponseEntity<Result<String>> receiveMessage(){
        String result = redisTestService.receiveMessage();
        return ResponseEntity.ok(Result.success(result));
    }
}
