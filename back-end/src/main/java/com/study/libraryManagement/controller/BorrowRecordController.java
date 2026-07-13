package com.study.libraryManagement.controller;

import com.study.libraryManagement.common.Result;
import com.study.libraryManagement.entity.BorrowRecord;
import com.study.libraryManagement.service.BorrowRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/borrow-record")
public class BorrowRecordController {

    @Resource
    private BorrowRecordService borrowRecordService;

    /**
     * 查询全部借阅记录
     *
     * 请求方式：
     * GET
     *
     * 请求地址：
     * /api/borrow-record/lists-all
     *
     * 当前主要提供给管理员使用
     *
     * @return 统一封装后的全部借阅记录
     */
    @GetMapping("/lists-all")
    public ResponseEntity<Result<List<BorrowRecord>>> getAllLists(){
        // 调用业务层查询全部借阅记录
        List<BorrowRecord> result = borrowRecordService.getAllLists();
        // 返回 HTTP 200 和查询结果
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 管理员根据用户名查询指定用户的借阅记录
     *
     * 请求方式：
     * GET
     *
     * 请求地址：
     * /api/borrow-record/lists-one-admin/{username}
     *
     * 请求示例：
     * /api/borrow-record/lists-one-admin/zhangsan
     *
     * 处理流程：
     * 1. 接收路径中的用户名
     * 2. 根据用户名查询用户
     * 3. 根据用户 ID 查询借阅记录
     *
     * 当前主要提供给管理员使用，
     * 后续需要增加管理员权限校验。
     *
     * @param username 要查询的用户名
     * @return 指定用户的借阅记录
     */
    @GetMapping("/lists-one-admin/{username}")
    public ResponseEntity<Result<List<BorrowRecord>>> getOneListsAdmin(@PathVariable("username") String username){
        // 调用业务层查询指定用户的借阅记录
        List<BorrowRecord> result = borrowRecordService.getOneListsAdmin(username);
        // 返回 HTTP 200 和查询结果
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 查询当前登录用户自己的借阅记录
     *
     * 请求方式：
     * GET
     *
     * 请求地址：
     * /api/borrow-record/lists-one-user
     *
     * 请求头：
     * Authorization: Bearer token值
     *
     * LoginInterceptor 会根据 token 获取当前用户 ID，
     * 并通过以下方式放入 request：
     *
     * request.setAttribute("userId", userId);
     *
     * Controller 再通过 @RequestAttribute 获取 userId。
     *
     * userId 不由前端直接提交，
     * 可以避免普通用户通过修改参数查询他人记录。
     *
     * @param userId 当前登录用户 ID
     * @return 当前用户自己的借阅记录
     */
    @GetMapping("/lists-one-user")
    public ResponseEntity<Result<List<BorrowRecord>>> getOneListsUser(@RequestAttribute("userId") Long userId){
        // 调用业务层查询当前用户的借阅记录
        List<BorrowRecord> result = borrowRecordService.getOneListsUser(userId);
        // 返回 HTTP 200 和查询结果
        return ResponseEntity.ok(Result.success(result));
    }
}
