package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.PurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {
    @Select("SELECT * FROM purchase_order_item WHERE purchase_order_id = #{orderId} AND deleted = 0")
    List<PurchaseOrderItem> selectAllByOrderId(@Param("orderId") Integer orderId);

    @Update("UPDATE purchase_order_item SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    @Update("UPDATE purchase_order_item SET deleted = 0 WHERE purchase_order_id = #{orderId} AND deleted = 1")
    int restoreByOrderId(@Param("orderId") Integer orderId);
}
