package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.SaleOrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface SaleOrderItemMapper extends BaseMapper<SaleOrderItem> {
    @Select("SELECT * FROM sale_order_item WHERE order_id = #{orderId} AND deleted = 0")
    List<SaleOrderItem> selectAllByOrderId(@Param("orderId") Integer orderId);

    @Update("UPDATE sale_order_item SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    @Update("UPDATE sale_order_item SET deleted = 0 WHERE order_id = #{orderId} AND deleted = 1")
    int restoreByOrderId(@Param("orderId") Integer orderId);

    @Select("<script>" +
            "SELECT YEAR(o.order_date) AS yr, MONTH(o.order_date) AS mo, " +
            "SUM(i.cost * i.quantity) AS cost " +
            "FROM sale_order_item i JOIN sale_order o ON i.order_id = o.id " +
            "WHERE i.deleted = 0 AND o.deleted = 0 AND (o.order_type = 'sale' OR o.order_type IS NULL) AND o.status != 'cancelled' " +
            "AND o.order_date &gt;= #{startDate} AND o.order_date &lt;= #{endDate} " +
            "GROUP BY yr, mo ORDER BY yr, mo" +
            "</script>")
    List<Map<String, Object>> monthlyCost(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
