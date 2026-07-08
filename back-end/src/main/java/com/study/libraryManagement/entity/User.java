package com.study.libraryManagement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import javax.persistence.Id;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * 对应数据库表：
 * tb_user
 *
 * 作用：
 * 1. MyBatis-Plus 用于数据库 CRUD 操作
 * 2. JPA 用于练习 Repository 操作
 *
 * 注意：
 * 实际项目一般不会在同一个实体中同时使用 MyBatis-Plus 和 JPA。
 * 本项目为了练习两种技术，所以暂时保留。
 */
@Entity
@Table(name = "tb_user")
@TableName(value = "tb_user")
public class User {
    /**
     * 用户ID
     *
     * 数据库字段：
     * user_id
     *
     * JPA:
     * @Id 表示主键
     * @GeneratedValue 表示主键自动生成
     *
     * MyBatis-Plus:
     * @TableId 表示该字段是主键
     * IdType.AUTO 表示数据库自增长
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /**
     * 用户登录账号
     *
     * 示例：
     * admin
     * zhangsan
     */
    @Column(length = 50, nullable = false, unique = true)
    private String username;

    /**
     * 用户密码
     *
     * 注意：
     * 数据库中不能保存明文密码。
     *
     * 注册时应该：
     * 明文密码
     *       ↓
     * BCrypt加密
     *       ↓
     * 保存数据库
     */
    @Column(length = 100, nullable = false)
    private String password;

    /**
     * 用户昵称
     *
     * 可以为空。
     */
    @Column(length = 50)
    private String nickname;

    /**
     * 用户角色
     *
     * 用于权限控制。
     *
     * 示例：
     * USER  普通用户
     * ADMIN 管理员
     */
    @Column(length = 20)
    private String role;

    /**
     * 用户状态
     *
     * 约定：
     *
     * 1  正常
     * 0  禁用
     */
    private Integer status;

    /**
     * 创建时间
     *
     * LocalDateTime:
     * Java8 时间类型。
     *
     * JsonFormat:
     * 控制返回给前端的时间格式。
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "create_time")
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     *
     * 建议数据库增加：
     * update_time
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "update_time")
    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    public User(){}

    public User(Long userId, String username, String password, String nickname, String role, Integer status, LocalDateTime createTime, LocalDateTime updateTime) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", nickname='" + nickname + '\'' +
                ", role='" + role + '\'' +
                ", status=" + status +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
