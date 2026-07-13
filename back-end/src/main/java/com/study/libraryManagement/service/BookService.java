package com.study.libraryManagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.libraryManagement.dto.BookDTO;
import com.study.libraryManagement.entity.Book;

import java.util.List;

public interface BookService extends IService<Book> {
    List<Book> getAllBooks();

    List<Book> searchBooks(String keyword);

    String addBook(BookDTO bookDTO);

    String updateBook(BookDTO bookDTO);

    String updateStatus(String isbn);
}
