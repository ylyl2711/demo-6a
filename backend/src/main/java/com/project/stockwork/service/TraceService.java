package com.project.stockwork.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.stockwork.common.BizException;
import com.project.stockwork.entity.ProductionWorkOrder;
import com.project.stockwork.entity.StockBatch;
import com.project.stockwork.entity.StockBill;
import com.project.stockwork.entity.WarehouseMaterial;
import com.project.stockwork.mapper.ProductionWorkOrderMapper;
import com.project.stockwork.mapper.StockBatchMapper;
import com.project.stockwork.mapper.StockBillMapper;
import com.project.stockwork.mapper.WarehouseMaterialMapper;
import com.project.stockwork.util.ExcelUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TraceService {

    private final StockBatchMapper batchMapper;
    private final StockBillMapper billMapper;
    private final WarehouseMaterialMapper materialMapper;
    private final ProductionWorkOrderMapper orderMapper;

    private static final String[] STATUS_LABEL = {"待生产", "生产中", "待质检", "已完成"};

    public Map<String, Object> traceForward(String batchNo) {
        StockBatch batch = findBatch(batchNo);
        WarehouseMaterial material = materialMapper.selectById(batch.getWarehouseMaterialId());
        List<StockBill> bills = billMapper.selectList(new LambdaQueryWrapper<StockBill>()
                .eq(StockBill::getBatchId, batch.getId())
                .eq(StockBill::getBillType, 2)
                .eq(StockBill::getBillStatus, 0)
                .orderByAsc(StockBill::getBillTime));

        List<Map<String, Object>> orders = new ArrayList<>();
        for (StockBill bill : bills) {
            if (bill.getWorkOrderId() == null) {
                continue;
            }
            ProductionWorkOrder order = orderMapper.selectById(bill.getWorkOrderId());
            if (order == null) {
                continue;
            }
            WarehouseMaterial product = materialMapper.selectById(order.getMaterialId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("workOrderNo", order.getWorkOrderNo());
            m.put("baseSite", order.getBaseSite());
            m.put("productName", product == null ? "" : product.getMaterialName());
            m.put("planNum", order.getPlanNum());
            m.put("productBatch", order.getProductBatch());
            m.put("workStatus", order.getWorkStatus());
            m.put("workStatusLabel", statusLabel(order.getWorkStatus()));
            m.put("useNum", bill.getOperNum());
            m.put("billTime", bill.getBillTime());
            orders.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchNo", batch.getBatchNo());
        result.put("materialName", material == null ? "" : material.getMaterialName());
        result.put("materialCode", material == null ? "" : material.getMaterialCode());
        result.put("baseSite", material == null ? "" : material.getBaseSite());
        result.put("remainStock", batch.getRemainStock());
        result.put("lastIoTime", batch.getLastIoTime());
        result.put("orders", orders);
        return result;
    }

    public Map<String, Object> traceBackward(String productBatch) {
        if (productBatch == null || productBatch.trim().isEmpty()) {
            throw new BizException("请输入成品批次号");
        }
        List<ProductionWorkOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getProductBatch, productBatch.trim()));
        if (orders.isEmpty()) {
            throw new BizException("未找到该成品批次对应的生产工单，请检查批次号");
        }

        List<Map<String, Object>> materials = new ArrayList<>();
        List<Map<String, Object>> orderViews = new ArrayList<>();
        for (ProductionWorkOrder order : orders) {
            WarehouseMaterial product = materialMapper.selectById(order.getMaterialId());
            Map<String, Object> ov = new LinkedHashMap<>();
            ov.put("workOrderNo", order.getWorkOrderNo());
            ov.put("baseSite", order.getBaseSite());
            ov.put("productName", product == null ? "" : product.getMaterialName());
            ov.put("planNum", order.getPlanNum());
            ov.put("workStatusLabel", statusLabel(order.getWorkStatus()));
            ov.put("createTime", order.getCreateTime());
            orderViews.add(ov);

            List<StockBill> bills = billMapper.selectList(new LambdaQueryWrapper<StockBill>()
                    .eq(StockBill::getWorkOrderId, order.getId())
                    .eq(StockBill::getBillType, 2)
                    .eq(StockBill::getBillStatus, 0)
                    .orderByAsc(StockBill::getBillTime));
            for (StockBill bill : bills) {
                StockBatch batch = batchMapper.selectById(bill.getBatchId());
                WarehouseMaterial material = bill.getWarehouseMaterialId() == null
                        ? null : materialMapper.selectById(bill.getWarehouseMaterialId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("workOrderNo", order.getWorkOrderNo());
                m.put("materialName", material == null ? "" : material.getMaterialName());
                m.put("materialCode", material == null ? "" : material.getMaterialCode());
                m.put("batchNo", batch == null ? "" : batch.getBatchNo());
                m.put("operNum", bill.getOperNum());
                m.put("billTime", bill.getBillTime());
                materials.add(m);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productBatch", productBatch.trim());
        result.put("orders", orderViews);
        result.put("materials", materials);
        return result;
    }

    public void exportForward(HttpServletResponse response, String batchNo) {
        Map<String, Object> data = traceForward(batchNo);
        XSSFWorkbook workbook = ExcelUtil.create("原料批次正向追溯",
                new String[]{"工单编号", "生产基地", "产出成品", "计划数量", "成品批次", "工单状态", "领用数量", "领料时间"});
        Sheet sheet = workbook.getSheetAt(0);
        List<Map<String, Object>> orders = (List<Map<String, Object>>) data.get("orders");
        int rowIndex = 1;
        for (Map<String, Object> order : orders) {
            ExcelUtil.fillRow(sheet, rowIndex++, order.get("workOrderNo"), order.get("baseSite"),
                    order.get("productName"), order.get("planNum"), order.get("productBatch"),
                    order.get("workStatusLabel"), order.get("useNum"), order.get("billTime"));
        }
        ExcelUtil.write(response, workbook, "原料批次正向追溯_" + batchNo + ".xlsx");
    }

    public void exportBackward(HttpServletResponse response, String productBatch) {
        Map<String, Object> data = traceBackward(productBatch);
        XSSFWorkbook workbook = ExcelUtil.create("成品批次反向溯源",
                new String[]{"工单编号", "原材料名称", "物料编码", "原材料批次", "耗用数量", "领料时间"});
        Sheet sheet = workbook.getSheetAt(0);
        List<Map<String, Object>> materials = (List<Map<String, Object>>) data.get("materials");
        int rowIndex = 1;
        for (Map<String, Object> material : materials) {
            ExcelUtil.fillRow(sheet, rowIndex++, material.get("workOrderNo"), material.get("materialName"),
                    material.get("materialCode"), material.get("batchNo"),
                    material.get("operNum"), material.get("billTime"));
        }
        ExcelUtil.write(response, workbook, "成品批次反向溯源_" + productBatch + ".xlsx");
    }

    private StockBatch findBatch(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            throw new BizException("请输入原材料批次号");
        }
        StockBatch batch = batchMapper.selectOne(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getBatchNo, batchNo.trim()));
        if (batch == null) {
            throw new BizException("批次号不存在：" + batchNo);
        }
        return batch;
    }

    private String statusLabel(Integer status) {
        if (status == null || status < 0 || status >= STATUS_LABEL.length) {
            return "";
        }
        return STATUS_LABEL[status];
    }
}
