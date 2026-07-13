package com.study.libraryManagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.libraryManagement.dto.BookReviewDTO;
import com.study.libraryManagement.entity.BookReview;

import java.util.List;

public interface BookReviewService extends IService<BookReview> {
    List<BookReview> getAllBookReviews();

    List<BookReview> getOneBookReviews(String isbn);

    String addBookReview(BookReviewDTO bookReviewDTO, Long userId);

    String updateBookReview(BookReviewDTO bookReviewDTO, Long userId);

    String deleteBookReview(BookReviewDTO bookReviewDTO, Long userId);

    List<BookReview> getMyBookReviews(Long userId);
}
