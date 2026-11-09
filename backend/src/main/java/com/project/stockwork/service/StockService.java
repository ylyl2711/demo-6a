package com.project.stockwork.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.stockwork.common.BizException;
import com.project.stockwork.common.PageResult;
import com.project.stockwork.dto.InBoundDto;
import com.project.stockwork.dto.OutBoundDto;
import com.project.stockwork.entity.StockBatch;
import com.project.stockwork.entity.StockBill;
import com.project.stockwork.entity.WarehouseMaterial;
import com.project.stockwork.mapper.StockBatchMapper;
import com.project.stockwork.mapper.StockBillMapper;
import com.project.stockwork.mapper.WarehouseMaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final WarehouseMaterialMapper materialMapper;
    private final StockBatchMapper batchMapper;
    private final StockBillMapper billMapper;
    private final OperLogService operLogService;

    @Transactional
    public StockBill inBound(InBoundDto dto) {
        WarehouseMaterial material = requireMaterial(dto.getMaterialId());
        if (dto.getBatchNo() == null || dto.getBatchNo().trim().isEmpty()) {
            throw new BizException("入库必须填写物料批次号");
        }
        BigDecimal num = requirePositive(dto.getOperNum(), "入库数量");
        String batchNo = dto.getBatchNo().trim();
        Date now = new Date();

        StockBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getWarehouseMaterialId, material.getId())
                .eq(StockBatch::getBatchNo, batchNo));
        if (batch == null) {
            batch = new StockBatch();
            batch.setWarehouseMaterialId(material.getId());
            batch.setBatchNo(batchNo);
            batch.setRemainStock(num);
            batch.setCreateTime(now);
            batch.setLastIoTime(now);
            batchMapper.insert(batch);
        } else {
            batch.setRemainStock(batch.getRemainStock().add(num));
            batch.setLastIoTime(now);
            batchMapper.updateById(batch);
        }

        materialMapper.update(null, new LambdaUpdateWrapper<WarehouseMaterial>()
                .setSql("total_stock = total_stock + " + num.toPlainString())
                .eq(WarehouseMaterial::getId, material.getId()));

        StockBill bill = new StockBill();
        bill.setBillNo("RK" + DateUtil.format(now, "yyyyMMddHHmmss") + RandomUtil.randomNumbers(3));
        bill.setBillType(1);
        bill.setWarehouseMaterialId(material.getId());
        bill.setBatchId(batch.getId());
        bill.setOperNum(num);
        bill.setOperUser(operatorName());
        bill.setBillTime(now);
        bill.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        bill.setWorkOrderId(dto.getWorkOrderId());
        bill.setBillStatus(0);
        billMapper.insert(bill);

        operLogService.log("仓储管理", String.format("物料入库：%s（%s）批次 %s，数量 %s",
                material.getMaterialName(), material.getBaseSite(), batchNo, num.toPlainString()));
        return bill;
    }

    @Transactional
    public List<StockBill> outBound(OutBoundDto dto) {
        WarehouseMaterial material = requireMaterial(dto.getMaterialId());
        BigDecimal num = requirePositive(dto.getOperNum(), "出库数量");
        if (material.getTotalStock().compareTo(num) < 0) {
            throw new BizException("物料总库存不足，当前库存 " + material.getTotalStock().toPlainString());
        }

        List<StockBill> result = new ArrayList<>();
        if (dto.getBatchId() != null) {
            StockBatch batch = batchMapper.selectById(dto.getBatchId());
            if (batch == null || !batch.getWarehouseMaterialId().equals(material.getId())) {
                throw new BizException("出库批次与物料不匹配");
            }
            deductBatch(batch, num);
            result.add(createBill(material, batch, num, dto));
        } else {
            List<StockBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                    .eq(StockBatch::getWarehouseMaterialId, material.getId())
                    .gt(StockBatch::getRemainStock, BigDecimal.ZERO)
                    .orderByAsc(StockBatch::getCreateTime));
            BigDecimal available = batches.stream().map(StockBatch::getRemainStock)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (available.compareTo(num) < 0) {
                throw new BizException("批次可用库存不足，FIFO 可用 " + available.toPlainString());
            }
            BigDecimal remain = num;
            for (StockBatch batch : batches) {
                if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal take = batch.getRemainStock().min(remain);
                deductBatch(batch, take);
                result.add(createBill(material, batch, take, dto));
                remain = remain.subtract(take);
            }
        }

        int updated = materialMapper.update(null, new LambdaUpdateWrapper<WarehouseMaterial>()
                .setSql("total_stock = total_stock - " + num.toPlainString())
                .eq(WarehouseMaterial::getId, material.getId())
                .ge(WarehouseMaterial::getTotalStock, num));
        if (updated == 0) {
            throw new BizException("物料总库存不足，出库失败");
        }

        operLogService.log("仓储管理", String.format("物料出库：%s（%s）FIFO 数量 %s%s",
                material.getMaterialName(), material.getBaseSite(), num.toPlainString(),
                dto.getWorkOrderId() == null ? "" : "，关联工单 " + dto.getWorkOrderId()));
        return result;
    }

    private void deductBatch(StockBatch batch, BigDecimal num) {
        int rows = batchMapper.update(null, new LambdaUpdateWrapper<StockBatch>()
                .setSql("remain_stock = remain_stock - " + num.toPlainString())
                .set(StockBatch::getLastIoTime, new Date())
                .eq(StockBatch::getId, batch.getId())
                .ge(StockBatch::getRemainStock, num));
        if (rows == 0) {
            throw new BizException("批次 " + batch.getBatchNo() + " 库存不足，无法出库");
        }
    }

    private StockBill createBill(WarehouseMaterial material, StockBatch batch, BigDecimal num, OutBoundDto dto) {
        StockBill bill = new StockBill();
        bill.setBillNo("CK" + DateUtil.format(new Date(), "yyyyMMddHHmmss") + RandomUtil.randomNumbers(3));
        bill.setBillType(2);
        bill.setWarehouseMaterialId(material.getId());
        bill.setBatchId(batch.getId());
        bill.setOperNum(num);
        bill.setOperUser(operatorName());
        bill.setBillTime(new Date());
        bill.setRemark(dto.getRemark() == null ? "" : dto.getRemark());
        bill.setWorkOrderId(dto.getWorkOrderId());
        bill.setBillStatus(0);
        billMapper.insert(bill);
        return bill;
    }

    public List<Map<String, Object>> fifoBatches(Long materialId) {
        requireMaterial(materialId);
        List<StockBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getWarehouseMaterialId, materialId)
                .gt(StockBatch::getRemainStock, BigDecimal.ZERO)
                .orderByAsc(StockBatch::getCreateTime));
        List<Map<String, Object>> list = new ArrayList<>();
        for (StockBatch b : batches) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("batchNo", b.getBatchNo());
            m.put("remainStock", b.getRemainStock());
            m.put("createTime", b.getCreateTime());
            m.put("lastIoTime", b.getLastIoTime());
            m.put("days", DateUtil.betweenDay(b.getCreateTime(), new Date(), true));
            list.add(m);
        }
        return list;
    }

    @Transactional
    public void voidBill(Long billId) {
        StockBill bill = billMapper.selectById(billId);
        if (bill == null) {
            throw new BizException("单据不存在");
        }
        if (bill.getBillStatus() != null && bill.getBillStatus() == 1) {
            throw new BizException("单据已作废，不能重复作废");
        }
        WarehouseMaterial material = requireMaterial(bill.getWarehouseMaterialId());
        StockBatch batch = batchMapper.selectById(bill.getBatchId());
        if (batch == null) {
            throw new BizException("单据关联批次不存在");
        }
        if (bill.getBillType() == 1) {
            if (batch.getRemainStock().compareTo(bill.getOperNum()) < 0) {
                throw new BizException("批次库存已被消耗，无法作废该入库单");
            }
            batchMapper.update(null, new LambdaUpdateWrapper<StockBatch>()
                    .setSql("remain_stock = remain_stock - " + bill.getOperNum().toPlainString())
                    .set(StockBatch::getLastIoTime, new Date())
                    .eq(StockBatch::getId, batch.getId()));
            materialMapper.update(null, new LambdaUpdateWrapper<WarehouseMaterial>()
                    .setSql("total_stock = total_stock - " + bill.getOperNum().toPlainString())
                    .eq(WarehouseMaterial::getId, material.getId()));
        } else {
            batchMapper.update(null, new LambdaUpdateWrapper<StockBatch>()
                    .setSql("remain_stock = remain_stock + " + bill.getOperNum().toPlainString())
                    .set(StockBatch::getLastIoTime, new Date())
                    .eq(StockBatch::getId, batch.getId()));
            materialMapper.update(null, new LambdaUpdateWrapper<WarehouseMaterial>()
                    .setSql("total_stock = total_stock + " + bill.getOperNum().toPlainString())
                    .eq(WarehouseMaterial::getId, material.getId()));
        }
        bill.setBillStatus(1);
        billMapper.updateById(bill);
        operLogService.log("仓储管理", "单据作废：" + bill.getBillNo());
    }

    public List<Map<String, Object>> stagnantList() {
        List<WarehouseMaterial> materials = materialMapper.selectList(null);
        List<Map<String, Object>> list = new ArrayList<>();
        Date now = new Date();
        for (WarehouseMaterial material : materials) {
            List<StockBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                    .eq(StockBatch::getWarehouseMaterialId, material.getId())
                    .gt(StockBatch::getRemainStock, BigDecimal.ZERO));
            if (batches.isEmpty()) {
                continue;
            }
            StockBatch oldest = batches.stream()
                    .min(Comparator.comparing(StockBatch::getLastIoTime))
                    .orElse(null);
            long days = DateUtil.betweenDay(oldest.getLastIoTime(), now, true);
            int threshold = material.getStagnantDay() == null ? 90 : material.getStagnantDay();
            if (days > threshold) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("materialId", material.getId());
                m.put("warehouseName", material.getWarehouseName());
                m.put("baseSite", material.getBaseSite());
                m.put("materialCode", material.getMaterialCode());
                m.put("materialName", material.getMaterialName());
                m.put("batchNo", oldest.getBatchNo());
                m.put("remainStock", oldest.getRemainStock());
                m.put("lastIoTime", oldest.getLastIoTime());
                m.put("stagnantDay", threshold);
                m.put("days", days);
                list.add(m);
            }
        }
        list.sort((a, b) -> Long.compare((Long) b.get("days"), (Long) a.get("days")));
        return list;
    }

    @Transactional
    public Map<String, Object> checkStock(Long materialId, BigDecimal realNum) {
        WarehouseMaterial material = requireMaterial(materialId);
        if (realNum == null || realNum.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("实盘数量不能小于 0");
        }
        BigDecimal before = material.getTotalStock();
        BigDecimal diff = realNum.subtract(before);

        material.setTotalStock(realNum);
        materialMapper.updateById(material);

        List<StockBatch> batches = batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getWarehouseMaterialId, materialId)
                .orderByAsc(StockBatch::getCreateTime));
        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            if (batches.isEmpty()) {
                StockBatch batch = new StockBatch();
                batch.setWarehouseMaterialId(materialId);
                batch.setBatchNo("PD" + DateUtil.format(new Date(), "yyyyMMddHHmmss"));
                batch.setRemainStock(diff);
                batch.setCreateTime(new Date());
                batch.setLastIoTime(new Date());
                batchMapper.insert(batch);
            } else {
                StockBatch first = batches.get(0);
                first.setRemainStock(first.getRemainStock().add(diff));
                first.setLastIoTime(new Date());
                batchMapper.updateById(first);
            }
        } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
            BigDecimal remain = diff.negate();
            for (StockBatch batch : batches) {
                if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                BigDecimal take = batch.getRemainStock().min(remain);
                batch.setRemainStock(batch.getRemainStock().subtract(take));
                batch.setLastIoTime(new Date());
                batchMapper.updateById(batch);
                remain = remain.subtract(take);
            }
        }

        operLogService.log("仓储管理", String.format("库存盘点：%s，账面 %s → 实盘 %s，差异 %s",
                material.getMaterialName(), before.toPlainString(), realNum.toPlainString(), diff.toPlainString()));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("materialName", material.getMaterialName());
        result.put("before", before);
        result.put("realNum", realNum);
        result.put("diff", diff);
        return result;
    }

    public PageResult<Map<String, Object>> billList(Integer billType, Long materialId, int pageNum, int pageSize) {
        LambdaQueryWrapper<StockBill> wrapper = new LambdaQueryWrapper<StockBill>()
                .eq(billType != null, StockBill::getBillType, billType)
                .eq(materialId != null, StockBill::getWarehouseMaterialId, materialId)
                .orderByDesc(StockBill::getBillTime);
        Page<StockBill> page = billMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, WarehouseMaterial> materials = materialMap();
        Map<Long, StockBatch> batches = batchMap();
        List<Map<String, Object>> list = page.getRecords().stream()
                .map(bill -> toBillView(bill, materials, batches))
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), list);
    }

    public List<Map<String, Object>> warningList() {
        List<WarehouseMaterial> materials = materialMapper.selectList(null);
        return materials.stream()
                .filter(m -> m.getTotalStock().compareTo(m.getStockMin()) < 0)
                .map(m -> {
                    Map<String, Object> view = new LinkedHashMap<>();
                    view.put("materialId", m.getId());
                    view.put("warehouseName", m.getWarehouseName());
                    view.put("baseSite", m.getBaseSite());
                    view.put("materialCode", m.getMaterialCode());
                    view.put("materialName", m.getMaterialName());
                    view.put("totalStock", m.getTotalStock());
                    view.put("stockMin", m.getStockMin());
                    return view;
                })
                .collect(Collectors.toList());
    }

    public Map<Long, WarehouseMaterial> materialMap() {
        return materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(WarehouseMaterial::getId, m -> m));
    }

    public Map<Long, StockBatch> batchMap() {
        return batchMapper.selectList(null).stream()
                .collect(Collectors.toMap(StockBatch::getId, b -> b));
    }

    public Map<String, Object> toBillView(StockBill bill, Map<Long, WarehouseMaterial> materials,
                                          Map<Long, StockBatch> batches) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", bill.getId());
        m.put("billNo", bill.getBillNo());
        m.put("billType", bill.getBillType());
        m.put("operNum", bill.getOperNum());
        m.put("operUser", bill.getOperUser());
        m.put("billTime", bill.getBillTime());
        m.put("remark", bill.getRemark());
        m.put("workOrderId", bill.getWorkOrderId());
        m.put("billStatus", bill.getBillStatus());
        WarehouseMaterial material = materials.get(bill.getWarehouseMaterialId());
        if (material != null) {
            m.put("materialCode", material.getMaterialCode());
            m.put("materialName", material.getMaterialName());
            m.put("baseSite", material.getBaseSite());
            m.put("warehouseName", material.getWarehouseName());
        }
        StockBatch batch = batches.get(bill.getBatchId());
        m.put("batchNo", batch == null ? "" : batch.getBatchNo());
        return m;
    }

    private WarehouseMaterial requireMaterial(Long materialId) {
        WarehouseMaterial material = materialId == null ? null : materialMapper.selectById(materialId);
        if (material == null) {
            throw new BizException("物料档案不存在");
        }
        return material;
    }

    private BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(field + "必须大于 0");
        }
        return value;
    }

    private String operatorName() {
        String name = com.project.stockwork.common.UserContext.name();
        return name.isEmpty() ? "system" : name;
    }
}
