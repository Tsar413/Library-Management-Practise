package com.study.libraryManagement.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 工具类
 *
 * 当前阶段：
 * 使用内存 Map 保存 token 与 userId 的关系。
 *
 * 后续接入 Redis 后：
 * 可以把 tokenMap 替换成 Redis 操作。
 */

public class TokenUtil {
    /**
     * 临时保存 token 和 userId 的对应关系
     *
     * key   token
     * value userId
     */
    private static final Map<String, Long> map = new ConcurrentHashMap<String, Long>();

    public static String createToken(Long id){
        String token = UUID.randomUUID().toString();
        map.put(token, id);
        return token;
    }

    /**
     * 根据 token 获取 userId
     *
     * 如果返回 null，说明 token 不存在或无效。
     */
    public static Long getUserIdByToken(String token){
        return map.get(token);
    }


}
