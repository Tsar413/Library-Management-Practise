package com.study.libraryManagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.libraryManagement.entity.BorrowRecord;

public interface BorrowRecordService extends IService<BorrowRecord> {

    String borrowBook(String isbn, Long userId);
}
