package com.window.controller;

import com.window.dto.Result;
import com.window.entity.*;
import com.window.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin")
public class RestoreController {

    private static final Logger log = LoggerFactory.getLogger(RestoreController.class);

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final CaseMapper caseMapper;
    private final EnquiryMapper enquiryMapper;
    private final CustomerMapper customerMapper;
    private final SupplierMapper supplierMapper;
    private final CommodityMapper commodityMapper;
    private final CommodityCategoryMapper commodityCategoryMapper;
    private final ProductTypeMapper productTypeMapper;
    private final PricingFormulaMapper pricingFormulaMapper;
    private final AfterSaleOrderMapper afterSaleOrderMapper;
    private final SaleOrderMapper saleOrderMapper;
    private final SaleOrderItemMapper saleOrderItemMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final SaleReturnMapper saleReturnMapper;
    private final SaleReturnItemMapper saleReturnItemMapper;
    private final PurchaseReturnMapper purchaseReturnMapper;
    private final PurchaseReturnItemMapper purchaseReturnItemMapper;
    private final StockInMapper stockInMapper;
    private final StockInItemMapper stockInItemMapper;
    private final StockOutMapper stockOutMapper;
    private final StockOutItemMapper stockOutItemMapper;
    private final PaymentMapper paymentMapper;

    public RestoreController(CategoryMapper categoryMapper, ProductMapper productMapper,
                             CaseMapper caseMapper, EnquiryMapper enquiryMapper,
                             CustomerMapper customerMapper, SupplierMapper supplierMapper,
                             CommodityMapper commodityMapper, CommodityCategoryMapper commodityCategoryMapper,
                             ProductTypeMapper productTypeMapper, PricingFormulaMapper pricingFormulaMapper,
                             AfterSaleOrderMapper afterSaleOrderMapper,
                             SaleOrderMapper saleOrderMapper, SaleOrderItemMapper saleOrderItemMapper,
                             PurchaseOrderMapper purchaseOrderMapper, PurchaseOrderItemMapper purchaseOrderItemMapper,
                             SaleReturnMapper saleReturnMapper, SaleReturnItemMapper saleReturnItemMapper,
                             PurchaseReturnMapper purchaseReturnMapper, PurchaseReturnItemMapper purchaseReturnItemMapper,
                             StockInMapper stockInMapper, StockInItemMapper stockInItemMapper,
                             StockOutMapper stockOutMapper, StockOutItemMapper stockOutItemMapper,
                             PaymentMapper paymentMapper) {
        this.categoryMapper = categoryMapper;
        this.productMapper = productMapper;
        this.caseMapper = caseMapper;
        this.enquiryMapper = enquiryMapper;
        this.customerMapper = customerMapper;
        this.supplierMapper = supplierMapper;
        this.commodityMapper = commodityMapper;
        this.commodityCategoryMapper = commodityCategoryMapper;
        this.productTypeMapper = productTypeMapper;
        this.pricingFormulaMapper = pricingFormulaMapper;
        this.afterSaleOrderMapper = afterSaleOrderMapper;
        this.saleOrderMapper = saleOrderMapper;
        this.saleOrderItemMapper = saleOrderItemMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.saleReturnMapper = saleReturnMapper;
        this.saleReturnItemMapper = saleReturnItemMapper;
        this.purchaseReturnMapper = purchaseReturnMapper;
        this.purchaseReturnItemMapper = purchaseReturnItemMapper;
        this.stockInMapper = stockInMapper;
        this.stockInItemMapper = stockInItemMapper;
        this.stockOutMapper = stockOutMapper;
        this.stockOutItemMapper = stockOutItemMapper;
        this.paymentMapper = paymentMapper;
    }

    @PostMapping("/restore/{entityType}/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result restore(@PathVariable String entityType, @PathVariable Integer id, HttpServletRequest request) {
        if (!"super_admin".equals(request.getAttribute("adminRole"))) {
            return Result.error(403, "仅超级管理员可操作");
        }
        switch (entityType) {
            case "category": categoryMapper.restoreById(id); break;
            case "product": productMapper.restoreById(id); break;
            case "case": caseMapper.restoreById(id); break;
            case "enquiry": enquiryMapper.restoreById(id); break;
            case "customer": customerMapper.restoreById(id); break;
            case "supplier": supplierMapper.restoreById(id); break;
            case "commodity": commodityMapper.restoreById(id); break;
            case "commodity-category": commodityCategoryMapper.restoreById(id); break;
            case "product-type": productTypeMapper.restoreById(id); break;
            case "pricing-formula": pricingFormulaMapper.restoreById(id); break;
            case "after-sale": afterSaleOrderMapper.restoreById(id); break;
            case "sale-order": restoreSaleOrder(id); break;
            case "purchase-order": restorePurchaseOrder(id); break;
            case "sale-return": restoreSaleReturn(id); break;
            case "purchase-return": restorePurchaseReturn(id); break;
            case "stock-in": restoreStockIn(id); break;
            case "stock-out": restoreStockOut(id); break;
            case "payment":
                Payment p = paymentMapper.selectByIdRaw(id);
                paymentMapper.restoreById(id);
                if (p != null && p.getOrderId() != null) {
                    syncPaymentOrderPaid(p.getOrderId(), p.getOrderType());
                }
                break;
            default: return Result.error(400, "不支持的实体类型: " + entityType);
        }
        return Result.success(null);
    }

