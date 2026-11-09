package com.project.stockwork.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.stockwork.common.CommonResult;
import com.project.stockwork.common.PageResult;
import com.project.stockwork.common.RequireRole;
import com.project.stockwork.dto.InBoundDto;
import com.project.stockwork.dto.OutBoundDto;
import com.project.stockwork.entity.StockBill;
import com.project.stockwork.entity.StockBatch;
import com.project.stockwork.entity.WarehouseMaterial;
import com.project.stockwork.mapper.StockBatchMapper;
import com.project.stockwork.mapper.WarehouseMaterialMapper;
import com.project.stockwork.service.StockService;
import com.project.stockwork.util.ExcelUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Api(tags = "库存管理")
@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@RequireRole({"BUSINESS", "ADMIN"})
public class StockController {

    private final StockService stockService;
    private final WarehouseMaterialMapper materialMapper;
    private final StockBatchMapper batchMapper;

    @ApiOperation("物料入库（必填批次号）")
    @PostMapping("/inBound")
    public CommonResult<StockBill> inBound(@RequestBody InBoundDto dto) {
        return CommonResult.ok(stockService.inBound(dto));
    }

    @ApiOperation("物料出库（不传批次则按 FIFO 自动分配）")
    @PostMapping("/outBound")
    public CommonResult<List<StockBill>> outBound(@RequestBody OutBoundDto dto) {
        return CommonResult.ok(stockService.outBound(dto));
    }

    @ApiOperation("FIFO 推荐批次")
    @GetMapping("/batch/fifo")
    public CommonResult<List<Map<String, Object>>> fifo(@RequestParam Long materialId) {
        return CommonResult.ok(stockService.fifoBatches(materialId));
    }

    @ApiOperation("库存单据列表")
    @GetMapping("/bill/list")
    public CommonResult<PageResult<Map<String, Object>>> billList(
            @RequestParam(required = false) Integer billType,
            @RequestParam(required = false) Long materialId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return CommonResult.ok(stockService.billList(billType, materialId, pageNum, pageSize));
    }

    @ApiOperation("作废库存单据")
    @PostMapping("/bill/void")
    public CommonResult<Void> voidBill(@RequestParam Long billId) {
        stockService.voidBill(billId);
        return CommonResult.ok();
    }

    @ApiOperation("呆滞库存预警列表")
    @GetMapping("/stagnant")
    public CommonResult<List<Map<String, Object>>> stagnant() {
        return CommonResult.ok(stockService.stagnantList());
    }

    @ApiOperation("配置物料呆滞阈值天数")
    @PostMapping("/stagnant/config")
    public CommonResult<Void> configStagnant(@RequestParam Long materialId, @RequestParam Integer day) {
        WarehouseMaterial material = materialMapper.selectById(materialId);
        if (material == null) {
            throw new com.project.stockwork.common.BizException("物料档案不存在");
        }
        if (day == null || day <= 0) {
            throw new com.project.stockwork.common.BizException("呆滞天数阈值必须大于 0");
        }
        material.setStagnantDay(day);
        materialMapper.updateById(material);
        return CommonResult.ok();
    }

    @ApiOperation("库存盘点")
    @PostMapping("/check")
    public CommonResult<Map<String, Object>> check(@RequestParam Long materialId, @RequestParam BigDecimal realNum) {
        return CommonResult.ok(stockService.checkStock(materialId, realNum));
    }

    @ApiOperation("库存台账导出（Excel）")
    @GetMapping("/export")
    public void exportLedger(@RequestParam(required = false) String baseSite,
                            HttpServletResponse response) {
        List<WarehouseMaterial> materials = materialMapper.selectList(new LambdaQueryWrapper<WarehouseMaterial>()
                .eq(baseSite != null && !baseSite.isEmpty(), WarehouseMaterial::getBaseSite, baseSite)
                .orderByAsc(WarehouseMaterial::getBaseSite)
                .orderByAsc(WarehouseMaterial::getId));
        List<StockBatch> batches = batchMapper.selectList(null);

        XSSFWorkbook workbook = ExcelUtil.create("库存台账",
                new String[]{"生产基地", "仓库", "物料编码", "物料名称", "物料类型", "总库存",
                        "库存下限", "库存上限", "库存状态", "批次号", "批次剩余"});
        Sheet sheet = workbook.getSheetAt(0);
        int rowIndex = 1;
        for (WarehouseMaterial material : materials) {
            String status = material.getTotalStock().compareTo(material.getStockMin()) < 0 ? "低于下限" : "正常";
            List<StockBatch> materialBatches = batches.stream()
                    .filter(b -> b.getWarehouseMaterialId().equals(material.getId()))
                    .collect(java.util.stream.Collectors.toList());
            if (materialBatches.isEmpty()) {
                ExcelUtil.fillRow(sheet, rowIndex++, material.getBaseSite(), material.getWarehouseName(),
                        material.getMaterialCode(), material.getMaterialName(), materialTypeName(material.getMaterialType()),
                        material.getTotalStock(), material.getStockMin(), material.getStockMax(), status, "", "");
            } else {
                for (StockBatch batch : materialBatches) {
                    ExcelUtil.fillRow(sheet, rowIndex++, material.getBaseSite(), material.getWarehouseName(),
                            material.getMaterialCode(), material.getMaterialName(), materialTypeName(material.getMaterialType()),
                            material.getTotalStock(), material.getStockMin(), material.getStockMax(), status,
                            batch.getBatchNo(), batch.getRemainStock());
                }
            }
        }
        ExcelUtil.write(response, workbook,
                "库存台账_" + (baseSite == null || baseSite.isEmpty() ? "全部基地" : baseSite)
                        + "_" + DateUtil.today() + ".xlsx");
    }

    private String materialTypeName(Integer type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case 1:
                return "原材料";
            case 2:
                return "半成品";
            case 3:
                return "成品";
            default:
                return "";
        }
    }
}
