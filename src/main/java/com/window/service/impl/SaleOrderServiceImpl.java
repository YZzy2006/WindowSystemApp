package com.window.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.common.StatusValidator;
import com.window.exception.OrderNoExistsException;
import com.window.entity.*;
import com.window.mapper.*;
import com.window.service.SaleOrderService;
import com.window.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.window.common.KeywordUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.window.common.SafeMathEvaluator;

@Slf4j
@Service
@RequiredArgsConstructor
public class SaleOrderServiceImpl implements SaleOrderService {
    private final SaleOrderMapper saleOrderMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final OrderSequenceMapper orderSequenceMapper;
    private final CustomerMapper customerMapper;
    private final SysConfigService sysConfigService;
    private final CommodityMapper commodityMapper;
    private final PaymentMapper paymentMapper;
    private final PricingFormulaMapper pricingFormulaMapper;
    private final ObjectMapper objectMapper;


    @Override
    public IPage<SaleOrder> list(Page<SaleOrder> page, String keyword, String status, Integer isCleared, String startDate, String endDate) {
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(SaleOrder::getOrderNo, escaped)
                    .or().like(SaleOrder::getCustomerName, escaped)
                    .or().like(SaleOrder::getCustomerPhone, escaped));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SaleOrder::getStatus, status);
        }
        if (isCleared != null) {
            wrapper.eq(SaleOrder::getIsCleared, isCleared);
            wrapper.ne(SaleOrder::getStatus, "cancelled");
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(SaleOrder::getOrderDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(SaleOrder::getOrderDate, LocalDate.parse(endDate));
        }
        wrapper.orderByDesc(SaleOrder::getOrderDate).orderByDesc(SaleOrder::getCreateTime);
        return saleOrderMapper.selectPage(page, wrapper);
    }

    @Override
    public Map<String, Object> getDetail(Integer id) {
        SaleOrder order = saleOrderMapper.selectById(id);
        if (order == null) return null;
        LambdaQueryWrapper<SaleOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOrderItem::getOrderId, id).orderByAsc(SaleOrderItem::getSort);
        List<SaleOrderItem> items = saleOrderItemMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return result;
    }

    @Override
    public SaleOrder getById(Integer id) {
        return saleOrderMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SaleOrder order, List<SaleOrderItem> items) {
        if (!StringUtils.hasText(order.getOrderType())) {
            order.setOrderType("sale");
        }
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus("pending");
        }
        if (!StringUtils.hasText(order.getOrderNo())) {
            boolean isPresale = "presale".equals(order.getOrderType());
            String prefix = getConfigPrefix(isPresale ? "presale_order_prefix" : "sale_order_prefix", isPresale ? "YS" : "XS");
            order.setOrderNo(generateOrderNo(isPresale ? "presale" : "sale", prefix));
        }
        // 订单号唯一预检（含软删除行，唯一索引仍占用），导入重复订单号时给出明确提示
        if (StringUtils.hasText(order.getOrderNo()) && saleOrderMapper.countByOrderNo(order.getOrderNo()) > 0) {
            throw new OrderNoExistsException("订单号已存在：" + order.getOrderNo());
        }
        // Auto-add new customer if not linked
        if (order.getCustomerId() == null && StringUtils.hasText(order.getCustomerName())) {
            LambdaQueryWrapper<Customer> cw = new LambdaQueryWrapper<>();
            cw.eq(Customer::getName, order.getCustomerName());
            Customer existing = customerMapper.selectOne(cw);
            if (existing == null) {
                try {
                    Customer newCustomer = new Customer();
                    newCustomer.setName(order.getCustomerName());
                    newCustomer.setPhone(order.getCustomerPhone());
                    newCustomer.setAddress(order.getCustomerAddress());
                    customerMapper.insert(newCustomer);
                    order.setCustomerId(newCustomer.getId());
                } catch (Exception e) {
                    // 并发插入导致唯一约束冲突，重新查询已存在的客户
                    Customer retry = customerMapper.selectOne(cw);
                    if (retry != null) {
                        order.setCustomerId(retry.getId());
                    } else {
                        throw e;
                    }
                }
            } else {
                order.setCustomerId(existing.getId());
            }
        }
        calculateOrderTotals(order, items);
        saleOrderMapper.insert(order);
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                SaleOrderItem item = items.get(i);
                item.setOrderId(order.getId());
                item.setSort(i);
                calculateItemAmount(item);
                saleOrderItemMapper.insert(item);
            }
        }
        // 定金自动入账
        syncDepositPayment(order);
        // 从实际收款记录同步 paidAmount 和 isCleared
        syncSaleOrderPaid(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateById(SaleOrder order, List<SaleOrderItem> items) {
        // 加锁防止并发编辑导致丢失更新
        LambdaQueryWrapper<SaleOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(SaleOrder::getId, order.getId()).last("FOR UPDATE");
        SaleOrder existing = saleOrderMapper.selectOne(lockWrapper);
        if (existing == null) throw new IllegalArgumentException("订单不存在");
        // 内部系统：任意状态的订单（含已完成/已取消）都可编辑

        // 销售单不允许转回预算单
        if ("sale".equals(existing.getOrderType()) && "presale".equals(order.getOrderType())) {
            throw new IllegalStateException("销售单不允许转回预算单");
        }

        // 预算单转销售单：重新生成订单编号，并同步更新所有关联收付款记录
        boolean converting = "presale".equals(existing.getOrderType()) && "sale".equals(order.getOrderType());
        if (converting) {
            String prefix = getConfigPrefix("sale_order_prefix", "XS");
            String newOrderNo = generateOrderNo("sale", prefix);
            order.setOrderNo(newOrderNo);
            // 保留原订单状态（parseOrder 默认 "pending" 会覆盖）
            order.setStatus(existing.getStatus());
            // 同步更新该订单所有收付款记录的 orderNo 和 orderType
            LambdaUpdateWrapper<Payment> puw = new LambdaUpdateWrapper<>();
            puw.eq(Payment::getOrderId, order.getId())
               .set(Payment::getOrderNo, newOrderNo)
               .set(Payment::getOrderType, "sale");
            paymentMapper.update(null, puw);
        }

        // 保留前端未传的字段（避免被覆盖为 null）
        if (!StringUtils.hasText(order.getOrderNo())) {
            order.setOrderNo(existing.getOrderNo());
        }
        if (order.getOrderDate() == null) {
            order.setOrderDate(existing.getOrderDate());
        }
        if (order.getCustomerId() == null && existing.getCustomerId() != null) {
            order.setCustomerId(existing.getCustomerId());
        }
        if (!StringUtils.hasText(order.getCustomerName())) {
            order.setCustomerName(existing.getCustomerName());
        }
        if (!StringUtils.hasText(order.getCustomerPhone())) {
            order.setCustomerPhone(existing.getCustomerPhone());
        }
        if (!StringUtils.hasText(order.getCustomerAddress())) {
            order.setCustomerAddress(existing.getCustomerAddress());
        }
        if (order.getDeposit() == null) {
            order.setDeposit(existing.getDeposit());
        }
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus(existing.getStatus());
        } else if (!order.getStatus().equals(existing.getStatus())) {
            StatusValidator.validateOrder(existing.getStatus(), order.getStatus());
        }

        if (items == null) {
            order.setTotalAmount(existing.getTotalAmount());
        }
        calculateOrderTotals(order, items);
        saleOrderMapper.updateById(order);
        if (items != null) {
            LambdaQueryWrapper<SaleOrderItem> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(SaleOrderItem::getOrderId, order.getId());
            saleOrderItemMapper.delete(deleteWrapper);
            for (int i = 0; i < items.size(); i++) {
                SaleOrderItem item = items.get(i);
                // 保留已有 item ID（防未来外键引用），新 item 的 id 由前端不传或传 null
                item.setOrderId(order.getId());
                item.setSort(i);
                calculateItemAmount(item);
                saleOrderItemMapper.insert(item);
            }
        }
        // 同步定金收款记录
        syncDepositPayment(order);
        // 从实际收款记录重新计算 paidAmount 和 isCleared
        syncSaleOrderPaid(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Integer id) {
        LambdaQueryWrapper<SaleOrder> lw = new LambdaQueryWrapper<>();
        lw.eq(SaleOrder::getId, id).last("FOR UPDATE");
        SaleOrder order = saleOrderMapper.selectOne(lw);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        // 内部系统：任意状态的订单都可删除，关联收付款一并清除
        // 删除关联的收付款记录
        LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.eq(Payment::getOrderId, id);
        paymentMapper.delete(paymentWrapper);
        // 删除订单明细
        LambdaQueryWrapper<SaleOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SaleOrderItem::getOrderId, id);
        saleOrderItemMapper.delete(wrapper);
        // 删除订单
        saleOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return;

        // Lock and validate each order exists
        for (Integer id : ids) {
            LambdaQueryWrapper<SaleOrder> lw = new LambdaQueryWrapper<>();
            lw.eq(SaleOrder::getId, id).last("FOR UPDATE");
            SaleOrder order = saleOrderMapper.selectOne(lw);
            if (order == null) throw new IllegalArgumentException("订单不存在: id=" + id);
        }

        // Batch delete payments
        LambdaQueryWrapper<Payment> paymentWrapper = new LambdaQueryWrapper<>();
        paymentWrapper.in(Payment::getOrderId, ids);
        paymentMapper.delete(paymentWrapper);

        // Batch delete order items
        LambdaQueryWrapper<SaleOrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(SaleOrderItem::getOrderId, ids);
        saleOrderItemMapper.delete(itemWrapper);

        // Batch delete orders
        saleOrderMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Integer> ids, String status) {
        if (ids == null || ids.isEmpty()) return;

        // Lock and validate each order's status transition
        for (Integer id : ids) {
            LambdaQueryWrapper<SaleOrder> lw = new LambdaQueryWrapper<>();
            lw.eq(SaleOrder::getId, id).last("FOR UPDATE");
            SaleOrder existing = saleOrderMapper.selectOne(lw);
            if (existing == null) throw new IllegalArgumentException("订单不存在: id=" + id);
            StatusValidator.validateOrder(existing.getStatus(), status);
        }

        // Batch update all orders' status
        LambdaUpdateWrapper<SaleOrder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(SaleOrder::getId, ids)
                .set(SaleOrder::getStatus, status);
        saleOrderMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, String status) {
        LambdaQueryWrapper<SaleOrder> lw = new LambdaQueryWrapper<>();
        lw.eq(SaleOrder::getId, id).last("FOR UPDATE");
        SaleOrder existing = saleOrderMapper.selectOne(lw);
        if (existing == null) throw new IllegalArgumentException("订单不存在");
        StatusValidator.validateOrder(existing.getStatus(), status);
        saleOrderMapper.updateStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toggleCleared(Integer id) {
        LambdaQueryWrapper<SaleOrder> lw = new LambdaQueryWrapper<>();
        lw.eq(SaleOrder::getId, id).last("FOR UPDATE");
        SaleOrder order = saleOrderMapper.selectOne(lw);
        if (order == null) throw new IllegalArgumentException("订单不存在");
        int newVal = order.getIsCleared() != null && order.getIsCleared() == 1 ? 0 : 1;
        saleOrderMapper.updateCleared(id, newVal);
        if (newVal == 1 && !"completed".equals(order.getStatus())) {
            saleOrderMapper.updateStatus(id, "completed");
        }
    }

    @Override
    public Map<String, Object> getSummary(String keyword, String status, Integer isCleared, String startDate, String endDate) {
        // 统计销售单：orderType = 'sale' 或 未设置(orderType IS NULL)，排除已取消
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        wrapper.ne(SaleOrder::getStatus, "cancelled");
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(SaleOrder::getOrderNo, escaped)
                    .or().like(SaleOrder::getCustomerName, escaped)
                    .or().like(SaleOrder::getCustomerPhone, escaped));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SaleOrder::getStatus, status);
        }
        if (isCleared != null) {
            wrapper.eq(SaleOrder::getIsCleared, isCleared);
            wrapper.ne(SaleOrder::getStatus, "cancelled");
        }
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(SaleOrder::getOrderDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(SaleOrder::getOrderDate, LocalDate.parse(endDate));
        }
        List<SaleOrder> orders = saleOrderMapper.selectList(wrapper);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        int totalCount = orders.size();
        int clearedCount = 0;

        // 批量查询所有订单明细，避免 N+1
        List<Integer> orderIds = orders.stream().map(SaleOrder::getId).collect(Collectors.toList());
        Map<Integer, List<SaleOrderItem>> itemsByOrder = new HashMap<>();
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<SaleOrderItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(SaleOrderItem::getOrderId, orderIds);
            List<SaleOrderItem> allItems = saleOrderItemMapper.selectList(itemWrapper);
            itemsByOrder = allItems.stream().collect(Collectors.groupingBy(SaleOrderItem::getOrderId));
        }

        for (SaleOrder o : orders) {
            totalAmount = totalAmount.add(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
            totalPaid = totalPaid.add(o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO);
            if (o.getIsCleared() != null && o.getIsCleared() == 1) clearedCount++;

            for (SaleOrderItem item : itemsByOrder.getOrDefault(o.getId(), Collections.emptyList())) {
                if (item.getCost() != null && item.getQuantity() != null) {
                    // cost 是单价成本，quantity 已包含对应因子，不再乘 doorCount
                    totalCost = totalCost.add(item.getCost().multiply(item.getQuantity()));
                }
            }
        }

        BigDecimal totalProfit = totalAmount.subtract(totalCost);
        BigDecimal profitRate = totalAmount.compareTo(BigDecimal.ZERO) > 0 ?
                totalProfit.divide(totalAmount, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", totalCount);
        summary.put("clearedCount", clearedCount);
        summary.put("unclearedCount", totalCount - clearedCount);
        summary.put("totalAmount", totalAmount);
        summary.put("totalPaid", totalPaid);
        summary.put("totalUnpaid", totalAmount.subtract(totalPaid));
        summary.put("totalCost", totalCost);
        summary.put("totalProfit", totalProfit);
        summary.put("profitRate", profitRate);
        return summary;
    }

    private void calculateOrderTotals(SaleOrder order, List<SaleOrderItem> items) {
        if (items == null) return;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal windowArea = BigDecimal.ZERO;
        int windowCount = 0;
        BigDecimal doorArea = BigDecimal.ZERO;
        int doorCount = 0;
        for (SaleOrderItem item : items) {
            calculateItemAmount(item);
            if (item.getAmount() != null) {
                totalAmount = totalAmount.add(item.getAmount());
            }
            String type = item.getProductType();
            if (type != null) {
                boolean isWindow = type.contains("窗") || type.contains("固定");
                boolean isDoor = type.contains("门") && !type.contains("门套");
                if (isWindow && item.getWidth() != null && item.getHeight() != null) {
                    BigDecimal area = item.getWidth().multiply(item.getHeight())
                            .divide(new BigDecimal("1000000"), 4, java.math.RoundingMode.HALF_UP);
                    windowArea = windowArea.add(area.multiply(new BigDecimal(item.getDoorCount() != null ? item.getDoorCount() : 1)));
                    windowCount += item.getDoorCount() != null ? item.getDoorCount() : 1;
                }
                if (isDoor && item.getWidth() != null && item.getHeight() != null) {
                    BigDecimal area = item.getWidth().multiply(item.getHeight())
                            .divide(new BigDecimal("1000000"), 4, java.math.RoundingMode.HALF_UP);
                    doorArea = doorArea.add(area.multiply(new BigDecimal(item.getDoorCount() != null ? item.getDoorCount() : 1)));
                    doorCount += item.getDoorCount() != null ? item.getDoorCount() : 1;
                }
            }
        }
        order.setTotalAmount(totalAmount);
        order.setWindowArea(windowArea);
        order.setWindowCount(windowCount);
        order.setDoorArea(doorArea);
        order.setDoorCount(doorCount);
    }

    private void calculateItemAmount(SaleOrderItem item) {
        // 手改金额（砍价）：用户点击金额列手动编辑后传入，直接保留，不按公式重算覆盖
        if (item.getAmount() != null) {
            if (item.getQuantity() == null) item.setQuantity(BigDecimal.ZERO);
            addExtraFee(item);
            return;
        }
        // 待定：面积或樘数任一标"待定" → 金额按 0，仅显示单价
        if (Boolean.TRUE.equals(item.getFangPending()) || Boolean.TRUE.equals(item.getDoorPending())) {
            item.setQuantity(BigDecimal.ZERO);
            item.setAmount(BigDecimal.ZERO);
            return;
        }
        if (item.getCommodityId() == null) {
            // 无商品ID（导入等）时，用 quantity * unitPrice
            if (item.getAmount() == null && item.getQuantity() != null && item.getUnitPrice() != null) {
                item.setAmount(item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
            }
            addExtraFee(item);
            return;
        }

        Commodity commodity = commodityMapper.selectById(item.getCommodityId());
        if (commodity == null) {
            // 商品已被删除，用 quantity * unitPrice
            if (item.getAmount() == null && item.getQuantity() != null && item.getUnitPrice() != null) {
                item.setAmount(item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
            }
            addExtraFee(item);
            return;
        }

        // 公式来源：行级覆盖 > 商品预设（不再按单位匹配，商品必须显式绑公式）
        PricingFormula formula = null;
        if (item.getFormulaId() != null) {
            formula = pricingFormulaMapper.selectById(item.getFormulaId());
        }
        if (formula == null && commodity.getFormulaId() != null) {
            formula = pricingFormulaMapper.selectById(commodity.getFormulaId());
        }

        // 手填面积优先：用户手填面积(manualArea)时按手填值（每樘面积）×樘数，不受公式重算影响
        if (Boolean.TRUE.equals(item.getManualArea())
                && item.getFangCount() != null
                && item.getFangCount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal doorCount = (item.getDoorCount() != null && item.getDoorCount() > 0)
                    ? BigDecimal.valueOf(item.getDoorCount())
                    : BigDecimal.ONE;
            item.setQuantity(item.getFangCount().multiply(doorCount).setScale(2, RoundingMode.HALF_UP));
            if (item.getUnitPrice() != null) {
                item.setAmount(item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
            }
            addExtraFee(item);
            return;
        }

        if (formula != null && StringUtils.hasText(formula.getFormula())) {
            try {
                // 统一变量映射 + 参数默认值（与前端 FORMULA_VAR_MAP 一致）
                Map<String, BigDecimal> paramMap = buildFormulaParamMap(item, formula);
                String expr = formula.getFormula();
                List<Map.Entry<String, BigDecimal>> sortedParams = new ArrayList<>(paramMap.entrySet());
                sortedParams.sort((a, b) -> b.getKey().length() - a.getKey().length());
                for (Map.Entry<String, BigDecimal> entry : sortedParams) {
                    expr = expr.replace(entry.getKey(), entry.getValue().toPlainString());
                }
                // 中文输入法符号 → 英文（兼容历史公式里的中文括号/乘除号，须在校验前转换）
                expr = SafeMathEvaluator.normalizeSymbols(expr);
                // 安全校验：替换后只允许数字、运算符、括号、小数点、空格
                if (!expr.matches("[0-9+\\-*/%().\\s]+")) {
                    throw new IllegalArgumentException("公式包含非法字符: " + expr);
                }
                BigDecimal qty = SafeMathEvaluator.evaluate(expr);
                if (qty.compareTo(BigDecimal.ZERO) < 0) qty = BigDecimal.ZERO;
                qty = qty.setScale(2, RoundingMode.HALF_UP); // 与前端一致（2位小数）
                // 单面积模型：公式算的是"每樘计量值"，数量 = 每樘值 × 樘数（樘数=数量），
                // 金额 = 数量 × 单价。数学结果与旧公式（公式内含樘数）完全等价。
                // 樘数缺省 1，且 ≤0（用户误填 0/负数）时按 1 处理，与前端 `doorCount > 0 ? doorCount : 1` 一致
                BigDecimal doorCount = (item.getDoorCount() != null && item.getDoorCount() > 0)
                        ? BigDecimal.valueOf(item.getDoorCount())
                        : BigDecimal.ONE;
                item.setQuantity(qty.multiply(doorCount).setScale(2, RoundingMode.HALF_UP));
                if (item.getUnitPrice() != null) {
                    item.setAmount(item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
                }
                // 公式快照
                try {
                    Map<String, Object> snapshot = new LinkedHashMap<>();
                    snapshot.put("formulaId", formula.getId());
                    snapshot.put("formula", formula.getFormula());
                    snapshot.put("name", formula.getName());
                    snapshot.put("unit", formula.getUnit());
                    Map<String, String> paramsSnapshot = new LinkedHashMap<>();
                    for (Map.Entry<String, BigDecimal> e : paramMap.entrySet()) {
                        paramsSnapshot.put(e.getKey(), e.getValue().toPlainString());
                    }
                    snapshot.put("params", paramsSnapshot);
                    item.setFormulaSnapshot(objectMapper.writeValueAsString(snapshot));
                } catch (JsonProcessingException ignored) {
                }
                addExtraFee(item);
                return;
            } catch (Exception e) {
                // 公式计算失败：保留明细传入的 quantity，不静默切换其它计价
                log.warn("公式 [{}] 计算失败，保留明细传入数量: {}", formula.getName(), e.getMessage());
            }
        }

        // 无公式或公式失败：保留 quantity，金额 = quantity * unitPrice
        if (item.getQuantity() == null) {
            item.setQuantity(BigDecimal.ZERO);
        }
        if (item.getAmount() == null && item.getUnitPrice() != null) {
            item.setAmount(item.getQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
        }
        addExtraFee(item);
    }

    /** 构建公式参数映射：item 字段值优先，缺失用公式参数默认值；樘数缺省 1 */
    private Map<String, BigDecimal> buildFormulaParamMap(SaleOrderItem item, PricingFormula formula) {
        Map<String, BigDecimal> defaults = parseFormulaDefaults(formula);
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        putParam(map, defaults, "宽", item.getWidth());
        putParam(map, defaults, "高", item.getHeight());
        putParam(map, defaults, "墙厚", item.getWallThickness());
        putParam(map, defaults, "吊脚", item.getDiaojiao() != null ? BigDecimal.valueOf(item.getDiaojiao()) : null);
        putParam(map, defaults, "面积", item.getFangCount());
        // 樘数缺省 1（与前端一致）
        putParam(map, defaults, "樘数", item.getDoorCount() != null ? BigDecimal.valueOf(item.getDoorCount()) : BigDecimal.ONE);
        return map;
    }

    private void putParam(Map<String, BigDecimal> map, Map<String, BigDecimal> defaults, String name, BigDecimal value) {
        if (value == null) value = defaults.get(name);
        if (value != null) map.put(name, value);
    }

    /** 解析公式 parameters JSON：[{name,label,unit,default}] 中的默认值 */
    private Map<String, BigDecimal> parseFormulaDefaults(PricingFormula formula) {
        Map<String, BigDecimal> defaults = new HashMap<>();
        if (!StringUtils.hasText(formula.getParameters())) return defaults;
        String json = formula.getParameters();
        try {
            List<?> params = objectMapper.readValue(json, List.class);
            for (Object o : params) {
                if (o instanceof Map<?, ?> m) {
                    Object name = m.get("name");
                    Object def = m.get("default");
                    if (name instanceof String n && def != null && !def.toString().isBlank()) {
                        try {
                            defaults.put(n, new BigDecimal(def.toString().trim()));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 兼容历史数据双重转义 [{\"name\":...}] → [{"name":...}]（前端已有同样容错）
            // 否则默认值 Map 为空 → 变量不被替换 → 公式校验失败 → 金额算不出
            try {
                json = json.replace("\\\"", "\"");
                List<?> params = objectMapper.readValue(json, List.class);
                for (Object o : params) {
                    if (o instanceof Map<?, ?> m) {
                        Object name = m.get("name");
                        Object def = m.get("default");
                        if (name instanceof String n && def != null && !def.toString().isBlank()) {
                            try {
                                defaults.put(n, new BigDecimal(def.toString().trim()));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                log.warn("公式[{}]参数解析失败(含容错重试): {}", formula.getName(), e.getMessage());
            }
        }
        return defaults;
    }

    /**
     * 累加额外费用到 amount
     */
    private void addExtraFee(SaleOrderItem item) {
        if (item.getExtraFee() != null && item.getExtraFee().compareTo(BigDecimal.ZERO) != 0) {
            if (item.getAmount() != null) {
                item.setAmount(item.getAmount().add(item.getExtraFee()).setScale(2, RoundingMode.HALF_UP));
            } else {
                item.setAmount(item.getExtraFee());
            }
        }
    }

    /**
     * 同步定金收款记录：定金>0时创建/更新，定金=0时删除
     */
    private void syncDepositPayment(SaleOrder order) {
        LambdaQueryWrapper<Payment> pw = new LambdaQueryWrapper<>();
        pw.eq(Payment::getOrderId, order.getId())
          .eq(Payment::getType, "receipt")
          .eq(Payment::getRemark, "定金");
        Payment existing = paymentMapper.selectOne(pw);

        BigDecimal deposit = order.getDeposit() != null ? order.getDeposit() : BigDecimal.ZERO;

        if (deposit.compareTo(BigDecimal.ZERO) > 0) {
            if (existing != null) {
                // 更新定金金额
                existing.setAmount(deposit);
                existing.setPaymentDate(order.getOrderDate());
                existing.setPartyName(order.getCustomerName());
                existing.setOrderNo(order.getOrderNo());
                existing.setOrderType(order.getOrderType());
                paymentMapper.updateById(existing);
            } else {
                // 创建定金收款
                Payment depositPayment = new Payment();
                depositPayment.setOrderId(order.getId());
                depositPayment.setOrderNo(order.getOrderNo());
                depositPayment.setPaymentDate(order.getOrderDate());
                depositPayment.setType("receipt");
                depositPayment.setAmount(deposit);
                depositPayment.setPartyName(order.getCustomerName());
                depositPayment.setRemark("定金");
                depositPayment.setOrderType(order.getOrderType());
                paymentMapper.insert(depositPayment);
            }
        } else if (existing != null) {
            // 定金清零，删除定金收款记录
            paymentMapper.deleteById(existing.getId());
        }
    }

    /**
     * 从实际收款记录同步 paidAmount 和 isCleared
     */
    private void syncSaleOrderPaid(Integer orderId) {
        // 先锁订单行，防止并发支付导致 paidAmount 丢失更新
        LambdaQueryWrapper<SaleOrder> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(SaleOrder::getId, orderId).last("FOR UPDATE");
        SaleOrder order = saleOrderMapper.selectOne(lockWrapper);
        if (order == null) return;

        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Payment::getOrderId, orderId).eq(Payment::getType, "receipt");
        List<Payment> payments = paymentMapper.selectList(wrapper);
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean cleared = false;
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unpaid = order.getTotalAmount().subtract(totalPaid);
            cleared = unpaid.compareTo(new BigDecimal("0.01")) <= 0;
        }
        saleOrderMapper.updatePaidAmount(orderId, totalPaid, cleared ? 1 : 0);
        if (cleared && !"completed".equals(order.getStatus()) && !"cancelled".equals(order.getStatus())) {
            saleOrderMapper.updateStatus(orderId, "completed");
        }
    }

    @Override
    public Map<Integer, List<SaleOrderItem>> getItemsByOrderIds(List<Integer> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) return Collections.emptyMap();
        LambdaQueryWrapper<SaleOrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SaleOrderItem::getOrderId, orderIds).orderByAsc(SaleOrderItem::getSort);
        List<SaleOrderItem> items = saleOrderItemMapper.selectList(wrapper);
        return items.stream().collect(Collectors.groupingBy(SaleOrderItem::getOrderId));
    }

    @Override
    public Map<String, Object> getReport(String startDate, String endDate, String keyword) {
        // 1. 查询销售订单（排除已取消）
        LambdaQueryWrapper<SaleOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(SaleOrder::getOrderType, "sale").or().isNull(SaleOrder::getOrderType));
        wrapper.ne(SaleOrder::getStatus, "cancelled");
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(SaleOrder::getOrderDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(SaleOrder::getOrderDate, LocalDate.parse(endDate));
        }
        if (StringUtils.hasText(keyword)) {
            final String escaped = KeywordUtil.escapeLike(keyword);
            wrapper.and(w -> w.like(SaleOrder::getOrderNo, escaped)
                    .or().like(SaleOrder::getCustomerName, escaped)
                    .or().like(SaleOrder::getCustomerPhone, escaped)
                    .or().like(SaleOrder::getRemark, escaped));
        }
        wrapper.orderByAsc(SaleOrder::getOrderDate).orderByAsc(SaleOrder::getId);
        wrapper.last("LIMIT 1001");
        List<SaleOrder> allOrders = saleOrderMapper.selectList(wrapper);
        boolean truncated = allOrders.size() > 1000;
        List<SaleOrder> orders = truncated ? allOrders.subList(0, 1000) : allOrders;

        // 2. 批量查询明细
        List<Integer> orderIds = orders.stream().map(SaleOrder::getId).collect(Collectors.toList());
        Map<Integer, List<SaleOrderItem>> itemsByOrder = new HashMap<>();
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<SaleOrderItem> iw = new LambdaQueryWrapper<>();
            iw.in(SaleOrderItem::getOrderId, orderIds);
            itemsByOrder = saleOrderItemMapper.selectList(iw).stream()
                    .collect(Collectors.groupingBy(SaleOrderItem::getOrderId));
        }

        // 3. 批量查询收付款
        Map<Integer, List<Payment>> paymentsByOrder = new HashMap<>();
        if (!orderIds.isEmpty()) {
            LambdaQueryWrapper<Payment> pw = new LambdaQueryWrapper<>();
            pw.in(Payment::getOrderId, orderIds);
            paymentsByOrder = paymentMapper.selectList(pw).stream()
                    .collect(Collectors.groupingBy(Payment::getOrderId));
        }

        // 4. 构建订单明细列表
        List<Map<String, Object>> orderDetails = new ArrayList<>();
        for (SaleOrder o : orders) {
            BigDecimal cost = BigDecimal.ZERO;
            for (SaleOrderItem item : itemsByOrder.getOrDefault(o.getId(), Collections.emptyList())) {
                if (item.getCost() != null && item.getQuantity() != null) {
                    cost = cost.add(item.getCost().multiply(item.getQuantity()));
                }
            }
            BigDecimal totalAmt = o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal paidAmt = o.getPaidAmount() != null ? o.getPaidAmount() : BigDecimal.ZERO;
            BigDecimal profit = totalAmt.subtract(cost);
            BigDecimal profitRate = totalAmt.compareTo(BigDecimal.ZERO) > 0 ?
                    profit.divide(totalAmt, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            Map<String, Object> detail = new HashMap<>();
            detail.put("orderId", o.getId());
            detail.put("orderDate", o.getOrderDate());
            detail.put("orderNo", o.getOrderNo());
            detail.put("customerName", o.getCustomerName());
            detail.put("totalAmount", totalAmt);
            detail.put("paidAmount", paidAmt);
            detail.put("unpaidAmount", totalAmt.subtract(paidAmt));
            detail.put("cost", cost);
            detail.put("profit", profit);
            detail.put("profitRate", profitRate);
            // 收付款明细
            List<Map<String, Object>> paymentList = new ArrayList<>();
            for (Payment p : paymentsByOrder.getOrDefault(o.getId(), Collections.emptyList())) {
                Map<String, Object> pm = new HashMap<>();
                pm.put("id", p.getId());
                pm.put("paymentDate", p.getPaymentDate());
                pm.put("type", p.getType());
                pm.put("amount", p.getAmount());
                pm.put("partyName", p.getPartyName());
                pm.put("remark", p.getRemark());
                paymentList.add(pm);
            }
            detail.put("payments", paymentList);
            orderDetails.add(detail);
        }

        // 5. 按款方汇总
        Map<String, List<Map<String, Object>>> grouped = orderDetails.stream()
                .collect(Collectors.groupingBy(d -> (String) d.getOrDefault("customerName", "未知")));
        List<Map<String, Object>> customerSummary = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;
        BigDecimal grandPaid = BigDecimal.ZERO;
        BigDecimal grandCost = BigDecimal.ZERO;

        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            BigDecimal cTotal = BigDecimal.ZERO;
            BigDecimal cPaid = BigDecimal.ZERO;
            BigDecimal cCost = BigDecimal.ZERO;
            for (Map<String, Object> d : entry.getValue()) {
                cTotal = cTotal.add((BigDecimal) d.get("totalAmount"));
                cPaid = cPaid.add((BigDecimal) d.get("paidAmount"));
                cCost = cCost.add((BigDecimal) d.get("cost"));
            }
            BigDecimal cProfit = cTotal.subtract(cCost);
            BigDecimal cRate = cTotal.compareTo(BigDecimal.ZERO) > 0 ?
                    cProfit.divide(cTotal, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            Map<String, Object> cs = new HashMap<>();
            cs.put("customerName", entry.getKey());
            cs.put("totalAmount", cTotal);
            cs.put("paidAmount", cPaid);
            cs.put("unpaidAmount", cTotal.subtract(cPaid));
            cs.put("cost", cCost);
            cs.put("profit", cProfit);
            cs.put("profitRate", cRate);
            cs.put("orders", entry.getValue());
            customerSummary.add(cs);

            grandTotal = grandTotal.add(cTotal);
            grandPaid = grandPaid.add(cPaid);
            grandCost = grandCost.add(cCost);
        }

        BigDecimal grandProfit = grandTotal.subtract(grandCost);
        BigDecimal grandRate = grandTotal.compareTo(BigDecimal.ZERO) > 0 ?
                grandProfit.divide(grandTotal, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAmount", grandTotal);
        summary.put("paidAmount", grandPaid);
        summary.put("unpaidAmount", grandTotal.subtract(grandPaid));
        summary.put("cost", grandCost);
        summary.put("profit", grandProfit);
        summary.put("profitRate", grandRate);
        summary.put("orderCount", orders.size());

        Map<String, Object> result = new HashMap<>();
        result.put("summary", summary);
        result.put("customers", customerSummary);
        result.put("truncated", truncated);
        return result;
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
                // 并发首次创建，另一线程已插入，重新读取
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
