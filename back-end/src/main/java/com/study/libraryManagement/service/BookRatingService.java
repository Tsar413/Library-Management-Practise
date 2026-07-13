package com.study.libraryManagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.libraryManagement.dto.BookRatingDTO;
import com.study.libraryManagement.entity.BookRating;
import com.study.libraryManagement.vo.BookRatingVO;

/**
 * 图书评分业务接口
 */
public interface BookRatingService extends IService<BookRating> {

    /**
     * 新增或修改评分
     *
     * 同一用户对同一本书再次评分时，
     * 修改原来的评分记录。
     */
    String saveOrUpdateRating(BookRatingDTO bookRatingDTO, Long userId);

    /**
     * 查询某本书的平均评分和评分人数
     */
    BookRatingVO getBookRating(String isbn);

    /**
     * 查询当前用户对某本书的评分
     */
    BookRating getMyRating(String isbn, Long userId);
}