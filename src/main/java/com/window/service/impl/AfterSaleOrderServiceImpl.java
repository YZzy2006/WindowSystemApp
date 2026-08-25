package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.entity.AfterSaleOrder;
import com.window.entity.OrderSequence;
import com.window.mapper.AfterSaleOrderMapper;
import com.window.mapper.OrderSequenceMapper;
import com.window.service.AfterSaleOrderService;
import com.window.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.window.common.KeywordUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AfterSaleOrderServiceImpl implements AfterSaleOrderService {
    private final AfterSaleOrderMapper afterSaleOrderMapper;
    private final OrderSequenceMapper orderSequenceMapper;
    private final SysConfigService sysConfigService;

    @Override
    public IPage<AfterSaleOrder> list(Page<AfterSaleOrder> page, String keyword, String status, String type, String startDate, String endDate) {
        LambdaQueryWrapper<AfterSaleOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(AfterSaleOrder::getOrderNo, escaped)
                    .or().like(AfterSaleOrder::getSaleOrderNo, escaped)
                    .or().like(AfterSaleOrder::getCustomerName, escaped)
                    .or().like(AfterSaleOrder::getDescription, escaped)
                    .or().like(AfterSaleOrder::getAssignedTo, escaped)
                    .or().like(AfterSaleOrder::getRemark, escaped));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AfterSaleOrder::getStatus, status);
        }
        if (StringUtils.hasText(type)) {
            wrapper.eq(AfterSaleOrder::getType, type);
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(AfterSaleOrder::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(AfterSaleOrder::getCreateTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        wrapper.orderByDesc(AfterSaleOrder::getCreateTime);
        return afterSaleOrderMapper.selectPage(page, wrapper);
    }

    @Override
    public AfterSaleOrder getById(Integer id) {
        return afterSaleOrderMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(AfterSaleOrder order) {
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus("pending");
        }
        String prefix = getConfigPrefix("after_sale_prefix", "SH");
        order.setOrderNo(generateOrderNo("after_sale", prefix));
        afterSaleOrderMapper.insert(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(AfterSaleOrder order) {
        afterSaleOrderMapper.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        afterSaleOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, String status) {
        AfterSaleOrder existing = afterSaleOrderMapper.selectById(id);
        if (existing == null) throw new IllegalArgumentException("售后单不存在");
        AfterSaleOrder update = new AfterSaleOrder();
        update.setId(id);
        update.setStatus(status);
        if ("completed".equals(status)) {
            update.setCompletedDate(LocalDate.now());
        }
        afterSaleOrderMapper.updateById(update);
    }

    @Override
    public Map<String, Object> getSummary(String startDate, String endDate) {
        LambdaQueryWrapper<AfterSaleOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(AfterSaleOrder::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(AfterSaleOrder::getCreateTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        List<AfterSaleOrder> orders = afterSaleOrderMapper.selectList(wrapper);

        int pendingCount = 0;
        int processingCount = 0;
        int completedCount = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        for (AfterSaleOrder o : orders) {
            if ("pending".equals(o.getStatus()) || "assigned".equals(o.getStatus())) {
                pendingCount++;
            } else if ("processing".equals(o.getStatus())) {
                processingCount++;
            } else if ("completed".equals(o.getStatus()) || "closed".equals(o.getStatus())) {
                completedCount++;
            }
            totalCost = totalCost.add(o.getCost() != null ? o.getCost() : BigDecimal.ZERO);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", orders.size());
        summary.put("pendingCount", pendingCount);
        summary.put("processingCount", processingCount);
        summary.put("completedCount", completedCount);
        summary.put("totalCost", totalCost);
        return summary;
    }

    private String getConfigPrefix(String configKey, String defaultPrefix) {
        String val = sysConfigService.get(configKey);
        return (val != null && !val.isEmpty()) ? val : defaultPrefix;
    }

    private String generateOrderNo(String seqType, String prefix) {
        LambdaQueryWrapper<OrderSequence> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderSequence::getSeqType, seqType).last("FOR UPDATE");
        OrderSequence seq = orderSequenceMapper.selectOne(wrapper);
        if (seq == null) {
            seq = new OrderSequence();
            seq.setSeqType(seqType);
            seq.setPrefix(prefix);
            seq.setCurrentSeq(1);
            seq.setLastDate(LocalDate.now());
            try {
                orderSequenceMapper.insert(seq);
            } catch (org.springframework.dao.DuplicateKeyException e) {
                seq = orderSequenceMapper.selectOne(wrapper);
            }
        } else if (!LocalDate.now().equals(seq.getLastDate())) {
            seq.setCurrentSeq(1);
        }
        int seqNum = seq.getCurrentSeq();
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String orderNo = prefix + dateStr + String.format("%03d", seqNum);
        seq.setCurrentSeq(seqNum + 1);
        seq.setLastDate(LocalDate.now());
        orderSequenceMapper.updateById(seq);
        return orderNo;
    }
}
