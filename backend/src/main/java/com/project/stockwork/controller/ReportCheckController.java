package com.project.stockwork.controller;

import com.project.stockwork.common.CommonResult;
import com.project.stockwork.common.PageResult;
import com.project.stockwork.common.RequireRole;
import com.project.stockwork.dto.AuditDto;
import com.project.stockwork.service.WorkOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Api(tags = "工单质检")
@RestController
@RequestMapping("/api/reportCheck")
@RequiredArgsConstructor
@RequireRole({"BUSINESS", "ADMIN"})
public class ReportCheckController {

    private final WorkOrderService workOrderService;

    @ApiOperation("待质检工单列表")
    @GetMapping("/todo")
    public CommonResult<PageResult<Map<String, Object>>> todo(
            @RequestParam(required = false) String baseSite,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return CommonResult.ok(workOrderService.todoList(baseSite, pageNum, pageSize));
    }

    @ApiOperation("质检审核（合格/不合格退回）")
    @PostMapping("/audit")
    public CommonResult<Void> audit(@RequestBody AuditDto dto) {
        workOrderService.audit(dto);
        return CommonResult.ok();
    }
}
