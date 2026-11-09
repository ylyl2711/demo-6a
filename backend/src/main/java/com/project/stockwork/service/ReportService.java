package com.project.stockwork.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.stockwork.common.BizException;
import com.project.stockwork.common.UserContext;
import com.project.stockwork.dto.ReportSubmitDto;
import com.project.stockwork.entity.ProductionWorkOrder;
import com.project.stockwork.entity.SysDict;
import com.project.stockwork.entity.WorkOrderProcess;
import com.project.stockwork.entity.WorkReportCheck;
import com.project.stockwork.mapper.ProductionWorkOrderMapper;
import com.project.stockwork.mapper.SysDictMapper;
import com.project.stockwork.mapper.WorkOrderProcessMapper;
import com.project.stockwork.mapper.WorkReportCheckMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ProductionWorkOrderMapper orderMapper;
    private final WorkOrderProcessMapper processMapper;
    private final WorkReportCheckMapper reportMapper;
    private final SysDictMapper dictMapper;
    private final WorkOrderService workOrderService;
    private final OperLogService operLogService;

    public List<Map<String, Object>> myOrders(Integer workStatus) {
        Long userId = UserContext.require().getId();
        List<ProductionWorkOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getOperatorUserId, userId)
                .eq(workStatus != null, ProductionWorkOrder::getWorkStatus, workStatus)
                .orderByDesc(ProductionWorkOrder::getCreateTime));
        Map<Long, com.project.stockwork.entity.WarehouseMaterial> materials = workOrderService.materialMap();
        Map<Long, com.project.stockwork.entity.SysUser> users = workOrderService.userMap();
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProductionWorkOrder order : orders) {
            Map<String, Object> view = workOrderService.orderView(order, materials, users);
            List<WorkOrderProcess> processes = workOrderService.processesOf(order.getId());
            List<WorkReportCheck> reports = workOrderService.reportsOf(order.getId());
            view.put("totalProcess", processes.size());
            view.put("doneProcess", processes.stream().filter(p -> p.getProcessStatus() == 2).count());
            view.put("goodTotal", reports.stream()
                    .map(r -> r.getGoodNum() == null ? BigDecimal.ZERO : r.getGoodNum())
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            view.put("badTotal", reports.stream()
                    .map(r -> r.getBadNum() == null ? BigDecimal.ZERO : r.getBadNum())
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            list.add(view);
        }
        return list;
    }

    public Map<String, Object> orderDetail(Long orderId) {
        ProductionWorkOrder order = requireOwnOrder(orderId);
        Map<String, Object> view = workOrderService.detail(order.getId());
        view.put("badReasons", badReasonOptions());
        return view;
    }

    public List<Map<String, Object>> badReasonOptions() {
        List<SysDict> dicts = dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, "bad_reason")
                .orderByAsc(SysDict::getSort));
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysDict dict : dicts) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", dict.getId());
            m.put("label", dict.getDictLabel());
            m.put("value", dict.getDictValue());
            list.add(m);
        }
        return list;
    }

    @Transactional
    public void submit(ReportSubmitDto dto) {
        ProductionWorkOrder order = requireOwnOrder(dto.getWorkOrderId());
        if (order.getWorkStatus() == WorkOrderService.STATUS_CHECK || order.getWorkStatus() == WorkOrderService.STATUS_DONE) {
            throw new BizException("工单已提交完工，报工记录已锁定，不能修改");
        }
        WorkOrderProcess process = dto.getProcessId() == null ? null : processMapper.selectById(dto.getProcessId());
        if (process == null || !process.getWorkOrderId().equals(order.getId())) {
            throw new BizException("工序不存在或不属于该工单");
        }
        BigDecimal good = dto.getGoodNum() == null ? BigDecimal.ZERO : dto.getGoodNum();
        BigDecimal bad = dto.getBadNum() == null ? BigDecimal.ZERO : dto.getBadNum();
        if (good.compareTo(BigDecimal.ZERO) < 0 || bad.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException("良品/不良品数量不能为负数");
        }
        if (good.compareTo(BigDecimal.ZERO) == 0 && bad.compareTo(BigDecimal.ZERO) == 0) {
            throw new BizException("请填写良品或不良品数量");
        }
        if (bad.compareTo(BigDecimal.ZERO) > 0 && dto.getBadReasonId() == null) {
            throw new BizException("不良品数量大于 0 时必须选择不良原因");
        }
        if (process.getProcessSeq() > 1) {
            List<WorkOrderProcess> processes = workOrderService.processesOf(order.getId());
            for (WorkOrderProcess p : processes) {
                if (p.getProcessSeq() < process.getProcessSeq() && p.getProcessStatus() != 2) {
                    throw new BizException("请按顺序先完成工序：" + p.getProcessName());
                }
            }
        }

        WorkReportCheck exist = reportMapper.selectOne(new LambdaQueryWrapper<WorkReportCheck>()
                .eq(WorkReportCheck::getWorkOrderId, order.getId())
                .eq(WorkReportCheck::getProcessId, process.getId()));
        Date now = new Date();
        if (exist != null) {
            exist.setGoodNum(good);
            exist.setBadNum(bad);
            exist.setBadReasonDictId(bad.compareTo(BigDecimal.ZERO) > 0 ? dto.getBadReasonId() : null);
            exist.setPhotoUrl(dto.getPhotoUrl() == null ? "" : dto.getPhotoUrl());
            exist.setReportTime(now);
            exist.setCheckResult(0);
            reportMapper.updateById(exist);
        } else {
            WorkReportCheck report = new WorkReportCheck();
            report.setWorkOrderId(order.getId());
            report.setProcessId(process.getId());
            report.setProcessName(process.getProcessName());
            report.setGoodNum(good);
            report.setBadNum(bad);
            report.setBadReasonDictId(bad.compareTo(BigDecimal.ZERO) > 0 ? dto.getBadReasonId() : null);
            report.setPhotoUrl(dto.getPhotoUrl() == null ? "" : dto.getPhotoUrl());
            report.setReportTime(now);
            report.setCheckResult(0);
            report.setCheckUser("");
            report.setCheckRemark("");
            reportMapper.insert(report);
        }

        process.setProcessStatus(2);
        processMapper.updateById(process);

        if (order.getWorkStatus() == WorkOrderService.STATUS_WAIT) {
            order.setWorkStatus(WorkOrderService.STATUS_PRODUCING);
            orderMapper.updateById(order);
        }
        operLogService.log("生产报工", "工序报工：" + order.getWorkOrderNo() + " / " + process.getProcessName()
                + "，良品 " + good.toPlainString() + "，不良 " + bad.toPlainString());
    }

    @Transactional
    public void finish(Long workOrderId) {
        ProductionWorkOrder order = requireOwnOrder(workOrderId);
        if (order.getWorkStatus() == WorkOrderService.STATUS_CHECK) {
            throw new BizException("工单已提交完工，等待质检");
        }
        if (order.getWorkStatus() == WorkOrderService.STATUS_DONE) {
            throw new BizException("工单已完成，无需重复提交");
        }
        List<WorkOrderProcess> processes = workOrderService.processesOf(order.getId());
        if (processes.isEmpty()) {
            throw new BizException("工单没有工序数据");
        }
        List<String> unfinished = new ArrayList<>();
        for (WorkOrderProcess p : processes) {
            if (p.getProcessStatus() != 2) {
                unfinished.add(p.getProcessName());
            }
        }
        if (!unfinished.isEmpty()) {
            throw new BizException("以下工序尚未报工：" + String.join("、", unfinished));
        }
        List<WorkReportCheck> reports = workOrderService.reportsOf(order.getId());
        if (reports.isEmpty()) {
            throw new BizException("没有报工记录，无法提交完工");
        }
        order.setWorkStatus(WorkOrderService.STATUS_CHECK);
        orderMapper.updateById(order);
        operLogService.log("生产报工", "提交完工申请：" + order.getWorkOrderNo());
    }

    private ProductionWorkOrder requireOwnOrder(Long orderId) {
        ProductionWorkOrder order = workOrderService.requireOrder(orderId);
        if (order.getOperatorUserId() == null || !order.getOperatorUserId().equals(UserContext.require().getId())) {
            throw new BizException(403, "只能操作分配给自己的工单");
        }
        return order;
    }
}
