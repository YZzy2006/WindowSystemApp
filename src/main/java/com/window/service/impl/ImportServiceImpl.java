package com.window.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.window.dto.*;
import com.window.entity.*;
import com.window.exception.OrderNoExistsException;
import com.window.mapper.*;
import com.window.service.*;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 统一批量导入：预览（校验+查重，只读）+ 提交（逐条独立事务）。
 * 每条记录由后端权威判定 created / skipped / failed，前端只做展示。
 * 模块 key 与 frontend/src/utils/importConfigs.js 一致。
 */
@Service
@RequiredArgsConstructor
public class ImportServiceImpl implements ImportService {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    private final SaleOrderMapper saleOrderMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SaleReturnMapper saleReturnMapper;
    private final PurchaseReturnMapper purchaseReturnMapper;
    private final StockInMapper stockInMapper;
    private final StockOutMapper stockOutMapper;
    private final CustomerMapper customerMapper;
    private final CommodityMapper commodityMapper;

    private final SaleOrderService saleOrderService;
    private final PurchaseOrderService purchaseOrderService;
    private final SaleReturnService saleReturnService;
    private final PurchaseReturnService purchaseReturnService;
    private final StockInService stockInService;
    private final StockOutService stockOutService;
    private final CustomerService customerService;
    private final SupplierService supplierService;
    private final CommodityService commodityService;
    private final PaymentService paymentService;

    private final Map<String, Handler> HANDLERS = new HashMap<>();

    private record Handler(Class<?> dtoType,
                           Function<Object, String> keyExtractor,
                           Predicate<String> existsChecker,
                           Consumer<Object> saveExecutor) {
    }

