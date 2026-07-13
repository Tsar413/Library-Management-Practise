package com.study.libraryManagement.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Getter
@Setter
@ToString(exclude = "image")
@EqualsAndHashCode(exclude = "image")
@RequiredArgsConstructor
public class BookDTO {

    private String isbn;

    private String bookName;

    private String author;

    private String publisher;

    private BigDecimal price;

    private String location;

    private Integer stock;

    private MultipartFile image;

}
