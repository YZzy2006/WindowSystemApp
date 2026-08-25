package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.StockInItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.time.LocalDate;

@Mapper
public interface StockInItemMapper extends BaseMapper<StockInItem> {
    @Select("SELECT * FROM stock_in_item WHERE stock_in_id = #{stockInId} AND deleted = 0")
    java.util.List<StockInItem> selectAllByStockInId(@Param("stockInId") Integer stockInId);
    @Select("SELECT COALESCE(SUM(sii.quantity), 0) FROM stock_in_item sii " +
            "INNER JOIN stock_in si ON sii.stock_in_id = si.id " +
            "WHERE si.deleted = 0 AND sii.deleted = 0 " +
            "AND sii.commodity_id = #{commodityId} " +
            "AND (#{startDate} IS NULL OR si.order_date >= #{startDate}) " +
            "AND (#{endDate} IS NULL OR si.order_date <= #{endDate})")
    BigDecimal sumQuantityByCommodityAndDateRange(@Param("commodityId") Integer commodityId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    @Update("UPDATE stock_in_item SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    @Update("UPDATE stock_in_item SET deleted = 0 WHERE stock_in_id = #{stockInId} AND deleted = 1")
    int restoreByStockInId(@Param("stockInId") Integer stockInId);
}
