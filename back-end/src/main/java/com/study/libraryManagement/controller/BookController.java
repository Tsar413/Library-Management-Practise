package com.study.libraryManagement.controller;

import com.study.libraryManagement.common.Result;
import com.study.libraryManagement.entity.Book;
import com.study.libraryManagement.service.BookService;
import com.study.libraryManagement.service.BorrowRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 图书控制层
 *
 * 负责接收前端或 Apifox 发出的图书相关请求，
 * 调用 BookService 完成业务处理，
 * 最后使用 Result 统一封装响应结果。
 */
@RestController
@RequestMapping("/api/books")
public class BookController {

    /**
     * 注入图书业务层对象
     *
     * Spring 会自动查找 BookService 的实现类，
     * 当前对应的是 BookServiceImpl。
     */
    @Resource
    private BookService bookService;

    @Resource
    private BorrowRecordService borrowRecordService;

    /**
     * 查询全部图书
     *
     * 请求方式：
     * GET
     *
     * 请求地址：
     * /api/books/list
     *
     * 如果 WebConfig 已经拦截 /api/books/**，
     * 请求时需要在请求头中携带 token：
     *
     * Authorization: Bearer token值
     *
     * 返回示例：
     * {
     *   "code": 200,
     *   "message": "操作成功",
     *   "data": []
     * }
     *
     * @return 统一封装后的图书列表
     */
    @GetMapping("/list")
    public ResponseEntity<Result<List<Book>>> getAllBooks() {

        // 调用业务层查询数据库中的全部图书
        List<Book> books = bookService.getAllBooks();

        // 返回 HTTP 200，并使用 Result 统一封装查询结果
        return ResponseEntity.ok(Result.success(books));
    }

    /**
     * 借阅图书
     *
     * 请求方式：
     * POST
     *
     * 请求地址：
     * /api/books/borrow/{isbn}
     *
     * 请求示例：
     * POST /api/books/borrow/9787300000001
     *
     * 请求头需要携带：
     * Authorization: Bearer token值
     *
     * LoginInterceptor 会校验 token，
     * 并把 token 对应的用户 ID 放入 request：
     *
     * request.setAttribute("userId", userId);
     *
     * @param isbn   要借阅图书的 ISBN
     * @param userId 当前登录用户 ID，由拦截器写入请求属性
     * @return 统一封装后的借阅结果
     */
    @PostMapping("/borrow/{isbn}")
    public ResponseEntity<Result<String>> borrowBook(@PathVariable String isbn, @RequestAttribute("userId") Long userId){
        // 调用借阅业务，返回具体处理结果
        String result = borrowRecordService.borrowBook(isbn, userId);
        // 借阅成功，返回 HTTP 200
        if("借阅成功".equals(result)){
            return ResponseEntity.ok(Result.success(result));
        }
        /*
         * 用户未登录时返回 HTTP 401。
         */
        if("用户不存在或未登录".equals(result)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized(result));
        }
        /*
         * 其他业务失败情况返回 HTTP 400。
         *
         * 例如：
         * 1. 图书不存在
         * 2. ISBN 为空
         * 3. 超出借阅数量
         * 4. 重复借阅
         * 5. 库存不足
         */
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.badRequest(result));
    }


}