    @PostConstruct
    void init() {
        HANDLERS.put("saleOrders", new Handler(SaleOrderSaveDto.class,
                d -> ((SaleOrderSaveDto) d).getOrderNo(),
                key -> saleOrderMapper.countByOrderNo(key) > 0,
                d -> {
                    SaleOrderSaveDto s = (SaleOrderSaveDto) d;
                    SaleOrder order = new SaleOrder();
                    order.setOrderNo(s.getOrderNo());
                    order.setOrderType(s.getOrderType());
                    order.setCustomerId(s.getCustomerId());
                    order.setCustomerName(s.getCustomerName());
                    order.setCustomerPhone(s.getCustomerPhone());
                    order.setCustomerAddress(s.getCustomerAddress());
                    order.setOrderDate(s.getOrderDate());
                    order.setDeposit(s.getDeposit());
                    order.setStatus(s.getStatus());
                    order.setRemark(s.getRemark());
                    order.setNotice(s.getNotice());
                    order.setHiddenProductTypes(s.getHiddenProductTypes());
                    saleOrderService.save(order, toSaleOrderItems(s.getItems()));
                }));

        HANDLERS.put("purchaseOrders", new Handler(PurchaseOrderSaveDto.class,
                d -> ((PurchaseOrderSaveDto) d).getOrderNo(),
                key -> purchaseOrderMapper.countByOrderNo(key) > 0,
                d -> {
                    PurchaseOrderSaveDto s = (PurchaseOrderSaveDto) d;
                    PurchaseOrder order = new PurchaseOrder();
                    order.setOrderNo(s.getOrderNo());
                    order.setSupplierId(s.getSupplierId());
                    order.setSupplierName(s.getSupplierName());
                    order.setSupplierContact(s.getSupplierContact());
                    order.setSupplierPhone(s.getSupplierPhone());
                    order.setSupplierAddress(s.getSupplierAddress());
                    order.setOrderDate(s.getOrderDate());
                    order.setStatus(s.getStatus());
                    order.setRemark(s.getRemark());
                    purchaseOrderService.save(order, toPurchaseOrderItems(s.getItems()));
                }));

        HANDLERS.put("saleReturns", new Handler(SaleReturnSaveDto.class,
                d -> ((SaleReturnSaveDto) d).getOrderNo(),
                key -> saleReturnMapper.countByOrderNo(key) > 0,
                d -> {
                    SaleReturnSaveDto s = (SaleReturnSaveDto) d;
                    SaleReturn sr = new SaleReturn();
                    sr.setOrderNo(s.getOrderNo());
                    sr.setOriginalOrderNo(s.getOriginalOrderNo());
                    sr.setCustomerId(s.getCustomerId());
                    sr.setCustomerName(s.getCustomerName());
                    sr.setReturnDate(s.getReturnDate());
                    sr.setStatus(s.getStatus());
                    sr.setRemark(s.getRemark());
                    saleReturnService.save(sr, toSaleReturnItems(s.getItems()));
                }));

        HANDLERS.put("purchaseReturns", new Handler(PurchaseReturnSaveDto.class,
                d -> ((PurchaseReturnSaveDto) d).getOrderNo(),
                key -> purchaseReturnMapper.countByOrderNo(key) > 0,
                d -> {
                    PurchaseReturnSaveDto s = (PurchaseReturnSaveDto) d;
                    PurchaseReturn pr = new PurchaseReturn();
                    pr.setOrderNo(s.getOrderNo());
                    pr.setOriginalOrderNo(s.getOriginalOrderNo());
                    pr.setSupplierId(s.getSupplierId());
                    pr.setSupplierName(s.getSupplierName());
                    pr.setReturnDate(s.getReturnDate());
                    pr.setStatus(s.getStatus());
                    pr.setRemark(s.getRemark());
                    purchaseReturnService.save(pr, toPurchaseReturnItems(s.getItems()));
                }));

        HANDLERS.put("stockIn", new Handler(StockInSaveDto.class,
                d -> ((StockInSaveDto) d).getOrderNo(),
                key -> stockInMapper.countByOrderNo(key) > 0,
                d -> {
                    StockInSaveDto s = (StockInSaveDto) d;
                    StockIn si = new StockIn();
                    si.setOrderNo(s.getOrderNo());
                    si.setOrderDate(s.getOrderDate());
                    si.setApplicant(s.getApplicant());
                    si.setWarehouseKeeper(s.getWarehouseKeeper());
                    si.setOperator(s.getOperator());
                    si.setRemark(s.getRemark());
                    stockInService.save(si, toStockInItems(s.getItems()));
                }));

        HANDLERS.put("stockOut", new Handler(StockOutSaveDto.class,
                d -> ((StockOutSaveDto) d).getOrderNo(),
                key -> stockOutMapper.countByOrderNo(key) > 0,
                d -> {
                    StockOutSaveDto s = (StockOutSaveDto) d;
                    StockOut so = new StockOut();
                    so.setOrderNo(s.getOrderNo());
                    so.setOrderDate(s.getOrderDate());
                    so.setApplicant(s.getApplicant());
                    so.setWarehouseKeeper(s.getWarehouseKeeper());
                    so.setOperator(s.getOperator());
                    so.setRemark(s.getRemark());
                    stockOutService.save(so, toStockOutItems(s.getItems()));
                }));

        HANDLERS.put("customers", new Handler(Customer.class,
                d -> ((Customer) d).getName(),
                key -> customerMapper.countByName(key) > 0,
                d -> customerService.save((Customer) d)));

        HANDLERS.put("suppliers", new Handler(Supplier.class,
                d -> null,
                key -> false,
                d -> supplierService.save((Supplier) d)));

        HANDLERS.put("commodities", new Handler(Commodity.class,
                d -> ((Commodity) d).getCode(),
                key -> commodityMapper.countByCode(key) > 0,
                d -> commodityService.save((Commodity) d)));

        HANDLERS.put("payments", new Handler(Payment.class,
                d -> null,
                key -> false,
                d -> paymentService.save((Payment) d)));
    }