    private void restoreSaleOrder(Integer orderId) {
        saleOrderMapper.restoreById(orderId);
        saleOrderItemMapper.restoreByOrderId(orderId);
        paymentMapper.restoreByOrderIdAndType(orderId, "receipt");
        syncSaleOrderPaid(orderId);
    }

    private void restorePurchaseOrder(Integer orderId) {
        purchaseOrderMapper.restoreById(orderId);
        purchaseOrderItemMapper.restoreByOrderId(orderId);
        paymentMapper.restoreByOrderIdAndType(orderId, "payment");
        syncPurchaseOrderPaid(orderId);
    }

    private void restoreSaleReturn(Integer returnId) {
        saleReturnMapper.restoreById(returnId);
        saleReturnItemMapper.restoreByReturnId(returnId);
        paymentMapper.restoreByOrderIdAndType(returnId, "payment");
        syncSaleReturnPaid(returnId);
    }

    private void restorePurchaseReturn(Integer returnId) {
        purchaseReturnMapper.restoreById(returnId);
        purchaseReturnItemMapper.restoreByReturnId(returnId);
        paymentMapper.restoreByOrderIdAndType(returnId, "receipt");
        syncPurchaseReturnPaid(returnId);
    }

    private void restoreStockIn(Integer stockInId) {
        // 幂等保护：已恢复则跳过，避免库存重复计算
        if (stockInMapper.selectById(stockInId) != null) return;
        stockInMapper.restoreById(stockInId);
        stockInItemMapper.restoreByStockInId(stockInId);
        List<StockInItem> items = stockInItemMapper.selectAllByStockInId(stockInId);
        for (StockInItem item : items) {
            if (item.getCommodityId() != null && item.getQuantity() != null) {
                commodityMapper.addQuantity(item.getCommodityId(), item.getQuantity());
            }
        }
    }

    private void restoreStockOut(Integer stockOutId) {
        // 幂等保护：已恢复则跳过，避免库存重复计算
        if (stockOutMapper.selectById(stockOutId) != null) return;
        stockOutMapper.restoreById(stockOutId);
        stockOutItemMapper.restoreByStockOutId(stockOutId);
        List<StockOutItem> items = stockOutItemMapper.selectAllByStockOutId(stockOutId);
        for (StockOutItem item : items) {
            if (item.getCommodityId() != null && item.getQuantity() != null) {
                commodityMapper.subtractQuantity(item.getCommodityId(), item.getQuantity());
            }
        }
    }

    // ===== Paid Amount Sync =====

    private void syncSaleOrderPaid(Integer orderId) {
        SaleOrder order = saleOrderMapper.selectById(orderId);
        if (order == null) return;
        BigDecimal totalPaid = sumPayments(orderId, "receipt");
        boolean cleared = false;
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unpaid = order.getTotalAmount().subtract(totalPaid);
            cleared = unpaid.compareTo(new BigDecimal("0.01")) <= 0;
        }
        saleOrderMapper.updatePaidAmount(orderId, totalPaid, cleared ? 1 : 0);
    }

    private void syncPurchaseOrderPaid(Integer orderId) {
        PurchaseOrder order = purchaseOrderMapper.selectById(orderId);
        if (order == null) return;
        BigDecimal totalPaid = sumPayments(orderId, "payment");
        boolean cleared = false;
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unpaid = order.getTotalAmount().subtract(totalPaid);
            cleared = unpaid.compareTo(new BigDecimal("0.01")) <= 0;
        }
        purchaseOrderMapper.updatePaidAmount(orderId, totalPaid, cleared ? 1 : 0);
    }

    private void syncSaleReturnPaid(Integer orderId) {
        SaleReturn order = saleReturnMapper.selectById(orderId);
        if (order == null) return;
        BigDecimal totalPaid = sumPayments(orderId, "payment");
        boolean cleared = false;
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unpaid = order.getTotalAmount().subtract(totalPaid);
            cleared = unpaid.compareTo(new BigDecimal("0.01")) <= 0;
        }
        saleReturnMapper.updatePaidAmount(orderId, totalPaid, cleared ? 1 : 0);
    }

    private void syncPurchaseReturnPaid(Integer orderId) {
        PurchaseReturn order = purchaseReturnMapper.selectById(orderId);
        if (order == null) return;
        BigDecimal totalPaid = sumPayments(orderId, "receipt");
        boolean cleared = false;
        if (order.getTotalAmount() != null && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal unpaid = order.getTotalAmount().subtract(totalPaid);
            cleared = unpaid.compareTo(new BigDecimal("0.01")) <= 0;
        }
        purchaseReturnMapper.updatePaidAmount(orderId, totalPaid, cleared ? 1 : 0);
    }

    private void syncPaymentOrderPaid(Integer orderId, String orderType) {
        if ("purchase".equals(orderType)) {
            syncPurchaseOrderPaid(orderId);
        } else if ("sale_return".equals(orderType)) {
            syncSaleReturnPaid(orderId);
        } else if ("purchase_return".equals(orderType)) {
            syncPurchaseReturnPaid(orderId);
        } else {
            syncSaleOrderPaid(orderId);
        }
    }

    private BigDecimal sumPayments(Integer orderId, String type) {
        List<Payment> payments = paymentMapper.selectAllByOrderId(orderId);
        return payments.stream()
                .filter(p -> type == null || type.equals(p.getType()))
                .map(Payment::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
