package com.study.libraryManagement.component;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class BorrowRedisGuard {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用户重复操作锁有效期
     */
    private static final long USER_OPERATION_SECONDS = 5L;

    /**
     * 图书库存锁有效期
     */
    private static final long BOOK_LOCK_SECONDS = 10L;

    public boolean tryBorrowOperationLock(Long userId, String isbn){
        String key = "lock:borrow:" + userId + ":" + isbn;
        try {
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", USER_OPERATION_SECONDS, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            /*
             * Redis故障时不阻断业务。
             *
             * 继续依靠数据库事务和业务校验。
             */
            return true;
        }
    }

    /**
     * 尝试获取用户归还防重复锁
     */
    public boolean tryReturnOperationLock(Long userId, String isbn) {
        String key = "lock:return:" + userId + ":" + isbn;
        try {
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", USER_OPERATION_SECONDS, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 尝试获取图书库存锁
     *
     * 返回锁的唯一值。
     * 获取失败时返回null。
     */
    public String tryBookStockLock(String isbn) {
        String key = "lock:book:stock:" + isbn;
        String lockValue = UUID.randomUUID().toString();
        try {
            Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, lockValue, BOOK_LOCK_SECONDS, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(result)) {
                return lockValue;
            }
            return null;
        } catch (Exception e) {
            /*
             * Redis不可用时返回特殊值，
             * 允许业务降级继续执行。
             */
            return "REDIS_DOWN";
        }
    }

    /**
     * 释放图书库存锁
     */
    public void releaseBookStockLock(String isbn, String lockValue) {
        if (isbn == null || lockValue == null || "REDIS_DOWN".equals(lockValue)) {
            return;
        }
        String key = "lock:book:stock:" + isbn;
        try {
            String currentValue = stringRedisTemplate.opsForValue().get(key);
            /*
             * 只有锁值一致时才能删除，
             * 避免误删其他线程重新获得的锁。
             */
            if (lockValue.equals(currentValue)) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            /*
             * 锁本身有过期时间，
             * Redis异常时不用继续抛出。
             */
        }
    }
}
