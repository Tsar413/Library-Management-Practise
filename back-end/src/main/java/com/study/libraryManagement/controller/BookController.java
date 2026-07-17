package com.study.libraryManagement.controller;

import com.study.libraryManagement.common.Result;
import com.study.libraryManagement.dto.BookDTO;
import com.study.libraryManagement.entity.Book;
import com.study.libraryManagement.service.BookService;
import com.study.libraryManagement.service.BorrowRecordService;
import com.study.libraryManagement.service.UserService;
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

    @Resource
    private UserService userService;
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
    @GetMapping("/query/{isbn}")
    public ResponseEntity<Result<Book>> getBookByISBN(@PathVariable String isbn) {
        Book book = bookService.getBookByISBN(isbn);
        if (book == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("图书不存在或已下架"));
        }
        return ResponseEntity.ok(Result.success(book));
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

    /**
     * 根据 ISBN 归还图书
     * 请求方式：
     * POST
     *
     * 请求地址：
     * /api/books/return/{isbn}
     *
     * 请求示例：
     * POST /api/books/return/9787300000001
     *
     * 请求头：
     * Authorization: Bearer token值
     *
     * LoginInterceptor 会读取请求头中的 token，
     * 根据 token 查询当前用户 ID，
     * 并将 userId 放入本次请求：
     *
     * request.setAttribute("userId", userId);
     *
     * Controller 使用 @RequestAttribute 获取当前用户 ID。
     *
     * @param isbn   要归还图书的 ISBN
     * @param userId 当前登录用户 ID
     * @return 统一封装后的归还结果
     */
    @PostMapping("/return/{isbn}")
    public ResponseEntity<Result<String>> returnBook(@PathVariable String isbn, @RequestAttribute("userId") Long userId){
        // 调用借阅业务层完成还书操作
        String result = borrowRecordService.returnBook(isbn, userId);
        // 归还成功，返回 HTTP 200
        if("归还成功".equals(result)){
            return ResponseEntity.ok(Result.success(result));
        }
        /*
         * 用户未登录时返回 HTTP 401。
         */
        if("用户不存在或未登录".equals(result)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.unauthorized(result));
        }
        /*
         * 其他业务错误返回 HTTP 400。
         *
         * 可能的情况包括：
         * 1. ISBN 为空
         * 2. 图书不存在
         * 3. 当前用户没有该图书的未归还记录
         */
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.badRequest(result));
    }

    /**
     * 手动扫描并更新逾期未归还记录
     *
     * 请求方式：
     * POST
     *
     * 请求地址：
     * /api/books/update-overdue
     *
     * 请求头：
     * Authorization: Bearer 管理员token
     *
     * 处理流程：
     * 1. 调用 BorrowRecordService 扫描借阅记录
     * 2. 将 status = 1 且 due_time 早于当前时间的记录
     *    更新为 status = 3
     * 3. 返回本次实际更新的记录数量
     *
     * 当前接口可以由管理员手动调用。
     * 后续也可以在管理员登录成功后，
     * 自动调用同一个 updateOverdue() 方法。
     *
     * @return 统一封装后的更新结果
     */
    @PostMapping("/update-overdue")
    public ResponseEntity<Result<String>> updateOverdue(@RequestAttribute("userId") Long userId){
        if (!userService.isAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.badRequest("权限不足，仅管理员可以更新逾期记录"));
        }
        // 调用业务层扫描并更新逾期记录
        String result = borrowRecordService.updateOverdue();
        // 更新成功，返回 HTTP 200
        if("更新成功".equals(result)){
            return ResponseEntity.ok(Result.success(result));
        }
        // 更新出现业务异常时返回 HTTP 400
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.badRequest(result));
    }

    /**
     * 根据关键词模糊查询图书
     *
     * 请求方式：
     * GET
     *
     * 请求地址：
     * /api/books/search?keyword=Java
     *
     * 查询范围：
     * 1. ISBN
     * 2. 图书名称
     * 3. 作者
     * 4. 出版社
     * 5. 馆藏位置
     *
     * 请求头：
     * Authorization: Bearer token值
     *
     * @param keyword 用户输入的查询关键词
     * @return 统一封装后的图书查询结果
     */
    @GetMapping("/search/{keyword}")
    public ResponseEntity<Result<List<Book>>> searchBooks(@PathVariable String keyword, @RequestAttribute("userId") Long userId){
        List<Book> result = bookService.searchBooks(keyword);
        // 查询成功，返回 HTTP 200
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping(value = "/add", consumes = "multipart/form-data")
    public ResponseEntity<Result<String>> addNewBook(@ModelAttribute BookDTO bookDTO, @RequestAttribute("userId") Long userId){
        if (!userService.isAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.badRequest("权限不足，仅管理员可以新增图书"));
        }
        String result = bookService.addBook(bookDTO);
        if ("保存成功".equals(result)) {
            return ResponseEntity.ok(Result.success(result));
        }
        return ResponseEntity.badRequest().body(Result.badRequest(result));
    }

    @PutMapping(value = "/update", consumes = "multipart/form-data")
    public ResponseEntity<Result<String>> updateBook(@ModelAttribute BookDTO bookDTO, @RequestAttribute("userId") Long userId){
        if (!userService.isAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.badRequest("权限不足，仅管理员可以修改图书"));
        }
        String result = bookService.updateBook(bookDTO);
        if ("修改成功".equals(result)) {
            return ResponseEntity.ok(Result.success(result));
        }
        return ResponseEntity.badRequest().body(Result.badRequest(result));
    }

    @PutMapping("/status/{isbn}")
    public ResponseEntity<Result<String>> updateStatus(@PathVariable String isbn, @RequestAttribute("userId") Long userId){
        if (!userService.isAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.badRequest("权限不足，仅管理员可以修改图书状态"));
        }
        String result = bookService.updateStatus(isbn);
        if ("修改成功".equals(result)) {
            return ResponseEntity.ok(Result.success(result));
        }
        return ResponseEntity.badRequest().body(Result.badRequest(result));
    }
}