// com/window/controller/PublicController.java
package com.window.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.window.common.PublicRateLimiter;
import com.window.common.SensitiveWordFilter;
import com.window.dto.EnquiryDto;
import com.window.dto.Result;
import com.window.entity.Case;
import com.window.entity.Category;
import com.window.entity.Enquiry;
import com.window.entity.Product;
import com.window.service.CaseService;
import com.window.service.CategoryService;
import com.window.service.EnquiryService;
import com.window.service.ProductService;
import com.window.service.SiteConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PublicController {

    private final CategoryService categoryService;
    private final ProductService productService;
    private final CaseService caseService;
    private final EnquiryService enquiryService;
    private final ObjectMapper objectMapper;
    private final SiteConfigService siteConfigService;
    private final PublicRateLimiter publicRateLimiter;

    /** 探活 */
    @GetMapping("/ping")
    public Result ping() {
        return Result.success("pong");
    }

    /** 分类列表（按sort升序） */
    @GetMapping("/categories")
    public Result categories() {
        List<Category> list = categoryService.list();
        return Result.success(list);
    }

    /** 前台产品列表（仅已上架，可选分类筛选），images转数组、specs转Map */
    @GetMapping("/products")
    public Result products(@RequestParam(required = false) Integer categoryId) {
        List<Product> list = productService.listByCategory(categoryId);
        List<Map<String, Object>> result = list.stream().map(p -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", p.getId());
            map.put("categoryId", p.getCategoryId());
            map.put("name", p.getName());
            map.put("model", p.getModel());
            map.put("material", p.getMaterial());
            map.put("price", p.getPrice());
            map.put("coverImage", p.getCoverImage());
            map.put("description", p.getDescription());

            // images: 逗号分隔 → 数组，每项 trim
            if (p.getImages() != null && !p.getImages().isEmpty()) {
                List<String> imgList = Arrays.stream(p.getImages().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                map.put("images", imgList);
            } else {
                map.put("images", Collections.emptyList());
            }

            // specs: JSON字符串 → Map，解析失败返回空Map
            map.put("specs", parseSpecs(p.getSpecs()));
            map.put("priceTag", p.getPriceTag());
            map.put("priceBreakdown", p.getPriceBreakdown());
            map.put("pricingRule", p.getPricingRule());
            map.put("referencePrice", p.getReferencePrice());
            map.put("optionPrices", p.getOptionPrices());
            map.put("performance", p.getPerformance());
            map.put("isShow", p.getIsShow());
            map.put("createTime", p.getCreateTime());
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    /** 产品详情，images转数组、specs转Map */
    @GetMapping("/products/{id}")
    public Result productDetail(@PathVariable Integer id) {
        Product p = productService.getById(id);
        if (p == null) {
            return Result.error(404, "产品不存在");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("categoryId", p.getCategoryId());
        map.put("name", p.getName());
        map.put("model", p.getModel());
        map.put("material", p.getMaterial());
        map.put("price", p.getPrice());
        map.put("coverImage", p.getCoverImage());
        map.put("description", p.getDescription());

        if (p.getImages() != null && !p.getImages().isEmpty()) {
            List<String> imgList = Arrays.stream(p.getImages().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            map.put("images", imgList);
        } else {
            map.put("images", Collections.emptyList());
        }

        map.put("specs", parseSpecs(p.getSpecs()));
        map.put("priceTag", p.getPriceTag());
        map.put("priceBreakdown", p.getPriceBreakdown());
        map.put("pricingRule", p.getPricingRule());
        map.put("referencePrice", p.getReferencePrice());
        map.put("optionPrices", p.getOptionPrices());
        map.put("performance", p.getPerformance());
        map.put("isShow", p.getIsShow());
        map.put("createTime", p.getCreateTime());
        return Result.success(map);
    }

    /** 案例列表（仅展示启用的案例，按 sort → 时间排序），images 转数组 */
    @GetMapping("/cases")
    public Result cases() {
        List<Case> list = caseService.listVisible();
        List<Map<String, Object>> result = list.stream().map(c -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", c.getId());
            map.put("title", c.getTitle());
            map.put("image", c.getImage());
            // images: 逗号分隔 → 数组
            if (c.getImages() != null && !c.getImages().isEmpty()) {
                List<String> imgList = Arrays.stream(c.getImages().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                map.put("images", imgList);
            } else {
                map.put("images", Collections.emptyList());
            }
            map.put("description", c.getDescription());
            map.put("productIds", parseIds(c.getProductIds()));
            map.put("location", c.getLocation());
            map.put("productType", c.getProductType());
            map.put("profileSpec", c.getProfileSpec());
            map.put("glassConfig", c.getGlassConfig());
            map.put("hardwareBrand", c.getHardwareBrand());
            map.put("beforeImage", c.getBeforeImage());
            // beforeImages
            if (c.getBeforeImages() != null && !c.getBeforeImages().isEmpty()) {
                List<String> biList = Arrays.stream(c.getBeforeImages().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
                map.put("beforeImages", biList);
            } else { map.put("beforeImages", Collections.emptyList()); }
            map.put("beforeDesc", c.getBeforeDesc());
            map.put("afterDesc", c.getAfterDesc());
            map.put("customerNeed", c.getCustomerNeed());
            map.put("customerReview", c.getCustomerReview());
            map.put("installDuration", c.getInstallDuration());
            map.put("createTime", c.getCreateTime());
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    /** 案例详情 */
    @GetMapping("/cases/{id}")
    public Result caseDetail(@PathVariable Integer id) {
        Case c = caseService.getById(id);
        if (c == null) return Result.error(404, "案例不存在");
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", c.getId());
        map.put("title", c.getTitle());
        map.put("image", c.getImage());
        map.put("description", c.getDescription());
        map.put("productIds", parseIds(c.getProductIds()));
        map.put("location", c.getLocation());
        map.put("productType", c.getProductType());
        map.put("profileSpec", c.getProfileSpec());
        map.put("glassConfig", c.getGlassConfig());
        map.put("hardwareBrand", c.getHardwareBrand());
        map.put("beforeImage", c.getBeforeImage());
        map.put("beforeDesc", c.getBeforeDesc());
        map.put("afterDesc", c.getAfterDesc());
        map.put("customerNeed", c.getCustomerNeed());
        map.put("customerReview", c.getCustomerReview());
        map.put("installDuration", c.getInstallDuration());
        map.put("createTime", c.getCreateTime());
        // 图片列表
        if (c.getImages() != null && !c.getImages().isEmpty()) {
            map.put("images", Arrays.stream(c.getImages().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        } else { map.put("images", Collections.emptyList()); }
        if (c.getBeforeImages() != null && !c.getBeforeImages().isEmpty()) {
            map.put("beforeImages", Arrays.stream(c.getBeforeImages().split(",")).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        } else { map.put("beforeImages", Collections.emptyList()); }
        return Result.success(map);
    }

    /** 提交询价 */
    @PostMapping("/enquiries")
    public Result submitEnquiry(@Valid @RequestBody EnquiryDto dto, HttpServletRequest request) {
        // IP限流：10分钟内最多5次提交
        String ip = getClientIp(request);
        if (!publicRateLimiter.allowEnquiry(ip)) {
            return Result.error(429, "提交过于频繁，请稍后再试");
        }

        // 姓名 + 内容敏感词过滤（后端兜底，使用增强版过滤器）
        String err = SensitiveWordFilter.check(dto.getName());
        if (err == null) err = SensitiveWordFilter.check(dto.getContent());
        if (err != null) return Result.error(400, err);

        Enquiry enquiry = new Enquiry();
        enquiry.setName(dto.getName());
        enquiry.setPhone(dto.getPhone());
        enquiry.setContent(dto.getContent());
        enquiry.setIsRead(0);
        enquiry.setNeedMeasure(dto.getNeedMeasure() != null ? dto.getNeedMeasure() : 0);
        enquiry.setBudget(dto.getBudget());
        enquiry.setCreateTime(LocalDateTime.now());
        enquiryService.save(enquiry);
        return Result.success("提交成功，我们会尽快联系您");
    }

    private String getClientIp(HttpServletRequest request) {
        // 优先使用 getRemoteAddr()（不可伪造），X-Forwarded-For 可被客户端篡改
        String ip = request.getRemoteAddr();
        if (ip != null && !ip.isEmpty()) return ip;
        // 仅当 getRemoteAddr() 为空时回退到 header（正常不会发生）
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty()) return ip;
        ip = request.getHeader("X-Forwarded-For");
        return (ip != null && !ip.isEmpty()) ? ip.split(",")[0].trim() : "unknown";
    }

    /** 解析逗号分隔的产品ID字符串 → 整数列表 */
    private List<Integer> parseIds(String ids) {
        if (ids == null || ids.trim().isEmpty()) return Collections.emptyList();
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSpecs(String specs) {
        if (specs == null || specs.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(specs, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    @GetMapping("/site-config")
    public Result siteConfig() {
        return Result.success(siteConfigService.get().getConfigJson());
    }

}
