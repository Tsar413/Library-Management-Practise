package com.study.libraryManagement.dto;

/**
 * 用户登录请求参数
 *
 * 用于接收前端或 Apifox 提交的登录 JSON。
 *
 * 请求示例：
 * {
 *   "username": "admin",
 *   "password": "123456"
 * }
 */
public class UserDTO {
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     *
     * 当前阶段用于明文测试。
     */

    private String password;

    public UserDTO() {
    }

    public UserDTO(String username, String password) {
        this.username = username;
        this.password = password;
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


}
