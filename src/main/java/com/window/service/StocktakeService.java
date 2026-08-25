package com.window.service;

import java.util.List;
import java.util.Map;

public interface StocktakeService {
    /**
     * 获取商品的FIFO库存成本明细
     * @param commodityId 商品ID
     * @return FIFO成本明细列表
     */
    List<Map<String, Object>> getFIFODetail(Integer commodityId);

    /**
     * 获取所有商品的库存盘点汇总
     * @param productType 细目（可选）
     * @param keyword 关键词（可选）
     * @return 盘点汇总列表
     */
    List<Map<String, Object>> getStocktakeSummary(String productType, String keyword);

    /**
     * 计算指定日期的库存快照
     * @param date 日期
     * @param productType 细目（可选）
     * @param keyword 关键词（可选）
     * @return 库存快照列表
     */
    List<Map<String, Object>> getStockSnapshot(String date, String productType, String keyword);

    /**
     * 获取统一的库存流水记录
     * @param commodityId 商品ID（可选）
     * @param startDate 开始日期（可选）
     * @param endDate 结束日期（可选）
     * @param productType 细目（可选）
     * @param keyword 关键词（可选）
     * @return 库存流水列表
     */
    List<Map<String, Object>> getStockMovements(Integer commodityId, String startDate, String endDate,
                                                 String productType, String keyword);
}
