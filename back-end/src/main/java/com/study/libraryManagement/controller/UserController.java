package com.study.libraryManagement.controller;

import com.study.libraryManagement.common.Result;
import com.study.libraryManagement.dto.UserLoginDTO;
import com.study.libraryManagement.entity.User;
import com.study.libraryManagement.service.UserService;
import com.study.libraryManagement.util.TokenUtil;
import com.study.libraryManagement.vo.LoginVO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 查询所有用户
     *
     * 请求方式：
     * GET
     *
     * 请求地址：
     * /api/users/list
     */
    @GetMapping("/list")
    public ResponseEntity<Result<List<User>>> getAllUsers(){
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(Result.success(users));
    }

    /**
     * 用户登录
     *
     * 请求方式：
     * POST
     *
     * 请求地址：
     * /api/users/login
     *
     * 请求体示例：
     * {
     *   "username": "admin",
     *   "password": "123456"
     * }
     *
     * 当前阶段：
     * 只验证用户名和密码是否匹配。
     *
     * 后续阶段：
     * 1. 密码加密校验
     * 2. 登录成功返回 token
     * 3. Redis 保存 token
     */
    @PostMapping("/login")
    public ResponseEntity<Result<LoginVO>> login(@RequestBody UserLoginDTO loginDTO){
        User user1 = userService.login(loginDTO);
        if(user1 != null){
            LoginVO loginVO = new LoginVO(TokenUtil.createToken(user1.getUserId()), user1.getUserId(), user1.getUsername(), user1.getNickname(), user1.getRole());
            return ResponseEntity.ok(Result.success(loginVO));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized("用户不存在或密码错误"));
    }

}
