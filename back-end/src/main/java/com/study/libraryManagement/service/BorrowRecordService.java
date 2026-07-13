package com.study.libraryManagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.libraryManagement.entity.BorrowRecord;

import java.util.List;

public interface BorrowRecordService extends IService<BorrowRecord> {

    String borrowBook(String isbn, Long userId);

    String returnBook(String isbn, Long userId);

    String updateOverdue();

    List<BorrowRecord> getAllLists();

    List<BorrowRecord> getOneLists(String username, Long userId);

    List<BorrowRecord> getOneListsAdmin(String username);

    List<BorrowRecord> getOneListsUser(Long userId);
}
