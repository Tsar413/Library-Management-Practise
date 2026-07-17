package com.study.libraryManagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.libraryManagement.component.BorrowRedisGuard;
import com.study.libraryManagement.entity.Book;
import com.study.libraryManagement.entity.BorrowRecord;
import com.study.libraryManagement.entity.User;
import com.study.libraryManagement.mapper.BookMapper;
import com.study.libraryManagement.mapper.BorrowRecordMapper;
import com.study.libraryManagement.mapper.UserMapper;
import com.study.libraryManagement.service.BorrowRecordService;
import com.study.libraryManagement.util.ParamsUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class BorrowServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowRecordService {

    @Resource
    private BorrowRedisGuard borrowRedisGuard;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserMapper userMapper;

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
        if (!borrowRedisGuard.tryBorrowOperationLock(userId, isbn.trim())) {
            return "操作过于频繁，请勿重复借阅";
        }
        String lockValue = borrowRedisGuard.tryBookStockLock(isbn.trim());
        if (lockValue == null) {
            return "当前图书操作繁忙，请稍后再试";
        }
        try {
            // 3. 根据 ISBN 查询图书
            QueryWrapper<Book> wrapper1 = new QueryWrapper<Book>();
            wrapper1.eq("isbn", isbn.trim());
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
                throw new RuntimeException("图书库存不足，借阅失败");
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
            stringRedisTemplate.delete(ParamsUtil.BOOK_CACHE_PREFIX + isbn.trim());
            return "借阅成功";
        } finally {
            borrowRedisGuard.releaseBookStockLock(isbn.trim(), lockValue);
        }
    }

    /**
     * 根据 ISBN 归还图书
     *
     * 处理流程：
     * 1. 判断用户是否已经登录
     * 2. 判断 ISBN 是否为空
     * 3. 根据 ISBN 查询图书
     * 4. 查询当前用户尚未归还的借阅记录
     * 5. 将借阅记录状态修改为已归还
     * 6. 将图书的 borrowed_count 减 1
     *
     * @Transactional：
     * 修改借阅记录和修改图书已借数量属于同一个事务。
     *
     * 如果其中任意一步出现异常，
     * 两次数据库操作都会自动回滚，
     * 避免出现“借阅记录已归还，但图书数量没有恢复”的问题。
     *
     * @param isbn   要归还图书的 ISBN
     * @param userId 当前登录用户 ID，由拦截器解析 token 后传入
     * @return 归还处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String returnBook(String isbn, Long userId) {
        /*
         * 1. 判断用户是否登录。
         */
        if (userId == null) {
            return "用户不存在或未登录";
        }
        /*
         * 2. 判断 ISBN 是否为空。
         */
        if (isbn == null || isbn.trim().isEmpty()) {
            return "ISBN不能为空";
        }
        /*
         * 后续统一使用去除首尾空格后的 ISBN。
         */
        String cleanIsbn = isbn.trim();
        /*
         * 3. 防止同一个用户在短时间内，
         * 对同一本书重复提交归还请求。
         */
        if (!borrowRedisGuard.tryReturnOperationLock(userId, cleanIsbn)) {
            return "操作过于频繁，请勿重复归还";
        }
        /*
         * 4. 获取图书库存锁。
         *
         * 借阅和归还都会修改 borrowed_count，
         * 因此必须共用同一个图书库存锁。
         */
        String lockValue = borrowRedisGuard.tryBookStockLock(cleanIsbn);
        if (lockValue == null) {
            return "当前图书操作繁忙，请稍后再试";
        }
        try {
            /*
             * 5. 根据 ISBN 查询图书。
             *
             * 不限制 status，
             * 因为即使图书已经下架，
             * 用户仍然应该能够归还。
             */
            QueryWrapper<Book> bookWrapper = new QueryWrapper<Book>();
            bookWrapper.eq("isbn", cleanIsbn);
            Book book = bookMapper.selectOne(bookWrapper);
            if (book == null) {
                return "图书不存在";
            }
            /*
             * 6. 查询当前用户尚未归还的借阅记录。
             *
             * status = 1：借阅中
             * status = 3：已逾期但尚未归还
             */
            QueryWrapper<BorrowRecord> recordWrapper = new QueryWrapper<BorrowRecord>();
            recordWrapper.eq("user_id", userId);
            recordWrapper.eq("book_id", book.getBookId());
            recordWrapper.in("status", 1, 3);
            BorrowRecord borrowRecord = baseMapper.selectOne(recordWrapper);
            if (borrowRecord == null) {
                return "没有未归还的借阅记录";
            }
            LocalDateTime now = LocalDateTime.now();
            /*
             * 7. 更新借阅记录。
             *
             * 更新时再次限制 status 为 1 或 3，
             * 防止并发情况下重复归还。
             */
            UpdateWrapper<BorrowRecord> recordUpdateWrapper = new UpdateWrapper<BorrowRecord>();
            recordUpdateWrapper.eq("record_id", borrowRecord.getRecordId());
            recordUpdateWrapper.eq("user_id", userId);
            recordUpdateWrapper.in("status", 1, 3);
            recordUpdateWrapper.set("status", 2);
            recordUpdateWrapper.set("return_time", now);
            recordUpdateWrapper.set("update_time", now);
            int recordUpdateRows = baseMapper.update(null, recordUpdateWrapper);
            if (recordUpdateRows != 1) {
                throw new RuntimeException("该图书可能已经归还，请勿重复操作");
            }
            /*
             * 8. 图书已借数量减 1。
             *
             * borrowed_count > 0，
             * 防止数量被减成负数。
             *
             * 不限制图书 status，
             * 下架图书依然允许归还。
             */
            UpdateWrapper<Book> bookUpdateWrapper = new UpdateWrapper<Book>();
            bookUpdateWrapper.eq("book_id", book.getBookId());
            bookUpdateWrapper.apply("borrowed_count > 0");
            bookUpdateWrapper.setSql("borrowed_count = borrowed_count - 1");
            bookUpdateWrapper.set("update_time", now);
            int bookUpdateRows = bookMapper.update(null, bookUpdateWrapper);
            /*
             * 修改失败时抛出异常，
             * 前面借阅记录的修改会随事务一起回滚。
             */
            if (bookUpdateRows != 1) {
                throw new RuntimeException("图书借阅数量异常，归还失败");
            }
            /*
             * 9. 删除图书详情缓存。
             *
             * 图书的 borrowed_count 已经变化，
             * Redis 中原来的缓存已经不再准确。
             *
             * Redis 异常不能影响数据库还书业务，
             * 因此这里必须捕获异常。
             */
            try {
                String cacheKey = ParamsUtil.BOOK_CACHE_PREFIX + cleanIsbn;
                stringRedisTemplate.delete(cacheKey);
            } catch (Exception e) {
                System.err.println("删除图书详情缓存失败：" + e.getMessage());
            }
            return "归还成功";
        } finally {
            /*
             * 10. 无论方法正常结束、提前 return，
             * 还是抛出异常，都释放图书库存锁。
             */
            borrowRedisGuard.releaseBookStockLock(cleanIsbn, lockValue);
        }
    }

    /**
     * 扫描并更新逾期未归还的借阅记录
     *
     * 处理规则：
     * 1. 只查询当前仍处于借阅中的记录，即 status = 1
     * 2. 如果 due_time 早于当前时间，说明已经超过应还日期
     * 3. 将这类记录的状态修改为 3，表示逾期未归还
     * 4. 同时更新 update_time
     *
     * 该方法既可以：
     * 1. 由管理员手动调用
     * 2. 在管理员登录成功后自动调用
     *
     * @Transactional：
     * 批量更新过程中如果出现异常，
     * 当前事务会自动回滚。
     *
     * @return 状态更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String updateOverdue() {
        // 统一获取当前时间
        LocalDateTime now = LocalDateTime.now();
        /*
         * 构建批量更新条件
         *
         * status = 1：
         * 只处理当前仍处于借阅中的记录。
         *
         * due_time < now：
         * 应还时间早于当前时间，说明图书已经逾期。
         */
        UpdateWrapper<BorrowRecord> wrapper1 = new UpdateWrapper<BorrowRecord>();
        wrapper1.eq("status", 1);
        /*
         * 状态 3 表示逾期未归还
         */
        wrapper1.set("status", 3);
        wrapper1.lt("due_time", now);
        // 更新记录的最后修改时间
        wrapper1.set("update_time", now);
        /*
         * 执行批量更新
         *
         */
        int update = baseMapper.update(null, wrapper1);
        if(update == 0){
            System.out.println("更新" + update + "数据");
        }
        /*
         * 返回本次实际更新数量，
         * 方便管理员了解本次扫描结果。
         */
        return "更新成功";
    }

    /**
     * 查询全部借阅记录
     *
     * 当前主要提供给管理员使用。
     *
     * 查询 tb_borrow_record 表中的全部数据，
     * 当前没有添加用户、状态或时间等筛选条件。
     *
     * 后续可以继续增加：
     * 1. 按借阅状态查询
     * 2. 按用户名查询
     * 3. 按图书查询
     * 4. 按借阅时间查询
     * 5. 分页查询
     *
     * @return 全部借阅记录
     */
    @Override
    public List<BorrowRecord> getAllLists() {
        /*
         * selectList(null) 表示不设置任何查询条件，
         * 等价于查询 tb_borrow_record 表中的全部记录。
         */
        return baseMapper.selectList(null);
    }

    /**
     * 根据用户名或用户 ID 查询借阅记录
     *
     * 当前方法暂未被 Controller 接口直接调用，
     * 作为通用查询方法暂时保留。
     *
     * 处理逻辑：
     * 1. 如果 username 不为空，则根据 username 查询用户
     * 2. 查询到用户后，使用该用户的 userId
     * 3. 如果 username 为空，则使用方法参数中的 userId
     * 4. 根据最终的 userId 查询借阅记录
     * 5. 按创建时间倒序排列
     *
     * @param username 用户名，可以为空
     * @param userId   用户 ID
     * @return 对应用户的借阅记录
     */
    @Override
    public List<BorrowRecord> getOneLists(String username, Long userId) {
        /*
         * 如果传入了用户名，
         * 优先根据用户名查询用户 ID。
         */
        if(username != null && !username.trim().isEmpty()){
            QueryWrapper<User> wrapper1 = new QueryWrapper<User>();
            wrapper1.eq("username", username);
            User user = userMapper.selectOne(wrapper1);
            // 用户不存在时返回 null
            if(user == null){
                return null;
            }
            // 使用查询到的用户 ID
            userId = user.getUserId();
        }
        /*
         * 根据最终确定的 userId 查询借阅记录。
         */
        QueryWrapper<BorrowRecord> wrapper2 = new QueryWrapper<BorrowRecord>();
        wrapper2.eq("user_id", userId);
        // 最近产生的借阅记录优先显示
        wrapper2.orderByDesc("create_time");
        List<BorrowRecord> borrowRecords = baseMapper.selectList(wrapper2);
        return borrowRecords;
    }

    /**
     * 管理员根据用户名查询指定用户的借阅记录
     *
     * 处理流程：
     * 1. 判断用户名是否为空
     * 2. 根据用户名查询用户信息
     * 3. 获取该用户的 userId
     * 4. 根据 userId 查询借阅记录
     * 5. 按创建时间倒序排列
     *
     * 当前方法主要提供给管理员接口使用。
     * 管理员权限校验后续应放在 Controller、
     * 拦截器或权限管理模块中处理。
     *
     * @param username 要查询的用户名
     * @return 指定用户的借阅记录
     */
    @Override
    public List<BorrowRecord> getOneListsAdmin(String username) {
        // 用户名为空时直接返回空列表
        if(username == null || username.trim().isEmpty()){
            return Collections.emptyList();

        }
        /*
         * 根据用户名查询用户。
         *
         * username 在数据库中应具有唯一性，
         * 因此正常情况下只会查询到一条用户数据。
         */
        QueryWrapper<User> wrapper1 = new QueryWrapper<User>();
        wrapper1.eq("username", username);
        User user = userMapper.selectOne(wrapper1);
        // 用户不存在时返回空列表
        if(user == null){
            return Collections.emptyList();
        }
        // 获取目标用户 ID
        Long userId2 = user.getUserId();
        /*
         * 根据用户 ID 查询借阅记录。
         */
        QueryWrapper<BorrowRecord> wrapper2 = new QueryWrapper<BorrowRecord>();
        wrapper2.eq("user_id", userId2);
        // 最近产生的记录优先显示
        wrapper2.orderByDesc("create_time");
        List<BorrowRecord> borrowRecords = baseMapper.selectList(wrapper2);
        return borrowRecords;
    }

    /**
     * 查询当前登录用户自己的借阅记录
     *
     * userId 由登录拦截器根据 token 获取，
     * 不由前端直接提交。
     *
     * 这样可以避免普通用户通过修改请求参数，
     * 查询其他用户的借阅记录。
     *
     * 查询结果按照创建时间倒序排列，
     * 最近产生的借阅记录优先显示。
     *
     * @param userId 当前登录用户 ID
     * @return 当前用户自己的借阅记录
     */
    @Override
    public List<BorrowRecord> getOneListsUser(Long userId) {
        /*
         * 用户 ID 为空时返回空列表。
         *
         * 正常情况下，无效 token 会先被拦截器处理，
         * 这里作为业务层的兜底判断。
         */
        if (userId == null) {
            return Collections.emptyList();
        }
        // 根据当前用户 ID 查询借阅记录
        QueryWrapper<BorrowRecord> wrapper2 = new QueryWrapper<BorrowRecord>();
        wrapper2.eq("user_id", userId);
        // 最近产生的借阅记录优先显示
        wrapper2.orderByDesc("create_time");
        List<BorrowRecord> borrowRecords = baseMapper.selectList(wrapper2);
        return borrowRecords;
    }
}
