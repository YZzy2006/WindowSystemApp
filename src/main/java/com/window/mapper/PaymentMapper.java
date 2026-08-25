package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Map;

@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    @Select("SELECT * FROM payment WHERE order_id = #{orderId} AND deleted = 0")
    java.util.List<Payment> selectAllByOrderId(@Param("orderId") Integer orderId);

    @Select("SELECT * FROM payment WHERE id = #{id}")
    Payment selectByIdRaw(@Param("id") Integer id);

    @Select("<script>" +
            "SELECT COALESCE(SUM(CASE WHEN type='receipt' THEN amount ELSE 0 END),0) AS totalReceipt, " +
            "COALESCE(SUM(CASE WHEN type='payment' THEN amount ELSE 0 END),0) AS totalPayment " +
            "FROM payment WHERE deleted = 0 AND payment_date BETWEEN #{startDate} AND #{endDate}" +
            "<if test='type != null and type != \"\"'> AND type = #{type}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (order_no LIKE CONCAT('%',#{keyword},'%') OR party_name LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "</script>")
    Map<String, Object> selectSummary(@Param("startDate") String startDate, @Param("endDate") String endDate,
                                      @Param("type") String type, @Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT COALESCE(SUM(CASE WHEN type='receipt' THEN amount ELSE 0 END),0) AS totalReceipt, " +
            "COALESCE(SUM(CASE WHEN type='payment' THEN amount ELSE 0 END),0) AS totalPayment " +
            "FROM payment WHERE deleted = 0 AND payment_date &lt; #{beforeDate}" +
            "<if test='type != null and type != \"\"'> AND type = #{type}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (order_no LIKE CONCAT('%',#{keyword},'%') OR party_name LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "</script>")
    Map<String, Object> selectSummaryBefore(@Param("beforeDate") String beforeDate,
                                            @Param("type") String type, @Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT COALESCE(SUM(CASE WHEN type='receipt' THEN amount ELSE 0 END),0) AS totalReceipt, " +
            "COALESCE(SUM(CASE WHEN type='payment' THEN amount ELSE 0 END),0) AS totalPayment " +
            "FROM payment WHERE deleted = 0" +
            "<if test='type != null and type != \"\"'> AND type = #{type}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (order_no LIKE CONCAT('%',#{keyword},'%') OR party_name LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "</script>")
    Map<String, Object> selectAllSummary(@Param("type") String type, @Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT COALESCE(SUM(CASE WHEN type='receipt' THEN amount ELSE 0 END),0) AS totalReceipt, " +
            "COALESCE(SUM(CASE WHEN type='payment' THEN amount ELSE 0 END),0) AS totalPayment " +
            "FROM payment WHERE deleted = 0 AND payment_date &gt;= #{startDate}" +
            "<if test='type != null and type != \"\"'> AND type = #{type}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (order_no LIKE CONCAT('%',#{keyword},'%') OR party_name LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "</script>")
    Map<String, Object> selectSummaryFrom(@Param("startDate") String startDate,
                                          @Param("type") String type, @Param("keyword") String keyword);

    @Select("<script>" +
            "SELECT COALESCE(SUM(CASE WHEN type='receipt' THEN amount ELSE 0 END),0) AS totalReceipt, " +
            "COALESCE(SUM(CASE WHEN type='payment' THEN amount ELSE 0 END),0) AS totalPayment " +
            "FROM payment WHERE deleted = 0 AND payment_date &lt;= #{endDate}" +
            "<if test='type != null and type != \"\"'> AND type = #{type}</if>" +
            "<if test='keyword != null and keyword != \"\"'> AND (order_no LIKE CONCAT('%',#{keyword},'%') OR party_name LIKE CONCAT('%',#{keyword},'%'))</if>" +
            "</script>")
    Map<String, Object> selectSummaryUntil(@Param("endDate") String endDate,
                                           @Param("type") String type, @Param("keyword") String keyword);

    @Update("UPDATE payment SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    @Update("UPDATE payment SET deleted = 0 WHERE order_id = #{orderId} AND type = #{type} AND deleted = 1")
    int restoreByOrderIdAndType(@Param("orderId") Integer orderId, @Param("type") String type);
}
