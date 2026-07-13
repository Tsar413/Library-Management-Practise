package com.study.libraryManagement.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 图书评分请求DTO
 */
@Getter
@Setter
@ToString
public class BookRatingDTO {

    /**
     * 图书ISBN
     */
    private String isbn;

    /**
     * 评分值：1～5分
     */
    private Integer score;
}