package com.study.libraryManagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.libraryManagement.dto.UserLoginDTO;
import com.study.libraryManagement.entity.User;
import com.study.libraryManagement.mapper.UserMapper;
import com.study.libraryManagement.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public List<User> getAllUsers() {
        return baseMapper.selectList(null);
    }

    /**
     * 用户登录
     *
     * 当前阶段：
     * 根据用户名和密码查询用户。
     *
     * 注意：
     * 1. 当前密码是明文匹配，只用于前期测试。
     * 2. 后续应改为密码加密校验。
     * 3. username 应在数据库中设置唯一索引，否则 selectOne 查询到多条数据可能报错。
     */
    @Override
    public User login(UserLoginDTO loginDTO) {
        QueryWrapper<User> wrapper = new QueryWrapper<User>();
        wrapper.eq("username", loginDTO.getUsername());
        wrapper.eq("password", loginDTO.getPassword());
        return baseMapper.selectOne(wrapper);
    }


}
