package com.study.libraryManagement.service;

public interface RedisTestService {
    String sendMessage(String message);

    String receiveMessage();
}
