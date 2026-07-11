package com.study.libraryManagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.libraryManagement.entity.Book;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

}
