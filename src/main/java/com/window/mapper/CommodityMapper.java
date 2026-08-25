package com.window.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.window.entity.Commodity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommodityMapper extends BaseMapper<Commodity> {

    @Select("SELECT * FROM commodity WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Commodity selectByIdForUpdate(@Param("id") Integer id);

    @Update("UPDATE commodity SET deleted = 0 WHERE id = #{id} AND deleted = 1")
    int restoreById(@Param("id") Integer id);

    @Update("UPDATE commodity SET current_qty = current_qty + #{qty} WHERE id = #{id}")
    int addQuantity(@Param("id") Integer id, @Param("qty") java.math.BigDecimal qty);

    @Update("UPDATE commodity SET current_qty = CASE WHEN current_qty - #{qty} > 0 THEN current_qty - #{qty} ELSE 0 END WHERE id = #{id}")
    int subtractQuantity(@Param("id") Integer id, @Param("qty") java.math.BigDecimal qty);

    // 含软删除行计数：唯一索引 code 覆盖所有行，删除行仍占用编码
    @Select("SELECT COUNT(*) FROM commodity WHERE code = #{code}")
    int countByCode(@Param("code") String code);
}
