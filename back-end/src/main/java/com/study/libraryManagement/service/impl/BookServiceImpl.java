package com.study.libraryManagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.libraryManagement.entity.Book;
import com.study.libraryManagement.mapper.BookMapper;
import com.study.libraryManagement.service.BookService;
import org.springframework.stereotype.Service;

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
}