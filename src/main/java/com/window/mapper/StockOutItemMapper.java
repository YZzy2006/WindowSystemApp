package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.StockOutItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;

@Mapper
public interface StockOutItemMapper extends BaseMapper<StockOutItem> {
    @Select("SELECT * FROM stock_out_item WHERE stock_out_id = #{stockOutId} AND deleted = 0")
    java.util.List<StockOutItem> selectAllByStockOutId(@Param("stockOutId") Integer stockOutId);
    @Select("SELECT COALESCE(SUM(soi.quantity), 0) FROM stock_out_item soi " +
            "INNER JOIN stock_out so ON soi.stock_out_id = so.id " +
            "WHERE so.deleted = 0 AND soi.deleted = 0 " +
            "AND soi.commodity_id = #{commodityId} " +
            "AND (#{startDate} IS NULL OR so.order_date >= #{startDate}) " +
            "AND (#{endDate} IS NULL OR so.order_date <= #{endDate})")
    BigDecimal sumQuantityByCommodityAndDateRange(@Param("commodityId") Integer commodityId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Update("UPDATE stock_out_item SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    @Update("UPDATE stock_out_item SET deleted = 0 WHERE stock_out_id = #{stockOutId} AND deleted = 1")
    int restoreByStockOutId(@Param("stockOutId") Integer stockOutId);
}
