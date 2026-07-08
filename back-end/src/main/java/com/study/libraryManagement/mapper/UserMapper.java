package com.study.libraryManagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.libraryManagement.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层
 *
 * 继承 MyBatis-Plus 提供的 BaseMapper，
 * 自动获得 User 表的 CRUD 操作。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
