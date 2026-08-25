package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.PurchaseReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface PurchaseReturnMapper extends BaseMapper<PurchaseReturn> {

    @Update("UPDATE purchase_return SET paid_amount = #{paidAmount}, is_cleared = #{isCleared}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePaidAmount(@Param("id") Integer id, @Param("paidAmount") BigDecimal paidAmount, @Param("isCleared") Integer isCleared);

    @Update("UPDATE purchase_return SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    @Update("UPDATE purchase_return SET is_cleared = #{isCleared}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateCleared(@Param("id") Integer id, @Param("isCleared") Integer isCleared);

    @Update("UPDATE purchase_return SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    // 含软删除行计数：唯一索引 order_no 覆盖所有行，删除行仍占用订单号
    @Select("SELECT COUNT(*) FROM purchase_return WHERE order_no = #{orderNo}")
    int countByOrderNo(@Param("orderNo") String orderNo);
}
