package com.study.libraryManagement.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * 图书评分统计结果
 */
@Getter
@Setter
@ToString
public class BookRatingVO {

    /**
     * 图书ID
     */
    private Long bookId;

    /**
     * 图书ISBN
     */
    private String isbn;

    /**
     * 平均评分
     */
    private BigDecimal averageScore;

    /**
     * 评分人数
     */
    private Integer ratingCount;
}