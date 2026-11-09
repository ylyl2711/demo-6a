package com.project.stockwork.controller;

import com.project.stockwork.common.CommonResult;
import com.project.stockwork.common.RequireRole;
import com.project.stockwork.service.TraceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Api(tags = "批次追溯")
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
@RequireRole({"BUSINESS", "ADMIN"})
public class BatchController {

    private final TraceService traceService;

    @ApiOperation("原材料批次正向追溯")
    @GetMapping("/traceForward")
    public CommonResult<Map<String, Object>> traceForward(@RequestParam String batchNo) {
        return CommonResult.ok(traceService.traceForward(batchNo));
    }

    @ApiOperation("成品批次反向溯源")
    @GetMapping("/traceBackward")
    public CommonResult<Map<String, Object>> traceBackward(@RequestParam String productBatch) {
        return CommonResult.ok(traceService.traceBackward(productBatch));
    }

    @ApiOperation("正向追溯结果导出")
    @GetMapping("/traceForward/export")
    public void exportForward(@RequestParam String batchNo, HttpServletResponse response) {
        traceService.exportForward(response, batchNo);
    }

    @ApiOperation("反向溯源结果导出")
    @GetMapping("/traceBackward/export")
    public void exportBackward(@RequestParam String productBatch, HttpServletResponse response) {
        traceService.exportBackward(response, productBatch);
    }
}
