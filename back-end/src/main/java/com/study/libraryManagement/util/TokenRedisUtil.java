package com.study.libraryManagement.util;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Token 管理工具类
 *
 * 设计目标：
 * 1. Redis正常时，Token保存到Redis并自动过期
 * 2. Redis无法连接时，自动使用本地内存缓存
 * 3. Redis故障不能导致整个项目无法登录和访问
 *
 * 注意：
 * 本地缓存只是降级方案。
 * 项目重启后，本地缓存中的Token会消失。
 */
@Component
public class TokenRedisUtil {

    /**
     * Redis Token键前缀
     *
     * 实际Redis键示例：
     * login:token:550e8400-e29b-41d4-a716-446655440000
     */
    private static final String TOKEN_PREFIX = "login:token:";

    /**
     * Token有效时间
     *
     * 当前设置为2小时。
     */
    private static final long TOKEN_EXPIRE_MINUTES = 120L;

    /**
     * Redis操作对象
     */
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 本地备用Token缓存
     *
     * key：token
     * value：本地Token信息
     */
    private final Map<String, LocalTokenInfo> localTokenMap = new ConcurrentHashMap<String, LocalTokenInfo>();

    /**
     * 创建Token
     *
     * 无论Redis是否可用，都会先保存到本地缓存。
     * Redis可用时，再同步写入Redis。
     *
     * @param userId 用户ID
     * @return Token
     */
    public String createToken(Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        String token = UUID.randomUUID().toString();

        LocalDateTime expireTime =
                LocalDateTime.now()
                        .plusMinutes(TOKEN_EXPIRE_MINUTES);

        /*
         * 先写本地备用缓存。
         *
         * 即使后面的Redis连接失败，
         * 当前登录仍然可以正常使用。
         */
        localTokenMap.put(
                token,
                new LocalTokenInfo(userId, expireTime)
        );

        /*
         * 再尝试写入Redis。
         *
         * Redis异常只能记录，
         * 不能影响正常登录。
         */
        try {
            stringRedisTemplate.opsForValue().set(
                    getRedisKey(token),
                    String.valueOf(userId),
                    TOKEN_EXPIRE_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            System.err.println(
                    "Redis连接失败，Token暂时保存到本地内存：" +
                            e.getMessage()
            );
        }

        return token;
    }

    /**
     * 根据Token获取用户ID
     *
     * 处理顺序：
     * 1. 优先查询Redis
     * 2. Redis连接失败时，查询本地缓存
     * 3. Redis正常但Token不存在时，说明Token已过期或无效
     *
     * @param token Token
     * @return 用户ID；无效时返回null
     */
    public Long getUserIdByToken(String token) {

        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        String cleanToken = token.trim();

        try {
            String userIdValue =
                    stringRedisTemplate
                            .opsForValue()
                            .get(getRedisKey(cleanToken));

            /*
             * Redis正常响应，但没有查到Token。
             *
             * 说明Token已过期、已退出或原本就不存在。
             *
             * 此时必须同时删除本地副本，
             * 否则Redis已经过期，本地缓存却仍会让Token继续有效。
             */
            if (userIdValue == null) {
                localTokenMap.remove(cleanToken);
                return null;
            }

            Long userId = Long.valueOf(userIdValue);

            /*
             * Redis中存在Token时，也同步更新本地备用缓存。
             */
            refreshLocalToken(cleanToken, userId);

            return userId;

        } catch (NumberFormatException e) {

            /*
             * Redis中的userId格式错误，删除异常数据。
             */
            deleteToken(cleanToken);
            return null;

        } catch (Exception e) {

            /*
             * Redis连接失败时，才允许使用本地备用缓存。
             */
            System.err.println(
                    "Redis连接失败，使用本地Token缓存：" +
                            e.getMessage()
            );

            return getUserIdFromLocal(cleanToken);
        }
    }

    /**
     * 刷新Token有效时间
     *
     * 实现滑动过期：
     * 用户持续操作时，Token继续保持有效；
     * 长时间不访问后，Token自动失效。
     *
     * @param token Token
     */
    public void refreshToken(String token) {

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        String cleanToken = token.trim();

        LocalTokenInfo localTokenInfo =
                localTokenMap.get(cleanToken);

        if (localTokenInfo != null) {
            localTokenInfo.setExpireTime(
                    LocalDateTime.now()
                            .plusMinutes(TOKEN_EXPIRE_MINUTES)
            );
        }

        try {
            stringRedisTemplate.expire(
                    getRedisKey(cleanToken),
                    TOKEN_EXPIRE_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            System.err.println(
                    "Redis连接失败，仅刷新本地Token有效期：" +
                            e.getMessage()
            );
        }
    }

    /**
     * 删除Token
     *
     * 用于退出登录。
     *
     * @param token Token
     */
    public void deleteToken(String token) {

        if (token == null || token.trim().isEmpty()) {
            return;
        }

        String cleanToken = token.trim();

        // 删除本地缓存
        localTokenMap.remove(cleanToken);

        try {
            // 删除Redis缓存
            stringRedisTemplate.delete(
                    getRedisKey(cleanToken)
            );
        } catch (Exception e) {
            System.err.println(
                    "Redis连接失败，本地Token已删除：" +
                            e.getMessage()
            );
        }
    }

    /**
     * 从本地备用缓存中读取用户ID
     */
    private Long getUserIdFromLocal(String token) {

        LocalTokenInfo tokenInfo =
                localTokenMap.get(token);

        if (tokenInfo == null) {
            return null;
        }

        /*
         * 判断本地Token是否已过期。
         */
        if (LocalDateTime.now()
                .isAfter(tokenInfo.getExpireTime())) {

            localTokenMap.remove(token);
            return null;
        }

        return tokenInfo.getUserId();
    }

    /**
     * 更新本地备用Token。
     */
    private void refreshLocalToken(
            String token,
            Long userId) {

        localTokenMap.put(
                token,
                new LocalTokenInfo(
                        userId,
                        LocalDateTime.now()
                                .plusMinutes(TOKEN_EXPIRE_MINUTES)
                )
        );
    }

    /**
     * 拼接Redis键。
     */
    private String getRedisKey(String token) {
        return TOKEN_PREFIX + token;
    }

    /**
     * 本地Token信息。
     */
    private static class LocalTokenInfo {

        private Long userId;

        private LocalDateTime expireTime;

        public LocalTokenInfo(
                Long userId,
                LocalDateTime expireTime) {

            this.userId = userId;
            this.expireTime = expireTime;
        }

        public Long getUserId() {
            return userId;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }

        public void setExpireTime(LocalDateTime expireTime) {
            this.expireTime = expireTime;
        }
    }
}