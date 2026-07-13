package com.study.libraryManagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_book_review")
@TableName(value = "tb_book_review")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class BookReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    @TableId(value = "review_id", type = IdType.AUTO)
    private Long reviewId;

    @Column(name = "user_id", nullable = false)
    @TableField(value = "user_id")
    private Long userId;

    @Column(name = "book_id", nullable = false)
    @TableField(value = "book_id")
    private Long bookId;

    @Lob
    @Column(name = "content", length = 500, nullable = false, columnDefinition = "LONGTEXT")
    @TableField(value = "content")
    private String content;

    private Integer status;

    /**
     * 借阅记录创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "create_time")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 借阅记录最后更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "update_time")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}
