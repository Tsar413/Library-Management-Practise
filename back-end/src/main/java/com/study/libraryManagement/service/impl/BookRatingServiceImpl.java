package com.study.libraryManagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.libraryManagement.dto.BookRatingDTO;
import com.study.libraryManagement.entity.Book;
import com.study.libraryManagement.entity.BookRating;
import com.study.libraryManagement.mapper.BookMapper;
import com.study.libraryManagement.mapper.BookRatingMapper;
import com.study.libraryManagement.service.BookRatingService;
import com.study.libraryManagement.util.ParamsUtil;
import com.study.libraryManagement.vo.BookRatingVO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 图书评分业务实现类
 *
 * 当前提供：
 * 1. 新增评分
 * 2. 修改已有评分
 * 3. 查询图书平均分和评分人数
 * 4. 查询当前用户自己的评分
 */
@Service
public class BookRatingServiceImpl extends ServiceImpl<BookRatingMapper, BookRating> implements BookRatingService {
    /**
     * 图书数据访问对象
     */
    @Resource
    private BookMapper bookMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 新增或修改评分
     *
     * 处理规则：
     * 1. 用户未评分过，新增评分
     * 2. 用户已经评分过，修改原评分
     *
     * @param bookRatingDTO 评分请求数据
     * @param userId 当前登录用户ID
     * @return 处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrUpdateRating(BookRatingDTO bookRatingDTO, Long userId) {
        // 判断用户是否登录
        if (userId == null) {
            return "用户不存在或未登录";
        }
        // 判断评分数据是否为空
        if (bookRatingDTO == null) {
            return "评分信息不能为空";
        }
        // 判断ISBN是否为空
        if (bookRatingDTO.getIsbn() == null
                || bookRatingDTO.getIsbn().trim().isEmpty()) {
            return "ISBN不能为空";
        }
        // 判断评分是否为空
        if (bookRatingDTO.getScore() == null) {
            return "评分不能为空";
        }
        // 评分只能在1～5分之间
        if (bookRatingDTO.getScore() < 1
                || bookRatingDTO.getScore() > 5) {
            return "评分必须在1到5分之间";
        }
        // 根据ISBN查询图书
        QueryWrapper<Book> bookWrapper = new QueryWrapper<>();
        bookWrapper.eq("isbn", bookRatingDTO.getIsbn().trim());
        Book book = bookMapper.selectOne(bookWrapper);
        if (book == null) {
            return "图书不存在";
        }
        if (!Integer.valueOf(1).equals(book.getStatus())) {
            return "图书已下架，不能评分";
        }
        /*
         * 查询当前用户是否已经对该图书评分。
         */
        QueryWrapper<BookRating> ratingWrapper = new QueryWrapper<>();
        ratingWrapper.eq("user_id", userId);
        ratingWrapper.eq("book_id", book.getBookId());
        BookRating oldRating = baseMapper.selectOne(ratingWrapper);
        LocalDateTime now = LocalDateTime.now();
        /*
         * 用户还没有评分时，新增评分。
         */
        if (oldRating == null) {
            BookRating bookRating = new BookRating();
            bookRating.setUserId(userId);
            bookRating.setBookId(book.getBookId());
            bookRating.setScore(bookRatingDTO.getScore());
            bookRating.setCreateTime(now);
            bookRating.setUpdateTime(now);
            int insert = baseMapper.insert(bookRating);
            if (insert != 1) {
                throw new RuntimeException("评分保存失败");
            }
            try {
                String cacheKey = ParamsUtil.BOOK_RATING_CACHE_PREFIX + book.getIsbn().trim();
                stringRedisTemplate.delete(cacheKey);
            } catch (Exception e) {
                System.err.println("删除图书评分缓存失败：" + e.getMessage());
            }
            return "评分成功";
        }
        /*
         * 用户已经评分过时，
         * 修改原来的评分记录。
         */
        UpdateWrapper<BookRating> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("rating_id", oldRating.getRatingId());
        // 再次限制必须属于当前用户
        updateWrapper.eq("user_id", userId);
        updateWrapper.set("score", bookRatingDTO.getScore());
        updateWrapper.set("update_time", now);
        int update = baseMapper.update(null, updateWrapper);
        if (update != 1) {
            throw new RuntimeException("评分修改失败");
        }
        try {
            String cacheKey = ParamsUtil.BOOK_RATING_CACHE_PREFIX + book.getIsbn().trim();
            stringRedisTemplate.delete(cacheKey);
        } catch (Exception e) {
            System.err.println("删除图书评分缓存失败：" + e.getMessage());
        }
        return "评分修改成功";
    }

