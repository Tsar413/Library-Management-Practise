package com.study.libraryManagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.libraryManagement.dto.BookDTO;
import com.study.libraryManagement.entity.Book;
import com.study.libraryManagement.mapper.BookMapper;
import com.study.libraryManagement.service.BookService;
import com.study.libraryManagement.util.ParamsUtil;
import com.study.libraryManagement.util.SavePhotosUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图书业务实现类
 *
 * 继承 MyBatis-Plus 提供的 ServiceImpl，
 * 从而获得常用的增删改查能力。
 *
 * 泛型说明：
 * BookMapper：图书数据访问层
 * Book：图书实体类
 */
@Service
public class BookServiceImpl extends ServiceImpl<BookMapper, Book> implements BookService {

    /**
     * 查询所有图书
     *
     * 当前阶段主要用于验证：
     * Controller -> Service -> Mapper -> MySQL
     * 这一整条查询链路是否可以正常工作。
     *
     * selectList(null) 表示不设置任何查询条件，
     * 等价于查询 tb_book 表中的全部数据。
     *
     * 后续可以继续增加：
     * 1. 按图书状态查询
     * 2. 按书名、ISBN、作者搜索
     * 3. 分页查询
     *
     * @return 图书列表
     */
    @Override
    public List<Book> getAllBooks() {
        return baseMapper.selectList(null);
    }

