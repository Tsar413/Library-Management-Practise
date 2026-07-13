package com.study.libraryManagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 图书评分实体类
 *
 * 对应数据库表：
 * tb_book_rating
 */
@Entity
@Table(name = "tb_book_rating")
@TableName("tb_book_rating")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class BookRating {

    /**
     * 评分ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rating_id")
    @TableId(value = "rating_id", type = IdType.AUTO)
    private Long ratingId;

    /**
     * 评分用户ID
     */
    @Column(name = "user_id", nullable = false)
    @TableField("user_id")
    private Long userId;

    /**
     * 被评分图书ID
     */
    @Column(name = "book_id", nullable = false)
    @TableField("book_id")
    private Long bookId;

    /**
     * 评分值
     *
     * 允许范围：1～5分
     */
    @Column(name = "score", nullable = false)
    private Integer score;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "create_time")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "update_time")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}