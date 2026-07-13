package com.study.libraryManagement.controller;

import com.study.libraryManagement.common.Result;
import com.study.libraryManagement.dto.BookRatingDTO;
import com.study.libraryManagement.entity.BookRating;
import com.study.libraryManagement.service.BookRatingService;
import com.study.libraryManagement.vo.BookRatingVO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 图书评分控制层
 *
 * 当前提供：
 * 1. 添加或修改评分
 * 2. 查询图书平均分
 * 3. 查询我的评分
 */
@RestController
@RequestMapping("/api/book-rating")
public class BookRatingController {
    /**
     * 注入图书评分业务层对象
     */
    @Resource
    private BookRatingService bookRatingService;
    /**
     * 添加或修改评分
     *
     * 请求方式：
     * POST
     *
     * 请求地址：
     * /api/book-rating/save
     *
     * 请求体：
     * {
     *   "isbn": "9787300000001",
     *   "score": 5
     * }
     */
    @PostMapping("/save")
    public ResponseEntity<Result<String>> saveRating(@RequestBody BookRatingDTO bookRatingDTO, @RequestAttribute("userId") Long userId) {
        String result = bookRatingService.saveOrUpdateRating(bookRatingDTO, userId);
        if ("评分成功".equals(result) || "评分修改成功".equals(result)) {
            return ResponseEntity.ok(Result.success(result));
        }
        return ResponseEntity.badRequest().body(Result.badRequest(result));
    }

    /**
     * 查询某本书的平均评分和评分人数
     *
     * 请求方式：
     * GET
     *
     * 请求地址：
     * /api/book-rating/book/{isbn}
     */
    @GetMapping("/book/{isbn}")
    public ResponseEntity<Result<BookRatingVO>> getBookRating(@PathVariable String isbn) {
        BookRatingVO result = bookRatingService.getBookRating(isbn);
        if (result == null) {
            return ResponseEntity.badRequest().body(Result.badRequest("图书不存在或ISBN错误"));
        }
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 查询当前用户对某本书的评分
     *
     * 请求方式：
     * GET
     *
     * 请求地址：
     * /api/book-rating/my/{isbn}
     */
    @GetMapping("/my/{isbn}")
    public ResponseEntity<Result<BookRating>> getMyRating(@PathVariable String isbn, @RequestAttribute("userId") Long userId) {
        BookRating result = bookRatingService.getMyRating(isbn, userId);
        /*
         * 没有评分时返回成功，
         * data为null。
         *
         * 这样前端可以判断用户还没有评分。
         */
        return ResponseEntity.ok(Result.success(result));
    }
}