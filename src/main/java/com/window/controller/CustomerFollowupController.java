package com.window.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.window.dto.Result;
import com.window.entity.CustomerFollowup;
import com.window.service.CustomerFollowupService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/customer-followups")
@RequiredArgsConstructor
public class CustomerFollowupController {

    private final CustomerFollowupService customerFollowupService;

    @GetMapping
    public Result list(@RequestParam Integer customerId,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "50") int size) {
        return Result.success(customerFollowupService.listByCustomerId(customerId, new Page<>(page, size)));
    }

    @PostMapping
    public Result add(@RequestBody CustomerFollowup followup, HttpServletRequest request) {
        if (followup.getCustomerId() == null || followup.getContent() == null || followup.getContent().trim().isEmpty()) {
            return Result.error(400, "客户ID和跟进内容不能为空");
        }
        String adminId = (String) request.getAttribute("adminId");
        String adminUsername = (String) request.getAttribute("adminUsername");
        if (adminId != null) followup.setAdminId(Integer.valueOf(adminId));
        followup.setUsername(adminUsername != null ? adminUsername : "");
        followup.setContent(followup.getContent().trim());
        followup.setCreateTime(LocalDateTime.now());
        customerFollowupService.save(followup);
        return Result.success("添加成功");
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Integer id) {
        customerFollowupService.removeById(id);
        return Result.success("删除成功");
    }
}
