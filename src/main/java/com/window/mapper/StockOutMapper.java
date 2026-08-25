package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.StockOut;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface StockOutMapper extends BaseMapper<StockOut> {

    @Update("UPDATE stock_out SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    // 含软删除行计数：唯一索引 order_no 覆盖所有行，删除行仍占用单号
    @Select("SELECT COUNT(*) FROM stock_out WHERE order_no = #{orderNo}")
    int countByOrderNo(@Param("orderNo") String orderNo);
}
