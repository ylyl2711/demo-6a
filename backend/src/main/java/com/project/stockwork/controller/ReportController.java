package com.project.stockwork.controller;

import com.project.stockwork.common.CommonResult;
import com.project.stockwork.common.RequireRole;
import com.project.stockwork.dto.ReportSubmitDto;
import com.project.stockwork.service.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "H5 生产报工")
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@RequireRole({"OPERATOR"})
public class ReportController {

    private final ReportService reportService;

    @ApiOperation("我的工单列表")
    @GetMapping("/myOrders")
    public CommonResult<List<Map<String, Object>>> myOrders(
            @RequestParam(required = false) Integer workStatus) {
        return CommonResult.ok(reportService.myOrders(workStatus));
    }

    @ApiOperation("工单详情（工序清单与报工记录）")
    @GetMapping("/detail")
    public CommonResult<Map<String, Object>> detail(@RequestParam Long orderId) {
        return CommonResult.ok(reportService.orderDetail(orderId));
    }

    @ApiOperation("工序报工（良品/不良/原因/照片）")
    @PostMapping("/submit")
    public CommonResult<Void> submit(@RequestBody ReportSubmitDto dto) {
        reportService.submit(dto);
        return CommonResult.ok();
    }

    @ApiOperation("提交完工申请")
    @PostMapping("/finish")
    public CommonResult<Void> finish(@RequestParam Long workOrderId) {
        reportService.finish(workOrderId);
        return CommonResult.ok();
    }

    @ApiOperation("不良原因选项")
    @GetMapping("/badReasons")
    public CommonResult<List<Map<String, Object>>> badReasons() {
        return CommonResult.ok(reportService.badReasonOptions());
    }
}