    /**
     * 查询某本书的平均评分和评分人数
     *
     * 当前先查询该书的全部评分记录，
     * 再在Java中计算平均分。
     *
     * @param isbn 图书ISBN
     * @return 评分统计结果
     */
    @Override
    public BookRatingVO getBookRating(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return null;
        }
        String key = ParamsUtil.BOOK_RATING_CACHE_PREFIX + isbn.trim();
        try {
            String s = stringRedisTemplate.opsForValue().get(key);
            if(s != null && !s.trim().isEmpty()){
                return objectMapper.readValue(s, BookRatingVO.class);
            }
        } catch (Exception e) {
            System.err.println("读取图书评分缓存失败，直接查询数据库：" + e.getMessage());
        }
        // 根据ISBN查询图书
        QueryWrapper<Book> bookWrapper = new QueryWrapper<>();
        bookWrapper.eq("isbn", isbn.trim());
        Book book = bookMapper.selectOne(bookWrapper);
        if (book == null) {
            return null;
        }
        // 查询该图书的全部评分
        QueryWrapper<BookRating> ratingWrapper = new QueryWrapper<>();
        ratingWrapper.eq("book_id", book.getBookId());
        List<BookRating> ratings = baseMapper.selectList(ratingWrapper);
        BookRatingVO ratingVO = new BookRatingVO();
        ratingVO.setBookId(book.getBookId());
        ratingVO.setIsbn(book.getIsbn());
        ratingVO.setRatingCount(ratings.size());
        /*
         * 没有人评分时，
         * 平均分设置为0。
         */
        if (ratings.isEmpty()) {
            ratingVO.setAverageScore(BigDecimal.ZERO);
            try {
                String json = objectMapper.writeValueAsString(ratingVO);
                stringRedisTemplate.opsForValue().set(key, json, ParamsUtil.BOOK_RATING_CACHE_MINUTES, TimeUnit.MINUTES);
            } catch (Exception e) {
                System.err.println("写入图书评分缓存失败：" + e.getMessage());
            }
            return ratingVO;
        }
        // 计算总分
        int totalScore = 0;
        for (BookRating rating : ratings) {
            totalScore += rating.getScore();
        }
        /*
         * 计算平均分，保留1位小数。
         *
         * 例如：
         * 4.3333 -> 4.3
         */
        BigDecimal averageScore = BigDecimal.valueOf(totalScore).divide(BigDecimal.valueOf(ratings.size()), 1, RoundingMode.HALF_UP);
        ratingVO.setAverageScore(averageScore);
        try {
            String json = objectMapper.writeValueAsString(ratingVO);
            stringRedisTemplate.opsForValue().set(key, json, ParamsUtil.BOOK_RATING_CACHE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            System.err.println("写入图书评分缓存失败：" + e.getMessage());
        }
        return ratingVO;
    }

    /**
     * 查询当前用户对某本书的评分
     *
     * @param isbn 图书ISBN
     * @param userId 当前登录用户ID
     * @return 当前用户的评分记录
     */
    @Override
    public BookRating getMyRating(String isbn, Long userId) {
        if (userId == null || isbn == null || isbn.trim().isEmpty()) {
            return null;
        }
        // 根据ISBN查询图书
        QueryWrapper<Book> bookWrapper = new QueryWrapper<>();
        bookWrapper.eq("isbn", isbn.trim());
        Book book = bookMapper.selectOne(bookWrapper);
        if (book == null) {
            return null;
        }
        QueryWrapper<BookRating> ratingWrapper = new QueryWrapper<>();
        ratingWrapper.eq("user_id", userId);
        ratingWrapper.eq("book_id", book.getBookId());
        return baseMapper.selectOne(ratingWrapper);
    }
}