package com.project.stockwork.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.stockwork.common.CommonResult;
import com.project.stockwork.entity.SysDict;
import com.project.stockwork.entity.WorkOrderProcess;
import com.project.stockwork.mapper.SysDictMapper;
import com.project.stockwork.mapper.WorkOrderProcessMapper;
import com.project.stockwork.service.ReportService;
import com.project.stockwork.service.StockService;
import com.project.stockwork.service.WorkOrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Api(tags = "公共基础数据")
@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final SysDictMapper dictMapper;
    private final WorkOrderService workOrderService;
    private final ReportService reportService;
    private final StockService stockService;
    private final WorkOrderProcessMapper processMapper;

    @ApiOperation("按类型查询字典")
    @GetMapping("/dict")
    public CommonResult<List<SysDict>> dict(@RequestParam String type) {
        List<SysDict> list = dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, type)
                .orderByAsc(SysDict::getSort));
        return CommonResult.ok(list);
    }

    @ApiOperation("生产基地选项")
    @GetMapping("/baseSite")
    public CommonResult<List<Map<String, Object>>> baseSite() {
        return CommonResult.ok(workOrderService.baseSiteOptions());
    }

    @ApiOperation("不良原因选项")
    @GetMapping("/badReason")
    public CommonResult<List<Map<String, Object>>> badReason() {
        return CommonResult.ok(reportService.badReasonOptions());
    }

    @ApiOperation("操作工选项")
    @GetMapping("/operator")
    public CommonResult<List<Map<String, Object>>> operator() {
        return CommonResult.ok(workOrderService.operatorOptions());
    }

    @ApiOperation("成品物料选项")
    @GetMapping("/product")
    public CommonResult<List<Map<String, Object>>> product() {
        return CommonResult.ok(workOrderService.productOptions());
    }

    @ApiOperation("库存预警列表")
    @GetMapping("/warning")
    public CommonResult<List<Map<String, Object>>> warning() {
        return CommonResult.ok(stockService.warningList());
    }
}
