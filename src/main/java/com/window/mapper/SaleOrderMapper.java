package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.SaleOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface SaleOrderMapper extends BaseMapper<SaleOrder> {

    @Update("UPDATE sale_order SET paid_amount = #{paidAmount}, is_cleared = #{isCleared}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updatePaidAmount(@Param("id") Integer id, @Param("paidAmount") BigDecimal paidAmount, @Param("isCleared") Integer isCleared);

    @Update("UPDATE sale_order SET status = #{status}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") String status);

    @Update("UPDATE sale_order SET is_cleared = #{isCleared}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateCleared(@Param("id") Integer id, @Param("isCleared") Integer isCleared);

    @Select("<script>" +
            "SELECT o.id, o.order_no AS orderNo, o.customer_name AS customerName, o.customer_phone AS customerPhone, " +
            "o.order_date AS orderDate, o.total_amount AS totalAmount, o.paid_amount AS paidAmount, " +
            "(o.total_amount - o.paid_amount) AS unpaidAmount, " +
            "DATEDIFF('DAY', o.order_date, CURRENT_DATE) AS agingDays, " +
            "CASE " +
            "  WHEN DATEDIFF('DAY', o.order_date, CURRENT_DATE) &lt;= 30 THEN '0-30天' " +
            "  WHEN DATEDIFF('DAY', o.order_date, CURRENT_DATE) &lt;= 60 THEN '30-60天' " +
            "  WHEN DATEDIFF('DAY', o.order_date, CURRENT_DATE) &lt;= 90 THEN '60-90天' " +
            "  ELSE '90天以上' " +
            "END AS bucket " +
            "FROM sale_order o " +
            "WHERE o.deleted = 0 " +
            "AND (o.order_type = 'sale' OR o.order_type IS NULL) " +
            "AND o.status != 'cancelled' " +
            "AND o.status != 'completed' " +
            "AND o.total_amount &gt; 0 " +
            "AND (o.total_amount - o.paid_amount) &gt; 0 " +
            "<if test='startDate != null and startDate != \"\"'> AND o.order_date &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'> AND o.order_date &lt;= #{endDate}</if>" +
            "ORDER BY agingDays DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<Map<String, Object>> selectReceivableAgingDetails(@Param("startDate") String startDate,
                                                            @Param("endDate") String endDate,
                                                            @Param("offset") int offset,
                                                            @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM sale_order o " +
            "WHERE o.deleted = 0 " +
            "AND (o.order_type = 'sale' OR o.order_type IS NULL) " +
            "AND o.status != 'cancelled' " +
            "AND o.status != 'completed' " +
            "AND o.total_amount &gt; 0 " +
            "AND (o.total_amount - o.paid_amount) &gt; 0 " +
            "<if test='startDate != null and startDate != \"\"'> AND o.order_date &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'> AND o.order_date &lt;= #{endDate}</if>" +
            "</script>")
    int countReceivableAgingDetails(@Param("startDate") String startDate,
                                     @Param("endDate") String endDate);

    @Update("UPDATE sale_order SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    // 含软删除行计数：唯一索引 order_no 覆盖所有行，删除行仍占用订单号
    @Select("SELECT COUNT(*) FROM sale_order WHERE order_no = #{orderNo}")
    int countByOrderNo(@Param("orderNo") String orderNo);

    @Select("<script>" +
            "SELECT status, COUNT(*) AS cnt FROM sale_order WHERE deleted = 0 AND (order_type = 'sale' OR order_type IS NULL) AND status != 'cancelled' " +
            "<if test='startDate != null and startDate != \"\"'> AND order_date &gt;= #{startDate}</if>" +
            "<if test='endDate != null and endDate != \"\"'> AND order_date &lt;= #{endDate}</if>" +
            "GROUP BY status" +
            "</script>")
    List<Map<String, Object>> countByStatus(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT YEAR(order_date) AS yr, MONTH(order_date) AS mo, " +
            "SUM(total_amount) AS sales, COUNT(*) AS cnt " +
            "FROM sale_order WHERE deleted = 0 AND (order_type = 'sale' OR order_type IS NULL) AND status != 'cancelled' " +
            "AND order_date &gt;= #{startDate} AND order_date &lt;= #{endDate} " +
            "GROUP BY yr, mo ORDER BY yr, mo" +
            "</script>")
    List<Map<String, Object>> monthlyTrend(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
