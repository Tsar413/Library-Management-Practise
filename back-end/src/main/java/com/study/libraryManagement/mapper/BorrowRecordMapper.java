package com.study.libraryManagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.libraryManagement.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {
    @Select("select count(*) from tb_borrow_record where user_id = #{userId} and status in (1, 3)")
    public Integer queryBorrowCount(Long userId);
}
