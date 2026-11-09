package com.project.stockwork.controller;

import com.project.stockwork.common.CommonResult;
import com.project.stockwork.common.PageResult;
import com.project.stockwork.common.RequireRole;
import com.project.stockwork.dto.BomSaveDto;
import com.project.stockwork.dto.TplSaveDto;
import com.project.stockwork.dto.WorkOrderSaveDto;
import com.project.stockwork.entity.ProductionWorkOrder;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "生产工单")
@RestController
@RequestMapping("/api/workorder")
@RequiredArgsConstructor
@RequireRole({"BUSINESS", "ADMIN"})
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @ApiOperation("新增/编辑工单（自动复制工序模板）")
    @PostMapping("/save")
    public CommonResult<ProductionWorkOrder> save(@RequestBody WorkOrderSaveDto dto) {
        return CommonResult.ok(workOrderService.save(dto));
    }

    @ApiOperation("工单列表（进度监控）")
    @GetMapping("/list")
    public CommonResult<PageResult<Map<String, Object>>> list(
            @RequestParam(required = false) String baseSite,
            @RequestParam(required = false) Integer workStatus,
            @RequestParam(required = false) Long operatorUserId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return CommonResult.ok(workOrderService.list(baseSite, workStatus, operatorUserId, pageNum, pageSize));
    }

    @ApiOperation("工单详情（含工序与报工）")
    @GetMapping("/detail")
    public CommonResult<Map<String, Object>> detail(@RequestParam Long id) {
        return CommonResult.ok(workOrderService.detail(id));
    }

    @ApiOperation("工单进度与报工照片")
    @GetMapping("/progress")
    public CommonResult<Map<String, Object>> progress(@RequestParam Long id) {
        return CommonResult.ok(workOrderService.progress(id));
    }

    @ApiOperation("BOM 列表")
    @GetMapping("/bom")
    public CommonResult<List<Map<String, Object>>> bom(@RequestParam Long productMaterialId) {
        return CommonResult.ok(workOrderService.bomList(productMaterialId));
    }

    @ApiOperation("保存 BOM（整单覆盖）")
    @PostMapping("/bom/save")
    public CommonResult<Void> saveBom(@RequestBody BomSaveDto dto) {
        workOrderService.bomSave(dto);
        return CommonResult.ok();
    }

    @ApiOperation("工序模板列表")
    @GetMapping("/template")
    public CommonResult<List<Map<String, Object>>> template(@RequestParam Long materialId) {
        return CommonResult.ok(workOrderService.tplList(materialId));
    }

    @ApiOperation("保存工序模板（整单覆盖）")
    @PostMapping("/template/save")
    public CommonResult<Void> saveTemplate(@RequestBody TplSaveDto dto) {
        workOrderService.tplSave(dto);
        return CommonResult.ok();
    }

    @ApiOperation("工单表单基础选项")
    @GetMapping("/options")
    public CommonResult<Map<String, Object>> options() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("baseSites", workOrderService.baseSiteOptions());
        result.put("products", workOrderService.productOptions());
        result.put("operators", workOrderService.operatorOptions());
        return CommonResult.ok(result);
    }
}