    @Override
    public ImportResult preview(String module, List<Map<String, Object>> records) {
        Handler h = requireHandler(module);
        ImportResult result = new ImportResult();
        result.setTotal(records == null ? 0 : records.size());
        if (records == null) return result;
        Set<String> seenKeys = new HashSet<>();
        int i = 1;
        for (Map<String, Object> rec : records) {
            Object dto;
            try {
                dto = objectMapper.convertValue(rec, h.dtoType());
            } catch (Exception e) {
                result.getResults().add(new ImportResult.RecordResult(i, null, "invalid", "数据格式错误：" + e.getMessage()));
                result.setInvalid(result.getInvalid() + 1);
                i++;
                continue;
            }
            String violation = validate(dto);
            if (violation != null) {
                result.getResults().add(new ImportResult.RecordResult(i, null, "invalid", violation));
                result.setInvalid(result.getInvalid() + 1);
                i++;
                continue;
            }
            String key = h.keyExtractor().apply(dto);
            // 批内重复 + 库内已存在 → 均视为"已存在"，与 commit 行为一致
            boolean exists = key != null && (seenKeys.contains(key) || h.existsChecker().test(key));
            if (key != null) seenKeys.add(key);
            if (exists) {
                result.getResults().add(new ImportResult.RecordResult(i, key, "exists", "已存在，将跳过"));
                result.setSkipped(result.getSkipped() + 1);
            } else {
                result.getResults().add(new ImportResult.RecordResult(i, key, "new", "可导入"));
                result.setCreated(result.getCreated() + 1);
            }
            i++;
        }
        return result;
    }

