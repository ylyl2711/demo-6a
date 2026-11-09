package com.project.stockwork.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.stockwork.common.BizException;
import com.project.stockwork.entity.ProductionWorkOrder;
import com.project.stockwork.entity.SysDict;
import com.project.stockwork.entity.SysUser;
import com.project.stockwork.entity.WarehouseMaterial;
import com.project.stockwork.entity.WorkOrderProcess;
import com.project.stockwork.entity.WorkReportCheck;
import com.project.stockwork.mapper.ProductionWorkOrderMapper;
import com.project.stockwork.mapper.SysDictMapper;
import com.project.stockwork.util.ExcelUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QualityService {

    private final ProductionWorkOrderMapper orderMapper;
    private final SysDictMapper dictMapper;
    private final WorkOrderService workOrderService;

    public Map<String, Object> stat(String startDate, String endDate, String baseSite) {
        Date start = startDate == null || startDate.isEmpty()
                ? DateUtil.beginOfMonth(new Date()) : DateUtil.parse(startDate + " 00:00:00");
        Date end = endDate == null || endDate.isEmpty()
                ? DateUtil.endOfDay(new Date()) : DateUtil.parse(endDate + " 23:59:59");

        List<ProductionWorkOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ProductionWorkOrder>()
                .between(ProductionWorkOrder::getCreateTime, start, end)
                .eq(baseSite != null && !baseSite.isEmpty(), ProductionWorkOrder::getBaseSite, baseSite));
        List<Long> orderIds = orders.stream().map(ProductionWorkOrder::getId).collect(Collectors.toList());

        Map<String, BigDecimal> counts = new LinkedHashMap<>();
        BigDecimal totalGood = BigDecimal.ZERO;
        BigDecimal totalBad = BigDecimal.ZERO;
        if (!orderIds.isEmpty()) {
            List<WorkReportCheck> reports = workOrderService.reportsOf(orderIds);
            Map<Long, String> reasonMap = badReasonMap();
            for (WorkReportCheck report : reports) {
                BigDecimal good = report.getGoodNum() == null ? BigDecimal.ZERO : report.getGoodNum();
                BigDecimal bad = report.getBadNum() == null ? BigDecimal.ZERO : report.getBadNum();
                totalGood = totalGood.add(good);
                totalBad = totalBad.add(bad);
                if (bad.compareTo(BigDecimal.ZERO) > 0) {
                    String label = reasonMap.getOrDefault(report.getBadReasonDictId(), "未填写原因");
                    merge(counts, label, bad);
                }
                if (report.getCheckResult() != null && report.getCheckResult() == 2
                        && report.getCheckBadReasonId() != null) {
                    String label = reasonMap.getOrDefault(report.getCheckBadReasonId(), "未填写原因");
                    merge(counts, "质检-" + label, BigDecimal.ONE);
                }
            }
        }

        List<Map<String, Object>> items = counts.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("value", e.getValue());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("startDate", DateUtil.formatDate(start));
        result.put("endDate", DateUtil.formatDate(end));
        result.put("baseSite", baseSite == null ? "" : baseSite);
        result.put("orderCount", orders.size());
        result.put("totalGood", totalGood);
        result.put("totalBad", totalBad);
        result.put("items", items);
        return result;
    }

    private void merge(Map<String, BigDecimal> counts, String key, BigDecimal value) {
        counts.merge(key, value, BigDecimal::add);
    }

    private Map<Long, String> badReasonMap() {
        return dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                        .eq(SysDict::getDictType, "bad_reason"))
                .stream()
                .collect(Collectors.toMap(SysDict::getId, SysDict::getDictLabel, (a, b) -> a));
    }

    public void exportStat(HttpServletResponse response, String startDate, String endDate, String baseSite) {
        Map<String, Object> stat = stat(startDate, endDate, baseSite);
        XSSFWorkbook workbook = ExcelUtil.create("不良品分类统计",
                new String[]{"不良原因", "不良数量"});
        Sheet sheet = workbook.getSheetAt(0);
        int rowIndex = 1;
        List<Map<String, Object>> items = (List<Map<String, Object>>) stat.get("items");
        for (Map<String, Object> item : items) {
            ExcelUtil.fillRow(sheet, rowIndex++, item.get("name"), item.get("value"));
        }
        if (items.isEmpty()) {
            ExcelUtil.fillRow(sheet, rowIndex++, "本期无不良记录", 0);
        }
        ExcelUtil.fillRow(sheet, rowIndex++, "合计不良", stat.get("totalBad"));
        ExcelUtil.write(response, workbook,
                "不良品分类统计_" + stat.get("startDate") + "_" + stat.get("endDate") + ".xlsx");
    }

    public void exportReport(HttpServletResponse response, Long orderId) {
        ProductionWorkOrder order = workOrderService.requireOrder(orderId);
        if (order.getWorkStatus() == null || order.getWorkStatus() != 3) {
            throw new BizException("只有质检合格的工单才能导出质检报告");
        }
        Map<Long, WarehouseMaterial> materials = workOrderService.materialMap();
        Map<Long, SysUser> users = workOrderService.userMap();
        WarehouseMaterial material = materials.get(order.getMaterialId());
        SysUser operator = order.getOperatorUserId() == null ? null : users.get(order.getOperatorUserId());
        List<WorkOrderProcess> processes = workOrderService.processesOf(orderId);
        List<WorkReportCheck> reports = workOrderService.reportsOf(orderId);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("质检报告");
        int[] widths = {18, 16, 16, 18, 22, 30, 14};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i] * 256);
        }

        ExcelUtil.fillRow(sheet, 0, "工单编号", order.getWorkOrderNo(),
                "成品名称", material == null ? "" : material.getMaterialName(),
                "成品批次", order.getProductBatch(), "");
        ExcelUtil.fillRow(sheet, 1, "生产基地", order.getBaseSite(),
                "计划数量", order.getPlanNum(),
                "操作工", operator == null ? "" : operator.getRealName(),
                "创建时间", DateUtil.formatDateTime(order.getCreateTime()));
        ExcelUtil.fillRow(sheet, 2, "质检人", reports.isEmpty() ? "" : reports.get(0).getCheckUser(),
                "质检时间", reports.isEmpty() || reports.get(0).getCheckTime() == null
                        ? "" : DateUtil.formatDateTime(reports.get(0).getCheckTime()),
                "质检结论", "合格", "质检备注",
                reports.isEmpty() ? "" : reports.get(0).getCheckRemark());

        ExcelUtil.fillRow(sheet, 4, "工序", "良品数量", "不良数量", "不良原因", "报工时间", "现场照片", "质检结果");
        int rowIndex = 5;
        for (WorkOrderProcess process : processes) {
            WorkReportCheck report = reports.stream()
                    .filter(r -> r.getProcessId() != null && r.getProcessId().equals(process.getId()))
                    .findFirst().orElse(null);
            ExcelUtil.fillRow(sheet, rowIndex++,
                    process.getProcessName(),
                    report == null ? "" : report.getGoodNum(),
                    report == null ? "" : report.getBadNum(),
                    report == null || report.getBadReasonDictId() == null ? "" : badReasonLabel(report.getBadReasonDictId()),
                    report == null ? "" : DateUtil.formatDateTime(report.getReportTime()),
                    report == null ? "" : report.getPhotoUrl(),
                    report == null ? "" : (report.getCheckResult() == 1 ? "合格" : "不合格"));
        }
        ExcelUtil.write(response, workbook, "质检报告_" + order.getWorkOrderNo() + ".xlsx");
    }

    private String badReasonLabel(Long id) {
        SysDict dict = dictMapper.selectById(id);
        return dict == null ? "" : dict.getDictLabel();
    }
}
