package com.study.libraryManagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书实体类
 *
 * 对应数据库表：
 * tb_book
 *
 * 当前同时使用：
 * 1. JPA 注解
 * 2. MyBatis-Plus 注解
 *
 * Lombok 注解用于自动生成：
 * getter、setter、toString、equals、hashCode 和构造方法。
 */
@Entity
@Table(name = "tb_book")
@TableName(value = "tb_book")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class Book {

    /**
     * 图书ID
     *
     * 数据库主键，使用 MySQL 自增策略。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    @TableId(value = "book_id", type = IdType.AUTO)
    private Long bookId;

    /**
     * ISBN 编号
     *
     * nullable = false：不能为空
     * unique = true：值不能重复
     */
    @Column(unique = true, nullable = false, length = 50)
    private String isbn;

    /**
     * 图书名称
     *
     * 对应数据库字段 book_name。
     */
    @Column(name = "book_name", nullable = false, length = 100)
    @TableField(value = "book_name")
    private String bookName;

    /**
     * 作者
     */
    @Column(length = 50)
    private String author;

    /**
     * 出版社
     */
    @Column(length = 100)
    private String publisher;

    /**
     * 图书定价
     *
     * BigDecimal 适合保存金额，
     * 避免 Double 可能产生的精度问题。
     *
     * precision = 10：总长度最多 10 位
     * scale = 2：保留 2 位小数
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * 馆藏位置
     *
     * 示例：
     * A区-2层-计算机书架
     */
    @Column(length = 100)
    private String location;

    /**
     * 图书总库存数量
     */
    private Integer stock;

    /**
     * 已借阅数量
     *
     * 可借数量可以通过以下方式计算：
     * stock - borrowedCount
     */
    @Column(name = "borrowed_count")
    @TableField(value = "borrowed_count")
    private Integer borrowedCount;

    /**
     * 图书封面图片地址
     *
     * 当前可以为空，
     * 后续完成图片上传或前端展示时再使用。
     */
    @Column(name = "image_url")
    @TableField(value = "image_url")
    private String imageUrl;

    /**
     * 图书状态
     *
     * 建议约定：
     * 1：正常
     * 0：下架
     */
    private Integer status;

    /**
     * 图书数据创建时间
     *
     * 返回 JSON 时格式化为：
     * yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "create_time")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 图书数据最后更新时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "update_time")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}