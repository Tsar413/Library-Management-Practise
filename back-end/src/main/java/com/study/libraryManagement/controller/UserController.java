package com.study.libraryManagement.controller;

import com.study.libraryManagement.common.Result;
import com.study.libraryManagement.dto.UserDTO;
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
    public ResponseEntity<Result<List<User>>> getAllUsers(@RequestAttribute("userId") Long userId){
        if (!userService.isAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.badRequest("权限不足，仅管理员可以查询用户列表"));
        }
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
    public ResponseEntity<Result<LoginVO>> login(@RequestBody UserDTO loginDTO){
        User user1 = userService.login(loginDTO);
        if(user1 != null){
            LoginVO loginVO = new LoginVO(TokenUtil.createToken(user1.getUserId()), user1.getUserId(), user1.getUsername(), user1.getNickname(), user1.getRole());
            return ResponseEntity.ok(Result.success(loginVO));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized("用户不存在或密码错误"));
    }

    /**
     * 用户注册
     *
     * 请求方式：
     * POST
     *
     * 请求地址：
     * /api/users/registration
     *
     * 请求体示例：
     * {
     *   "username": "zhangsan",
     *   "password": "123456"
     * }
     *
     * 处理流程：
     * 1. 接收前端传来的用户名和密码
     * 2. 调用 userService.registration() 执行注册逻辑
     * 3. 如果返回“注册成功”，说明用户保存成功
     * 4. 如果返回其他信息，例如“用户名已存在”“用户名不能为空”，则返回 400
     *
     * 返回说明：
     * 注册成功：
     * HTTP 状态码 200
     *
     * 注册失败：
     * HTTP 状态码 400
     */
    @PostMapping("/registration")
    public ResponseEntity<Result<String>> registration(@RequestBody UserDTO registrationDTO){
        String word = userService.registration(registrationDTO);
        if(word.equals("注册成功")){
            return ResponseEntity.ok(Result.success(word));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.badRequest(word));
    }
}
