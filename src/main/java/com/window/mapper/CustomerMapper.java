package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    @Update("UPDATE customer SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    // 含软删除行计数：唯一索引 name 覆盖所有行，删除行仍占用名称
    @Select("SELECT COUNT(*) FROM customer WHERE name = #{name}")
    int countByName(@Param("name") String name);
}
