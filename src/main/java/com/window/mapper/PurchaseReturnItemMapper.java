package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.PurchaseReturnItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface PurchaseReturnItemMapper extends BaseMapper<PurchaseReturnItem> {
    @Select("SELECT * FROM purchase_return_item WHERE return_id = #{returnId} AND deleted = 0")
    List<PurchaseReturnItem> selectAllByReturnId(@Param("returnId") Integer returnId);

    @Update("UPDATE purchase_return_item SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    @Update("UPDATE purchase_return_item SET deleted = 0 WHERE return_id = #{returnId} AND deleted = 1")
    int restoreByReturnId(@Param("returnId") Integer returnId);
}
