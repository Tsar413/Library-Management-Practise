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
 * 图书借阅记录实体类
 *
 * 对应数据库表：
 * tb_borrow_record
 *
 * 每借阅一次图书，就会生成一条借阅记录。
 *
 * 主要记录：
 * 1. 哪个用户借书
 * 2. 借了哪本书
 * 3. 借阅时间
 * 4. 应还时间
 * 5. 实际归还时间
 * 6. 当前借阅状态
 */
@Entity
@Table(name = "tb_borrow_record")
@TableName(value = "tb_borrow_record")
@Getter
@Setter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class BorrowRecord {

    /**
     * 借阅记录ID
     *
     * 数据库主键，使用 MySQL 自增策略。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;

    /**
     * 借阅用户ID
     *
     * 对应 tb_user 表中的 user_id。
     */
    @Column(name = "user_id", nullable = false)
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 被借阅图书ID
     *
     * 对应 tb_book 表中的 book_id。
     */
    @Column(name = "book_id", nullable = false)
    @TableField(value = "book_id")
    private Long bookId;

    /**
     * 实际借阅时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "borrow_time")
    @TableField(value = "borrow_time")
    private LocalDateTime borrowTime;

    /**
     * 应还时间
     *
     * 后续可以根据借阅时间计算，
     * 例如借阅时间增加 30 天。
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "due_time")
    @TableField("due_time")
    private LocalDateTime dueTime;

    /**
     * 实际归还时间
     *
     * 尚未归还时，该字段为 null。
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "return_time")
    @TableField(value = "return_time")
    private LocalDateTime returnTime;

    /**
     * 借阅状态
     *
     * 建议约定：
     * 1：借阅中
     * 2：已归还
     * 3：已逾期
     */
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