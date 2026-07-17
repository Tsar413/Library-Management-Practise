package com.study.libraryManagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.libraryManagement.dto.UserDTO;
import com.study.libraryManagement.entity.User;
import com.study.libraryManagement.mapper.UserMapper;
import com.study.libraryManagement.service.UserService;
import com.study.libraryManagement.util.PasswordUtil;
import com.study.libraryManagement.util.RandomNickname;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    @Override
    public List<User> getAllUsers() {
        return baseMapper.selectList(null);
    }

    /**
     * 用户登录
     *
     * 处理流程：
     * 1. 根据用户名查询用户
     * 2. 如果用户不存在，返回 null
     * 3. 如果用户存在，使用 BCrypt 校验密码
     * 4. 密码正确返回用户对象
     * 5. 密码错误返回 null
     *
     * 注意：
     * BCrypt 每次加密同一个密码，结果都不同。
     * 所以登录时不能把 password 放进 SQL 条件中查询，
     * 必须先查出用户，再使用 BCrypt.checkpw() 校验。
     */
    @Override
    public User login(UserDTO loginDTO) {
        QueryWrapper<User> wrapper = new QueryWrapper<User>();
        wrapper.eq("username", loginDTO.getUsername());
        wrapper.eq("status", 1);
        User user = baseMapper.selectOne(wrapper);
        if(user == null){
            return null;
        }
        if(!PasswordUtil.matchPassword(loginDTO.getPassword(), user.getPassword())){
            return null;
        }
        return user;
    }

    @Override
    public String registration(UserDTO registrationDTO) {
        if (registrationDTO == null) {
            return "注册信息不能为空";
        }
        if (registrationDTO.getUsername() == null || registrationDTO.getUsername().trim().isEmpty()) {
            return "用户名不能为空";
        }
        if (registrationDTO.getPassword() == null || registrationDTO.getPassword().trim().isEmpty()) {
            return "密码不能为空";
        }
        QueryWrapper<User> wrapper = new QueryWrapper<User>();
        wrapper.eq("username", registrationDTO.getUsername());
        User user = baseMapper.selectOne(wrapper);
        if(user != null){
            return "用户名已存在";
        }
        User saveUser = new User();
        saveUser.setUsername(registrationDTO.getUsername());
        saveUser.setPassword(PasswordUtil.generatePassword(registrationDTO.getPassword()));
        saveUser.setRole("USER");
        saveUser.setNickname(RandomNickname.generateRandomNickname());
        saveUser.setStatus(1);
        saveUser.setCreateTime(LocalDateTime.now());
        saveUser.setUpdateTime(LocalDateTime.now());
        try {
            baseMapper.insert(saveUser);
        } catch (Exception e) {
            return "注册失败";
        }
        return "注册成功";
    }
}
