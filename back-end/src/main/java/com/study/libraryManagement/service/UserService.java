package com.study.libraryManagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.libraryManagement.dto.UserDTO;
import com.study.libraryManagement.entity.User;

import java.util.List;

public interface UserService extends IService<User> {
    List<User> getAllUsers();

    User login(UserDTO loginDTO);

    String registration(UserDTO registrationDTO);
}
