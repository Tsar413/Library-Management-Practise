package com.study.libraryManagement.dto;

import lombok.*;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class BookReviewDTO {
    private Long reviewId;
    private String isbn;
    private String content;
}
