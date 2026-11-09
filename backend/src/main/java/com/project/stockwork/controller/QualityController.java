package com.project.stockwork.controller;

import com.project.stockwork.common.CommonResult;
import com.project.stockwork.common.RequireRole;
import com.project.stockwork.service.QualityService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Api(tags = "质量统计与报告")
@RestController
@RequestMapping("/api/quality")
@RequiredArgsConstructor
@RequireRole({"BUSINESS", "ADMIN"})
public class QualityController {

    private final QualityService qualityService;

    @ApiOperation("不良品分类统计")
    @GetMapping("/stat")
    public CommonResult<Map<String, Object>> stat(@RequestParam(required = false) String startDate,
                                                  @RequestParam(required = false) String endDate,
                                                  @RequestParam(required = false) String baseSite) {
        return CommonResult.ok(qualityService.stat(startDate, endDate, baseSite));
    }

    @ApiOperation("不良统计导出 Excel")
    @GetMapping("/stat/export")
    public void exportStat(@RequestParam(required = false) String startDate,
                           @RequestParam(required = false) String endDate,
                           @RequestParam(required = false) String baseSite,
                           HttpServletResponse response) {
        qualityService.exportStat(response, startDate, endDate, baseSite);
    }

    @ApiOperation("质检报告导出（仅合格工单）")
    @GetMapping("/report/export")
    public void exportReport(@RequestParam Long orderId, HttpServletResponse response) {
        qualityService.exportReport(response, orderId);
    }
}
