package com.study.libraryManagement.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码工具类
 *
 * 使用 BCrypt 对密码进行不可逆加密。
 *
 * 注册时：
 * 明文密码 -> BCrypt加密 -> 保存数据库
 *
 * 登录时：
 * 明文密码 + 数据库中的密文 -> 校验是否匹配
 */
public class PasswordUtil {
    /**
     * 加密密码
     *
     * @param password 用户输入的明文密码
     * @return BCrypt 加密后的密码
     */
    public static String generatePassword(String password){
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * 校验密码
     *
     * 登录时使用。
     *
     * @param rawPassword 用户输入的明文密码
     * @param secretPassword 数据库中保存的加密密码
     */
    public static Boolean matchPassword(String rawPassword, String secretPassword){
        if (rawPassword == null || secretPassword == null) {
            return false;
        }
        return BCrypt.checkpw(rawPassword, secretPassword);
    }
}
