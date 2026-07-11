package com.study.libraryManagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.libraryManagement.entity.Book;
import com.study.libraryManagement.entity.BorrowRecord;
import com.study.libraryManagement.mapper.BookMapper;
import com.study.libraryManagement.mapper.BorrowRecordMapper;
import com.study.libraryManagement.service.BorrowRecordService;
import com.study.libraryManagement.util.ParamsUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@Service
public class BorrowServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowRecordService {

    /**
     * 图书数据访问对象
     *
     * 借阅过程中需要查询图书信息，
     * 并更新图书的 borrowed_count 字段。
     */
    @Resource
    private BookMapper bookMapper;

    /**
     * 借阅图书
     *
     * 处理流程：
     * 1. 判断用户是否已经登录
     * 2. 判断 ISBN 是否有效
     * 3. 根据 ISBN 查询图书
     * 4. 判断用户当前借阅数量是否达到上限
     * 5. 判断用户是否已经借阅该图书
     * 6. 原子更新图书已借数量
     * 7. 新增借阅记录
     *
     * @Transactional：
     * 更新图书数量和新增借阅记录属于同一个事务。
     * 如果保存借阅记录时发生异常，
     * 前面 borrowed_count + 1 的操作也会回滚。
     *
     * @param isbn   图书 ISBN
     * @param userId 当前登录用户 ID，由拦截器解析 token 后传入
     * @return 借阅处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String borrowBook(String isbn, Long userId) {
        // 1. 判断用户是否已经登录
        if(userId == null){
            return "用户不存在或未登录";
        }
        // 2. 判断 ISBN 是否为空
        if (isbn == null || isbn.trim().isEmpty()) {
            return "ISBN不能为空";
        }
        // 3. 根据 ISBN 查询图书
        QueryWrapper<Book> wrapper1 = new QueryWrapper<Book>();
        wrapper1.eq("isbn", isbn);
        Book book = bookMapper.selectOne(wrapper1);
        // 图书不存在，结束借阅
        if(book == null){
            return "图书不存在";
        }
        // 4. 查询用户当前正在借阅的图书数量
        // queryBorrowCount 应只统计 status = 1 的借阅记录
        Integer i = baseMapper.queryBorrowCount(userId);
        // 达到最大借阅数量，不允许继续借阅
        if(i != null && i >= ParamsUtil.BORROW_MAX_COUNT){
            return "超出借阅数量";
        }
        // 5. 查询用户是否已经借阅当前图书且尚未归还
        QueryWrapper<BorrowRecord> recordWrapper = new QueryWrapper<>();
        recordWrapper.eq("user_id", userId);
        recordWrapper.eq("book_id", book.getBookId());
        recordWrapper.eq("status", 1);
        Integer sameBookCount = baseMapper.selectCount(recordWrapper);
        if (sameBookCount != null && sameBookCount > 0) {
            return "您已经借阅了这本图书";
        }
        /*
         * 6. 原子更新图书已借数量
         *
         * 更新条件：
         * 1. book_id 必须匹配
         * 2. 图书状态必须为 1
         * 3. borrowed_count 必须小于 stock
         *
         * 更新内容：
         * borrowed_count = borrowed_count + 1
         *
         * 使用数据库条件更新，可以降低并发情况下超借的风险。
         */
        UpdateWrapper<Book> wrapper2 = new UpdateWrapper<Book>();
        wrapper2.eq("book_id", book.getBookId());
        wrapper2.eq("status", 1);
        wrapper2.apply("borrowed_count < stock");
        wrapper2.setSql("borrowed_count = borrowed_count + 1");
        wrapper2.set("update_time", LocalDateTime.now());
        int updateRows = bookMapper.update(null, wrapper2);
        /*
         * 如果没有更新到一行数据，可能是：
         * 1. 图书已下架
         * 2. 图书库存已经不足
         * 3. 并发情况下最后一本已被其他用户借走
         */
        if (updateRows != 1) {
            return "图书库存不足，借阅失败";
        }
        // 7. 创建借阅记录
        BorrowRecord borrowRecord = new BorrowRecord();
        // 设置图书和用户
        borrowRecord.setBookId(book.getBookId());
        borrowRecord.setUserId(userId);

        // 状态 1 表示借阅中
        borrowRecord.setStatus(1);
        // 设置借阅时间
        borrowRecord.setBorrowTime(LocalDateTime.now());
        // 当前借阅期限暂定为 30 天
        borrowRecord.setDueTime(LocalDateTime.now().plusDays(30));
        // 尚未归还，因此归还时间为空
        borrowRecord.setReturnTime(null);
        // 设置记录创建和更新时间
        borrowRecord.setCreateTime(LocalDateTime.now());
        borrowRecord.setUpdateTime(LocalDateTime.now());

        /*
         * 插入失败时抛出异常。
         *
         * 不能只返回“借阅失败”，否则事务会认为方法正常结束，
         * 前面已经增加的 borrowed_count 不会回滚。
         */
        int insertRows = baseMapper.insert(borrowRecord);

        if (insertRows != 1) {
            throw new RuntimeException("借阅记录保存失败");
        }
        return "借阅成功";
    }
}