    @Override
    public List<Book> searchBooks(String keyword) {
        /*
         * 如果关键词为空，
         * 当前设计直接返回全部图书。
         *
         * 这样前端搜索框清空后，
         * 可以重新显示完整图书列表。
         */
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllBooks();
        }
        // 去除关键词前后的空格
        keyword = keyword.trim();
        /*
         * 构建查询条件
         *
         * SQL 逻辑类似：
         *
         * WHERE
         * isbn LIKE '%关键词%'
         * OR book_name LIKE '%关键词%'
         * OR author LIKE '%关键词%'
         * OR publisher LIKE '%关键词%'
         * OR location LIKE '%关键词%'
         */
        QueryWrapper<Book> wrapper1 = new QueryWrapper<Book>();
        wrapper1.eq("status", 1);
        wrapper1.like("isbn", keyword)
                .or()
                .like("book_name", keyword)
                .or()
                .like("author", keyword)
                .or()
                .like("publisher", keyword)
                .or()
                .like("location", keyword);
        return baseMapper.selectList(wrapper1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String addBook(BookDTO bookDTO) {
        if (bookDTO.getIsbn() == null || bookDTO.getIsbn().trim().isEmpty()) {
            return "ISBN不能为空";
        }
        if(bookDTO.getIsbn().trim().length() != ParamsUtil.ISBN_LENGTH){
            return "ISBN必须为" + ParamsUtil.ISBN_LENGTH + "位";
        }
        if(bookDTO.getBookName() == null || bookDTO.getBookName().trim().isEmpty()){
            return "书名不能为空";
        }
        if (bookDTO.getStock() == null) {
            return "库存不能为空";
        }
        if (bookDTO.getStock() < 0) {
            return "库存不能小于0";
        }
        // 根据 ISBN 查询图书
        QueryWrapper<Book> wrapper1 = new QueryWrapper<Book>();
        wrapper1.eq("isbn", bookDTO.getIsbn().trim());
        Book book1 = baseMapper.selectOne(wrapper1);
        if(book1 != null){
            return "图书已经存在";
        }
        LocalDateTime now = LocalDateTime.now();
        Book book = new Book();
        book.setIsbn(bookDTO.getIsbn().trim());
        book.setBookName(bookDTO.getBookName());
        book.setStatus(1);
        book.setAuthor(bookDTO.getAuthor());
        book.setPrice(bookDTO.getPrice());
        book.setBorrowedCount(0);
        book.setPublisher(bookDTO.getPublisher());
        book.setStock(bookDTO.getStock());
        book.setLocation(bookDTO.getLocation());
        book.setCreateTime(now);
        book.setUpdateTime(now);
        if(bookDTO.getImage() != null && !bookDTO.getImage().isEmpty()){
            book.setImageUrl(SavePhotosUtil.savePhoto(bookDTO.getImage(), ParamsUtil.SERVER_PATH, ParamsUtil.LOCAL_PATH));
        } else {
            book.setImageUrl(null);
        }
        int count = baseMapper.insert(book);
        if(count != 1){
            throw new RuntimeException("保存失败");
        }
        return "保存成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateBook(BookDTO bookDTO) {
        if (bookDTO.getIsbn() == null || bookDTO.getIsbn().trim().isEmpty()) {
            return "ISBN不能为空";
        }
        if(bookDTO.getIsbn().trim().length() != ParamsUtil.ISBN_LENGTH){
            return "ISBN必须为" + ParamsUtil.ISBN_LENGTH + "位";
        }
        if(bookDTO.getBookName() == null || bookDTO.getBookName().trim().isEmpty()){
            return "书名不能为空";
        }
        if (bookDTO.getStock() == null) {
            return "库存不能为空";
        }
        if (bookDTO.getStock() < 0) {
            return "库存不能小于0";
        }
        QueryWrapper<Book> wrapper1 = new QueryWrapper<Book>();
        wrapper1.eq("isbn", bookDTO.getIsbn().trim());
        Book book1 = baseMapper.selectOne(wrapper1);
        // 更新接口找不到图书时不自动新增
        if(book1 == null){
            return "图书不存在";
        }
        // 总库存不能小于当前已经借出的数量
        if (bookDTO.getStock() < book1.getBorrowedCount()) {
            return "库存不能小于当前已借数量";
        }
        UpdateWrapper<Book> wrapper2 = new UpdateWrapper<Book>();
        // 根据主键更新指定图书
        wrapper2.eq("book_id", book1.getBookId());
        wrapper2.eq("isbn", book1.getIsbn());
        wrapper2.set("author", bookDTO.getAuthor());
        wrapper2.set("book_name", bookDTO.getBookName());
        wrapper2.set("location", bookDTO.getLocation());
        wrapper2.set("price", bookDTO.getPrice());
        wrapper2.set("publisher", bookDTO.getPublisher());
        wrapper2.set("stock", bookDTO.getStock());
        /*
         * 只有上传了新图片时，
         * 才更新 image_url。
         *
         * 没有上传新图片时，
         * 保留数据库中的旧图片地址。
         */
        if (bookDTO.getImage() != null && !bookDTO.getImage().isEmpty()) {
            String imageUrl = SavePhotosUtil.savePhoto(bookDTO.getImage(), ParamsUtil.SERVER_PATH, ParamsUtil.LOCAL_PATH);
            wrapper2.set("image_url", imageUrl);
        }
        wrapper2.set("update_time", LocalDateTime.now());
        int update = baseMapper.update(null, wrapper2);
        if(update != 1){
            throw new RuntimeException("修改失败");
        }
        return "修改成功";
    }

    /**
     * 修改图书上架状态
     *
     * 状态规则：
     * 1：正常上架
     * 0：已经下架
     *
     * 当前采用状态切换方式：
     * 上架图书调用后变为下架，
     * 下架图书调用后恢复上架。
     *
     * 不物理删除数据库记录，
     * 避免破坏历史借阅数据。
     *
     * @param isbn 图书ISBN
     * @return 修改结果
     */
    @Override
    public String updateStatus(String isbn) {
        // 判断ISBN是否为空
        if (isbn == null ||isbn.trim().isEmpty()) {
            return "ISBN不能为空";
        }
        // 根据ISBN查询图书
        QueryWrapper<Book> wrapper1 = new QueryWrapper<Book>();
        wrapper1.eq("isbn", isbn.trim());
        Book book1 = baseMapper.selectOne(wrapper1);
        // 更新接口找不到图书时不自动新增
        if(book1 == null){
            return "图书不存在";
        }
        // 根据主键更新指定图书
        UpdateWrapper<Book> wrapper2 = new UpdateWrapper<Book>();
        wrapper2.eq("book_id", book1.getBookId());
        /*
         * 当前状态为1时改为0，
         * 其他状态改为1。
         */
        wrapper2.set("status", book1.getStatus() == 1 ? 0 : 1);
        // 更新最后修改时间
        wrapper2.set("update_time", LocalDateTime.now());
        int update = baseMapper.update(null, wrapper2);
        if(update != 1){
            throw new RuntimeException("修改失败");
        }
        return "修改成功";
    }
}