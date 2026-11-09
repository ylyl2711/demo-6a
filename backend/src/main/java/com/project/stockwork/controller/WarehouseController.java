package com.project.stockwork.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.stockwork.common.BizException;
import com.project.stockwork.common.CommonResult;
import com.project.stockwork.common.PageResult;
import com.project.stockwork.common.RequireRole;
import com.project.stockwork.entity.StockBatch;
import com.project.stockwork.entity.WarehouseMaterial;
import com.project.stockwork.mapper.StockBatchMapper;
import com.project.stockwork.mapper.WarehouseMaterialMapper;
import com.project.stockwork.service.OperLogService;
import com.project.stockwork.service.StockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Api(tags = "仓库物料档案")
@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
@RequireRole({"BUSINESS", "ADMIN"})
public class WarehouseController {

    private final WarehouseMaterialMapper materialMapper;
    private final StockBatchMapper batchMapper;
    private final StockService stockService;
    private final OperLogService operLogService;

    @ApiOperation("仓库物料分页列表")
    @GetMapping("/list")
    public CommonResult<PageResult<WarehouseMaterial>> list(
            @RequestParam(required = false) String baseSite,
            @RequestParam(required = false) Integer materialType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<WarehouseMaterial> wrapper = new LambdaQueryWrapper<WarehouseMaterial>()
                .eq(baseSite != null && !baseSite.isEmpty(), WarehouseMaterial::getBaseSite, baseSite)
                .eq(materialType != null, WarehouseMaterial::getMaterialType, materialType)
                .and(keyword != null && !keyword.isEmpty(), w -> w
                        .like(WarehouseMaterial::getMaterialName, keyword)
                        .or().like(WarehouseMaterial::getMaterialCode, keyword))
                .orderByAsc(WarehouseMaterial::getBaseSite)
                .orderByAsc(WarehouseMaterial::getId);
        Page<WarehouseMaterial> page = materialMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return CommonResult.ok(new PageResult<>(page.getTotal(), page.getRecords()));
    }

    @ApiOperation("物料下拉选项")
    @GetMapping("/options")
    public CommonResult<List<WarehouseMaterial>> options(@RequestParam(required = false) String baseSite,
                                                        @RequestParam(required = false) Integer materialType) {
        return CommonResult.ok(materialMapper.selectList(new LambdaQueryWrapper<WarehouseMaterial>()
                .eq(baseSite != null && !baseSite.isEmpty(), WarehouseMaterial::getBaseSite, baseSite)
                .eq(materialType != null, WarehouseMaterial::getMaterialType, materialType)
                .orderByAsc(WarehouseMaterial::getId)));
    }

    @ApiOperation("新增/编辑仓库物料")
    @PostMapping("/save")
    public CommonResult<Void> save(@RequestBody WarehouseMaterial dto) {
        if (dto.getWarehouseName() == null || dto.getWarehouseName().trim().isEmpty()) {
            throw new BizException("仓库名称不能为空");
        }
        if (dto.getBaseSite() == null || dto.getBaseSite().trim().isEmpty()) {
            throw new BizException("请选择所属生产基地");
        }
        if (dto.getMaterialCode() == null || dto.getMaterialCode().trim().isEmpty()) {
            throw new BizException("物料编码不能为空");
        }
        if (dto.getMaterialName() == null || dto.getMaterialName().trim().isEmpty()) {
            throw new BizException("物料名称不能为空");
        }
        WarehouseMaterial sameCode = materialMapper.selectOne(new LambdaQueryWrapper<WarehouseMaterial>()
                .eq(WarehouseMaterial::getMaterialCode, dto.getMaterialCode().trim()));
        if (dto.getId() == null) {
            if (sameCode != null) {
                throw new BizException("物料编码已存在");
            }
            dto.setId(null);
            dto.setTotalStock(BigDecimal.ZERO);
            dto.setMaterialType(dto.getMaterialType() == null ? 1 : dto.getMaterialType());
            dto.setStockMin(dto.getStockMin() == null ? BigDecimal.ZERO : dto.getStockMin());
            dto.setStockMax(dto.getStockMax() == null ? BigDecimal.ZERO : dto.getStockMax());
            dto.setStagnantDay(dto.getStagnantDay() == null ? 90 : dto.getStagnantDay());
            materialMapper.insert(dto);
            operLogService.log("仓储管理", "新增物料档案：" + dto.getMaterialName());
        } else {
            WarehouseMaterial exist = materialMapper.selectById(dto.getId());
            if (exist == null) {
                throw new BizException("物料档案不存在");
            }
            if (sameCode != null && !sameCode.getId().equals(exist.getId())) {
                throw new BizException("物料编码已存在");
            }
            exist.setWarehouseName(dto.getWarehouseName().trim());
            exist.setBaseSite(dto.getBaseSite().trim());
            exist.setMaterialCode(dto.getMaterialCode().trim());
            exist.setMaterialName(dto.getMaterialName().trim());
            exist.setMaterialType(dto.getMaterialType() == null ? exist.getMaterialType() : dto.getMaterialType());
            exist.setStockMin(dto.getStockMin() == null ? exist.getStockMin() : dto.getStockMin());
            exist.setStockMax(dto.getStockMax() == null ? exist.getStockMax() : dto.getStockMax());
            exist.setStagnantDay(dto.getStagnantDay() == null ? exist.getStagnantDay() : dto.getStagnantDay());
            materialMapper.updateById(exist);
            operLogService.log("仓储管理", "编辑物料档案：" + exist.getMaterialName());
        }
        return CommonResult.ok();
    }

    @ApiOperation("删除仓库物料")
    @PostMapping("/delete")
    public CommonResult<Void> delete(@RequestParam Long id) {
        WarehouseMaterial material = materialMapper.selectById(id);
        if (material == null) {
            throw new BizException("物料档案不存在");
        }
        if (material.getTotalStock().compareTo(BigDecimal.ZERO) > 0) {
            throw new BizException("该物料仍有库存，不能删除");
        }
        Long batches = batchMapper.selectCount(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getWarehouseMaterialId, id));
        if (batches != null && batches > 0) {
            throw new BizException("该物料存在批次记录，不能删除");
        }
        materialMapper.deleteById(id);
        operLogService.log("仓储管理", "删除物料档案：" + material.getMaterialName());
        return CommonResult.ok();
    }

    @ApiOperation("库存预警列表（低于下限）")
    @GetMapping("/warning")
    public CommonResult<List<Map<String, Object>>> warning() {
        return CommonResult.ok(stockService.warningList());
    }
}