    @Override
    public ImportResult commit(String module, List<Map<String, Object>> records) {
        Handler h = requireHandler(module);
        ImportResult result = new ImportResult();
        result.setTotal(records == null ? 0 : records.size());
        if (records == null) return result;
        int i = 1;
        for (Map<String, Object> rec : records) {
            Object dto;
            try {
                dto = objectMapper.convertValue(rec, h.dtoType());
            } catch (Exception e) {
                result.getResults().add(new ImportResult.RecordResult(i, null, "failed", "数据格式错误：" + e.getMessage()));
                result.setFailed(result.getFailed() + 1);
                i++;
                continue;
            }
            String violation = validate(dto);
            if (violation != null) {
                result.getResults().add(new ImportResult.RecordResult(i, null, "failed", violation));
                result.setFailed(result.getFailed() + 1);
                i++;
                continue;
            }
            String key = h.keyExtractor().apply(dto);
            try {
                h.saveExecutor().accept(dto);
                result.getResults().add(new ImportResult.RecordResult(i, key, "created", "导入成功"));
                result.setCreated(result.getCreated() + 1);
            } catch (OrderNoExistsException e) {
                result.getResults().add(new ImportResult.RecordResult(i, key, "skipped", e.getMessage()));
                result.setSkipped(result.getSkipped() + 1);
            } catch (DataIntegrityViolationException e) {
                String msg = e.getMessage();
                boolean dup = msg != null && (msg.contains("Duplicate")
                        || msg.contains("Unique index or primary key violation")
                        || msg.contains("already exists"));
                if (dup) {
                    result.getResults().add(new ImportResult.RecordResult(i, key, "skipped", "已存在，已跳过"));
                    result.setSkipped(result.getSkipped() + 1);
                } else {
                    result.getResults().add(new ImportResult.RecordResult(i, key, "failed", "数据约束冲突，操作无法完成"));
                    result.setFailed(result.getFailed() + 1);
                }
            } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
                result.getResults().add(new ImportResult.RecordResult(i, key, "failed", e.getMessage()));
                result.setFailed(result.getFailed() + 1);
            } catch (Exception e) {
                result.getResults().add(new ImportResult.RecordResult(i, key, "failed", "服务器内部错误"));
                result.setFailed(result.getFailed() + 1);
            }
            i++;
        }
        return result;
    }

    private Handler requireHandler(String module) {
        Handler h = HANDLERS.get(module);
        if (h == null) throw new IllegalArgumentException("未知的导入模块：" + module);
        return h;
    }

    private String validate(Object dto) {
        Set<ConstraintViolation<Object>> violations = validator.validate(dto);
        if (violations.isEmpty()) return null;
        return violations.stream().findFirst().map(ConstraintViolation::getMessage).orElse("数据校验不通过");
    }

    private List<SaleOrderItem> toSaleOrderItems(List<SaleOrderSaveDto.SaleOrderItemDto> list) {
        if (list == null) return new ArrayList<>();
        return list.stream().map(dto -> {
            SaleOrderItem item = new SaleOrderItem();
            item.setCommodityId(dto.getCommodityId());
            item.setProductName(dto.getProductName());
            item.setSeries(dto.getSeries());
            item.setColor(dto.getColor());
            item.setProductType(dto.getProductType());
            item.setUnit(dto.getUnit());
            item.setWidth(dto.getWidth());
            item.setHeight(dto.getHeight());
            item.setWallThickness(dto.getWallThickness());
            item.setGlassType(dto.getGlassType());
            item.setLockPosition(dto.getLockPosition());
            item.setDoorCount(dto.getDoorCount());
            item.setDiaojiao(dto.getDiaojiao());
            item.setFangCount(dto.getFangCount());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setCost(dto.getCost());
            item.setMaterialCost(dto.getMaterialCost());
            item.setLaborCost(dto.getLaborCost());
            item.setAccessoryCost(dto.getAccessoryCost());
            item.setExtraFee(dto.getExtraFee());
            item.setFormulaId(dto.getFormulaId());
            item.setFormulaSnapshot(dto.getFormulaSnapshot());
            item.setImage(dto.getImage());
            item.setRemark(dto.getRemark());
            return item;
        }).collect(Collectors.toList());
    }

    private List<PurchaseOrderItem> toPurchaseOrderItems(List<PurchaseOrderSaveDto.PurchaseOrderItemDto> list) {
        if (list == null) return new ArrayList<>();
        return list.stream().map(dto -> {
            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setCommodityId(dto.getCommodityId());
            item.setProductName(dto.getProductName());
            item.setProductCategory(dto.getProductCategory());
            item.setSpec(dto.getSpec());
            item.setUnit(dto.getUnit());
            item.setWarehouseLoc(dto.getWarehouseLoc());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setRemark(dto.getRemark());
            return item;
        }).collect(Collectors.toList());
    }

    private List<SaleReturnItem> toSaleReturnItems(List<SaleReturnSaveDto.SaleReturnItemDto> list) {
        if (list == null) return new ArrayList<>();
        return list.stream().map(dto -> {
            SaleReturnItem item = new SaleReturnItem();
            item.setCommodityId(dto.getCommodityId());
            item.setProductName(dto.getProductName());
            item.setProductCategory(dto.getProductCategory());
            item.setSpec(dto.getSpec());
            item.setUnit(dto.getUnit());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setRemark(dto.getRemark());
            return item;
        }).collect(Collectors.toList());
    }

    private List<PurchaseReturnItem> toPurchaseReturnItems(List<PurchaseReturnSaveDto.PurchaseReturnItemDto> list) {
        if (list == null) return new ArrayList<>();
        return list.stream().map(dto -> {
            PurchaseReturnItem item = new PurchaseReturnItem();
            item.setCommodityId(dto.getCommodityId());
            item.setProductName(dto.getProductName());
            item.setProductCategory(dto.getProductCategory());
            item.setSpec(dto.getSpec());
            item.setUnit(dto.getUnit());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setRemark(dto.getRemark());
            return item;
        }).collect(Collectors.toList());
    }

    private List<StockInItem> toStockInItems(List<StockInSaveDto.StockInItemDto> list) {
        if (list == null) return new ArrayList<>();
        return list.stream().map(dto -> {
            StockInItem item = new StockInItem();
            item.setCommodityId(dto.getCommodityId());
            item.setProductName(dto.getProductName());
            item.setProductCategory(dto.getProductCategory());
            item.setSpec(dto.getSpec());
            item.setUnit(dto.getUnit());
            item.setWarehouseLoc(dto.getWarehouseLoc());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setRemark(dto.getRemark());
            return item;
        }).collect(Collectors.toList());
    }

    private List<StockOutItem> toStockOutItems(List<StockOutSaveDto.StockOutItemDto> list) {
        if (list == null) return new ArrayList<>();
        return list.stream().map(dto -> {
            StockOutItem item = new StockOutItem();
            item.setCommodityId(dto.getCommodityId());
            item.setProductName(dto.getProductName());
            item.setProductCategory(dto.getProductCategory());
            item.setSpec(dto.getSpec());
            item.setUnit(dto.getUnit());
            item.setWarehouseLoc(dto.getWarehouseLoc());
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(dto.getUnitPrice());
            item.setRemark(dto.getRemark());
            return item;
        }).collect(Collectors.toList());
    }
}
