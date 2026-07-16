package com.study.libraryManagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.study.libraryManagement.dto.BookReviewDTO;
import com.study.libraryManagement.entity.Book;
import com.study.libraryManagement.entity.BookReview;
import com.study.libraryManagement.entity.User;
import com.study.libraryManagement.mapper.BookMapper;
import com.study.libraryManagement.mapper.BookReviewMapper;
import com.study.libraryManagement.mapper.UserMapper;
import com.study.libraryManagement.service.BookReviewService;
import com.study.libraryManagement.util.ParamsUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 图书评论业务实现类
 * 继承 ServiceImpl 后，可以直接使用 MyBatis-Plus 提供的
 * 新增、查询、修改和删除等基础数据库操作。
 * 当前实现的功能：
 * 1. 查询全部评论
 * 2. 根据图书 ISBN 查询评论
 * 3. 用户发表评论
 * 4. 用户修改自己的评论
 * 5. 用户逻辑删除自己的评论
 * 泛型说明：
 * BookReviewMapper：评论数据访问层
 * BookReview：评论实体类
 */

@Service
public class BookReviewServiceImpl extends ServiceImpl<BookReviewMapper, BookReview> implements BookReviewService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 图书数据访问对象
     * 发表评论或查询评论时，
     * 需要先根据 ISBN 查询图书信息。
     */
    @Resource
    private BookMapper bookMapper;

    @Override
    public List<BookReview> getAllBookReviews() {
        return baseMapper.selectList(null);
    }

    /**
     * 根据 ISBN 查询某一本图书的正常评论
     * 处理流程：
     * 1. 判断 ISBN 是否为空
     * 2. 根据 ISBN 查询图书
     * 3. 获取图书 ID
     * 4. 根据图书 ID 查询评论
     * 5. 只返回 status = 1 的正常评论
     * 6. 按创建时间倒序排列
     *
     * @param isbn 图书 ISBN
     * @return 指定图书的正常评论列表
     */
    @Override
    public List<BookReview> getOneBookReviews(String isbn) {
        // ISBN为空时返回空列表
        if (isbn == null || isbn.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String key = ParamsUtil.BOOK_REVIEW_CACHE_PREFIX + isbn.trim();
        try {
            String s = stringRedisTemplate.opsForValue().get(key);
            if(!ParamsUtil.NULL_CACHE_VALUE.equals(s) && s != null){
                 return objectMapper.readValue(s, new TypeReference<List<BookReview>>() {});
            } else if(ParamsUtil.NULL_CACHE_VALUE.equals(s)){
                return Collections.emptyList();
            }
        } catch (Exception e) {
            /*
             * Redis异常不能影响评论查询，
             * 后续继续查询数据库。
             */
            System.err.println("读取图书评论缓存失败，直接查询数据库：" + e.getMessage());
        }

        // 根据ISBN查询图书
        QueryWrapper<Book> wrapper1 = new QueryWrapper<Book>();
        wrapper1.eq("isbn", isbn.trim());
        Book book = bookMapper.selectOne(wrapper1);
        // 图书不存在时返回空列表
        if(book == null){
            return Collections.emptyList();
        }
        // 根据图书ID查询评论
        QueryWrapper<BookReview> wrapper2 = new QueryWrapper<BookReview>();
        wrapper2.eq("book_id", book.getBookId());
        // 只查询正常评论
        wrapper2.eq("status", 1);
        // 最新评论优先显示
        wrapper2.orderByDesc("create_time");
        List<BookReview> bookReviews = baseMapper.selectList(wrapper2);
        try {
            String json = objectMapper.writeValueAsString(bookReviews);
            stringRedisTemplate.opsForValue().set(key, json, ParamsUtil.BOOK_REVIEW_CACHE_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            /*
             * Redis写入失败不影响数据库结果返回。
             */
            System.err.println("写入图书评论缓存失败：" + e.getMessage());
        }
        return bookReviews;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addBookReview(BookReviewDTO bookReviewDTO, Long userId) {
        if (userId == null) {
            return "用户不存在或未登录";
        }
        if(bookReviewDTO == null){
            return "评论信息不能为空";
        }
        // 判断ISBN是否为空
        if (bookReviewDTO.getIsbn() == null || bookReviewDTO.getIsbn().trim().isEmpty()) {
            return "ISBN不能为空";
        }
        // 判断评论内容是否为空
        if (bookReviewDTO.getContent() == null || bookReviewDTO.getContent().trim().isEmpty()) {
            return "评论内容不能为空";
        }
        String key = ParamsUtil.BOOK_REVIEW_CACHE_PREFIX + bookReviewDTO.getIsbn().trim();
        // 根据ISBN查询图书
        QueryWrapper<Book> wrapper1 = new QueryWrapper<Book>();
        wrapper1.eq("isbn", bookReviewDTO.getIsbn().trim());
        Book book = bookMapper.selectOne(wrapper1);
        // 图书不存在
        if(book == null){
            return "图书不存在";
        }
        LocalDateTime now = LocalDateTime.now();
        // 创建评论实体
        BookReview bookReview = new BookReview();
        bookReview.setBookId(book.getBookId());
        // 状态1表示正常
        bookReview.setStatus(1);
        bookReview.setContent(bookReviewDTO.getContent());
        bookReview.setUserId(userId);
        bookReview.setCreateTime(now);
        bookReview.setUpdateTime(now);
        // 保存评论
        int insert = baseMapper.insert(bookReview);
        if(insert != 1){
            throw new RuntimeException("保存失败");
        }
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            /*
             * Redis写入失败不影响数据库结果返回。
             */
            System.err.println("图书评论缓存删除失败：" + e.getMessage());
        }
        return "评论成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateBookReview(BookReviewDTO bookReviewDTO, Long userId) {
        if (userId == null) {
            return "用户不存在或未登录";
        }
        if(bookReviewDTO == null){
            return "评论信息不能为空";
        }
        // 判断ISBN是否为空
        if (bookReviewDTO.getIsbn() == null || bookReviewDTO.getIsbn().trim().isEmpty()) {
            return "ISBN不能为空";
        }
        // 判断评论内容是否为空
        if (bookReviewDTO.getContent() == null || bookReviewDTO.getContent().trim().isEmpty()) {
            return "评论内容不能为空";
        }
        if (bookReviewDTO.getReviewId() == null) {
            return "评论ID不能为空";
        }
        // 根据ISBN查询图书
        QueryWrapper<Book> wrapper1 = new QueryWrapper<Book>();
        wrapper1.eq("isbn", bookReviewDTO.getIsbn().trim());
        Book book = bookMapper.selectOne(wrapper1);
        // 图书不存在
        if(book == null){
            return "图书不存在";
        }
        QueryWrapper<BookReview> wrapper2 = new QueryWrapper<BookReview>();
        wrapper2.eq("review_id", bookReviewDTO.getReviewId());
        wrapper2.eq("user_id", userId);
        wrapper2.eq("book_id", book.getBookId());
        wrapper2.eq("status", 1);
        BookReview bookReview = baseMapper.selectOne(wrapper2);
        if (bookReview == null) {
            return "评论不存在或无权修改";
        }
        // 构建更新条件
        UpdateWrapper<BookReview> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("review_id", bookReview.getReviewId());
        updateWrapper.eq("user_id", userId);
        updateWrapper.eq("status", 1);
        // 修改评论内容
        updateWrapper.set("content", bookReviewDTO.getContent());
        // 修改更新时间
        updateWrapper.set("update_time", LocalDateTime.now());
        int update = baseMapper.update(null, updateWrapper);
        if (update != 1) {
            throw new RuntimeException("评论修改失败");
        }
        String key = ParamsUtil.BOOK_REVIEW_CACHE_PREFIX + bookReviewDTO.getIsbn().trim();
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            /*
             * Redis写入失败不影响数据库结果返回。
             */
            System.err.println("图书评论缓存删除失败：" + e.getMessage());
        }
        return "修改成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deleteBookReview(BookReviewDTO bookReviewDTO, Long userId) {
        if (userId == null) {
            return "用户不存在或未登录";
        }
        if(bookReviewDTO == null){
            return "评论信息不能为空";
        }
        if (bookReviewDTO.getReviewId() == null) {
            return "评论ID不能为空";
        }
        QueryWrapper<BookReview> wrapper2 = new QueryWrapper<BookReview>();
        wrapper2.eq("review_id", bookReviewDTO.getReviewId());
        wrapper2.eq("user_id", userId);
        wrapper2.eq("status", 1);
        BookReview bookReview = baseMapper.selectOne(wrapper2);
        if (bookReview == null) {
            return "评论不存在或无权修改";
        }
        // 构建更新条件
        UpdateWrapper<BookReview> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("review_id", bookReview.getReviewId());
        updateWrapper.eq("user_id", userId);
        updateWrapper.eq("status", 1);
        // 修改评论内容
        updateWrapper.set("status", 0);
        // 修改更新时间
        updateWrapper.set("update_time", LocalDateTime.now());
        int update = baseMapper.update(null, updateWrapper);
        if (update != 1) {
            throw new RuntimeException("评论删除失败");
        }
        Book book = bookMapper.selectById(bookReview.getBookId());
        if (book != null && book.getIsbn() != null) {
            try {
                String key = ParamsUtil.BOOK_REVIEW_CACHE_PREFIX + book.getIsbn().trim();
                stringRedisTemplate.delete(key);
            } catch (Exception e) {
                /*
                 * Redis写入失败不影响数据库结果返回。
                 */
                System.err.println("图书评论缓存删除失败：" + e.getMessage());
            }
        }
        return "删除成功";
    }

    @Override
    public List<BookReview> getMyBookReviews(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        QueryWrapper<BookReview> wrapper = new QueryWrapper<BookReview>();
        wrapper.eq("user_id", userId);
        wrapper.orderByDesc("create_time");
        return baseMapper.selectList(wrapper);
    }
}
