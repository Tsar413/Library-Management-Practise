package com.study.libraryManagement.controller;

import com.study.libraryManagement.common.Result;
import com.study.libraryManagement.dto.BookReviewDTO;
import com.study.libraryManagement.entity.BookReview;
import com.study.libraryManagement.service.BookReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/book-review")
public class BookReviewController {

    @Resource
    private BookReviewService bookReviewService;

    @GetMapping("/lists")
    public ResponseEntity<Result<List<BookReview>>> getAllBookReviews(){
        List<BookReview> result = bookReviewService.getAllBookReviews();
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 根据ISBN查询某本书的正常评论
     */
    @GetMapping("/list/{isbn}")
    public ResponseEntity<Result<List<BookReview>>> getOneBookReview(@PathVariable String isbn){
        List<BookReview> result = bookReviewService.getOneBookReviews(isbn);
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/list-me")
    public ResponseEntity<Result<List<BookReview>>> getMyBookReview(@RequestAttribute("userId") Long userId){
        List<BookReview> result = bookReviewService.getMyBookReviews(userId);
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 发表评论
     *
     * 请求方式：
     * POST
     *
     * 请求地址：
     * /api/book-review/add
     *
     * 请求体：
     * {
     *   "isbn": "9787300000001",
     *   "content": "这本书适合初学者"
     * }
     */
    @PostMapping("/add")
    public ResponseEntity<Result<String>> addBookReview(@RequestBody BookReviewDTO bookReviewDTO, @RequestAttribute("userId") Long userId){
        String result = bookReviewService.addBookReview(bookReviewDTO, userId);
        if ("评论成功".equals(result)) {
            return ResponseEntity.ok(Result.success(result));
        }
        return ResponseEntity.badRequest().body(Result.badRequest(result));
    }

    /**
     * 修改当前登录用户自己的评论
     * 请求方式：
     * PUT
     * 请求地址：
     * /api/book-review/update
     *
     * 请求头：
     * Authorization: Bearer token值
     * 请求体：
     * {
     * "reviewId": 1,
     * "isbn": "9787300000001",
     * "content": "修改后的评论内容"
     * }
     * 用户只能修改属于自己的正常评论。
     * @param bookReviewDTO 评论修改数据
     * @param userId 当前登录用户 ID
     * @return 评论修改结果
     */
    @PutMapping("/update")
    public ResponseEntity<Result<String>> updateBookReview(@RequestBody BookReviewDTO bookReviewDTO, @RequestAttribute("userId") Long userId){
        String result = bookReviewService.updateBookReview(bookReviewDTO, userId);
        if ("修改成功".equals(result)) {
            return ResponseEntity.ok(Result.success(result));
        }
        return ResponseEntity.badRequest().body(Result.badRequest(result));
    }

    /**
     * 逻辑删除当前登录用户自己的评论
     * 请求方式：
     * PUT
     * 请求地址：
     * /api/book-review/delete
     * 请求头：
     * Authorization: Bearer token值
     * 请求体：
     * {
     * "reviewId": 1
     * }
     * 当前不直接删除数据库记录，
     * 而是将评论 status 修改为 0。
     * 用户只能删除自己发表的正常评论。
     * @param bookReviewDTO 评论删除数据
     * @param userId 当前登录用户 ID
     * @return 评论删除结果
     */
    @PutMapping("/delete")
    public ResponseEntity<Result<String>> deleteBookReview(@RequestBody BookReviewDTO bookReviewDTO, @RequestAttribute("userId") Long userId){
        String result = bookReviewService.deleteBookReview(bookReviewDTO, userId);
        if ("删除成功".equals(result)) {
            return ResponseEntity.ok(Result.success(result));
        }
        return ResponseEntity.badRequest().body(Result.badRequest(result));
    }
}
