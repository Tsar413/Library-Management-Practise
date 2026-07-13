package com.study.libraryManagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.libraryManagement.entity.BookRating;
import org.apache.ibatis.annotations.Mapper;

/**
 * 图书评分数据访问层
 */
@Mapper
public interface BookRatingMapper extends BaseMapper<BookRating> {
}