package com.project.stockwork.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.stockwork.common.BizException;
import com.project.stockwork.common.PageResult;
import com.project.stockwork.common.UserContext;
import com.project.stockwork.dto.AuditDto;
import com.project.stockwork.dto.BomSaveDto;
import com.project.stockwork.dto.InBoundDto;
import com.project.stockwork.dto.TplSaveDto;
import com.project.stockwork.dto.WorkOrderSaveDto;
import com.project.stockwork.entity.MaterialBom;
import com.project.stockwork.entity.ProcessTemplate;
import com.project.stockwork.entity.ProductionWorkOrder;
import com.project.stockwork.entity.StockBatch;
import com.project.stockwork.entity.SysDict;
import com.project.stockwork.entity.SysRole;
import com.project.stockwork.entity.SysUser;
import com.project.stockwork.entity.WarehouseMaterial;
import com.project.stockwork.entity.WorkOrderProcess;
import com.project.stockwork.entity.WorkReportCheck;
import com.project.stockwork.mapper.MaterialBomMapper;
import com.project.stockwork.mapper.ProcessTemplateMapper;
import com.project.stockwork.mapper.ProductionWorkOrderMapper;
import com.project.stockwork.mapper.StockBatchMapper;
import com.project.stockwork.mapper.SysDictMapper;
import com.project.stockwork.mapper.SysRoleMapper;
import com.project.stockwork.mapper.SysUserMapper;
import com.project.stockwork.mapper.WarehouseMaterialMapper;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkOrderService {

    private final ProductionWorkOrderMapper orderMapper;
    private final WorkOrderProcessMapper processMapper;
    private final WorkReportCheckMapper reportMapper;
    private final ProcessTemplateMapper templateMapper;
    private final MaterialBomMapper bomMapper;
    private final WarehouseMaterialMapper materialMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDictMapper dictMapper;
    private final StockBatchMapper batchMapper;
    private final StockService stockService;
    private final OperLogService operLogService;

    /** 工单状态：0待生产 1生产中 2待质检 3已完成 */
    public static final int STATUS_WAIT = 0;
    public static final int STATUS_PRODUCING = 1;
    public static final int STATUS_CHECK = 2;
    public static final int STATUS_DONE = 3;

    @Transactional
    public ProductionWorkOrder save(WorkOrderSaveDto dto) {
        if (dto.getBaseSite() == null || dto.getBaseSite().isEmpty()) {
            throw new BizException("请选择生产基地");
        }
        WarehouseMaterial product = dto.getMaterialId() == null ? null : materialMapper.selectById(dto.getMaterialId());
        if (product == null) {
            throw new BizException("请选择生产产品");
        }
        if (dto.getPlanNum() == null || dto.getPlanNum().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("计划生产数量必须大于 0");
        }
        if (dto.getOperatorUserId() == null) {
            throw new BizException("请分配车间操作工");
        }
        SysUser operator = userMapper.selectById(dto.getOperatorUserId());
        SysRole role = operator == null ? null : roleMapper.selectById(operator.getRoleId());
        if (operator == null || role == null || !"OPERATOR".equals(role.getRoleCode())) {
            throw new BizException("分配的操作工账号不存在或角色不正确");
        }

        ProductionWorkOrder order;
        if (dto.getId() == null) {
            order = new ProductionWorkOrder();
            order.setWorkOrderNo(genOrderNo());
            order.setCreateTime(new Date());
            order.setWorkStatus(STATUS_WAIT);
            order.setProductBatch(dto.getProductBatch() == null || dto.getProductBatch().isEmpty()
                    ? "PB-" + DateUtil.format(new Date(), "yyyyMMdd") + "-" + RandomUtil.randomNumbers(4)
                    : dto.getProductBatch().trim());
            order.setBaseSite(dto.getBaseSite());
            order.setMaterialId(dto.getMaterialId());
            order.setPlanNum(dto.getPlanNum());
            order.setOperatorUserId(dto.getOperatorUserId());
            orderMapper.insert(order);
        } else {
            order = requireOrder(dto.getId());
            if (order.getWorkStatus() != STATUS_WAIT) {
                throw new BizException("只有待生产状态的工单可以修改");
            }
            order.setBaseSite(dto.getBaseSite());
            order.setMaterialId(dto.getMaterialId());
            order.setPlanNum(dto.getPlanNum());
            order.setOperatorUserId(dto.getOperatorUserId());
            if (dto.getProductBatch() != null && !dto.getProductBatch().isEmpty()) {
                order.setProductBatch(dto.getProductBatch().trim());
            }
            orderMapper.updateById(order);
            processMapper.delete(new LambdaQueryWrapper<WorkOrderProcess>()
                    .eq(WorkOrderProcess::getWorkOrderId, order.getId()));
        }

        copyProcessTemplate(order);
        operLogService.log("生产工单", "保存工单：" + order.getWorkOrderNo() + "，产品 "
                + product.getMaterialName() + "，计划数量 " + dto.getPlanNum().toPlainString());
        return order;
    }

    private void copyProcessTemplate(ProductionWorkOrder order) {
        List<ProcessTemplate> templates = templateMapper.selectList(new LambdaQueryWrapper<ProcessTemplate>()
                .eq(ProcessTemplate::getMaterialId, order.getMaterialId())
                .orderByAsc(ProcessTemplate::getProcessSeq));
        if (templates.isEmpty()) {
            throw new BizException("该产品未维护工序模板，请先在工序模板中维护");
        }
        for (ProcessTemplate t : templates) {
            WorkOrderProcess process = new WorkOrderProcess();
            process.setWorkOrderId(order.getId());
            process.setProcessSeq(t.getProcessSeq());
            process.setProcessName(t.getProcessName());
            process.setRequirement(t.getRequirement());
            process.setProcessStatus(0);
            processMapper.insert(process);
        }
    }

    public PageResult<Map<String, Object>> list(String baseSite, Integer workStatus, Long operatorUserId,
                                                int pageNum, int pageSize) {
        LambdaQueryWrapper<ProductionWorkOrder> wrapper = new LambdaQueryWrapper<ProductionWorkOrder>()
                .eq(baseSite != null && !baseSite.isEmpty(), ProductionWorkOrder::getBaseSite, baseSite)
                .eq(workStatus != null, ProductionWorkOrder::getWorkStatus, workStatus)
                .eq(operatorUserId != null, ProductionWorkOrder::getOperatorUserId, operatorUserId)
                .orderByDesc(ProductionWorkOrder::getCreateTime);
        Page<ProductionWorkOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, WarehouseMaterial> materials = materialMap();
        Map<Long, SysUser> users = userMap();
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProductionWorkOrder order : page.getRecords()) {
            Map<String, Object> view = orderView(order, materials, users);
            List<WorkOrderProcess> processes = processesOf(order.getId());
            List<WorkReportCheck> reports = reportsOf(order.getId());
            view.put("totalProcess", processes.size());
            view.put("doneProcess", processes.stream().filter(p -> p.getProcessStatus() == 2).count());
            view.put("goodTotal", sum(reports, true));
            view.put("badTotal", sum(reports, false));
            list.add(view);
        }
        return new PageResult<>(page.getTotal(), list);
    }

    public Map<String, Object> detail(Long orderId) {
        ProductionWorkOrder order = requireOrder(orderId);
        Map<Long, WarehouseMaterial> materials = materialMap();
        Map<Long, SysUser> users = userMap();
        Map<String, Object> view = orderView(order, materials, users);
        List<WorkOrderProcess> processes = processesOf(orderId);
        List<WorkReportCheck> reports = reportsOf(orderId);
        Map<Long, WorkReportCheck> reportByProcess = reports.stream()
                .filter(r -> r.getProcessId() != null)
                .collect(Collectors.toMap(WorkReportCheck::getProcessId, r -> r, (a, b) -> b));

        List<Map<String, Object>> processViews = new ArrayList<>();
        for (WorkOrderProcess process : processes) {
            Map<String, Object> pv = new LinkedHashMap<>();
            pv.put("id", process.getId());
            pv.put("processSeq", process.getProcessSeq());
            pv.put("processName", process.getProcessName());
            pv.put("requirement", process.getRequirement());
            pv.put("processStatus", process.getProcessStatus());
            WorkReportCheck report = reportByProcess.get(process.getId());
            pv.put("report", report == null ? null : reportView(report));
            processViews.add(pv);
        }
        view.put("processes", processViews);
        view.put("reports", reports.stream().map(this::reportView).collect(Collectors.toList()));
        view.put("summary", summary(processes, reports));
        return view;
    }

    public Map<String, Object> progress(Long orderId) {
        ProductionWorkOrder order = requireOrder(orderId);
        Map<Long, WarehouseMaterial> materials = materialMap();
        Map<Long, SysUser> users = userMap();
        Map<String, Object> view = orderView(order, materials, users);
        List<WorkOrderProcess> processes = processesOf(orderId);
        List<WorkReportCheck> reports = reportsOf(orderId);
        view.put("reports", reports.stream().map(this::reportView).collect(Collectors.toList()));
        view.put("processes", processes.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("processSeq", p.getProcessSeq());
            m.put("processName", p.getProcessName());
            m.put("processStatus", p.getProcessStatus());
            return m;
        }).collect(Collectors.toList()));
        view.put("summary", summary(processes, reports));
        return view;
    }

    private Map<String, Object> summary(List<WorkOrderProcess> processes, List<WorkReportCheck> reports) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalProcess", processes.size());
        summary.put("doneProcess", processes.stream().filter(p -> p.getProcessStatus() == 2).count());
        summary.put("goodTotal", sum(reports, true));
        summary.put("badTotal", sum(reports, false));
        return summary;
    }

    private BigDecimal sum(List<WorkReportCheck> reports, boolean good) {
        return reports.stream()
                .map(r -> good ? nvl(r.getGoodNum()) : nvl(r.getBadNum()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<Map<String, Object>> bomList(Long productMaterialId) {
        Map<Long, WarehouseMaterial> materials = materialMap();
        List<MaterialBom> boms = bomMapper.selectList(new LambdaQueryWrapper<MaterialBom>()
                .eq(MaterialBom::getProductMaterialId, productMaterialId)
                .orderByAsc(MaterialBom::getId));
        return boms.stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.getId());
            m.put("componentMaterialId", b.getComponentMaterialId());
            WarehouseMaterial component = materials.get(b.getComponentMaterialId());
            m.put("componentMaterialCode", component == null ? "" : component.getMaterialCode());
            m.put("componentMaterialName", component == null ? "" : component.getMaterialName());
            m.put("quantity", b.getQuantity());
            m.put("remark", b.getRemark());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void bomSave(BomSaveDto dto) {
        if (dto.getProductMaterialId() == null) {
            throw new BizException("请选择成品物料");
        }
        bomMapper.delete(new LambdaQueryWrapper<MaterialBom>()
                .eq(MaterialBom::getProductMaterialId, dto.getProductMaterialId()));
        if (dto.getItems() != null) {
            for (BomSaveDto.Item item : dto.getItems()) {
                if (item.getComponentMaterialId() == null || item.getQuantity() == null) {
                    continue;
                }
                MaterialBom bom = new MaterialBom();
                bom.setProductMaterialId(dto.getProductMaterialId());
                bom.setComponentMaterialId(item.getComponentMaterialId());
                bom.setQuantity(item.getQuantity());
                bom.setRemark(item.getRemark() == null ? "" : item.getRemark());
                bomMapper.insert(bom);
            }
        }
        operLogService.log("生产工单", "维护 BOM，成品物料ID " + dto.getProductMaterialId());
    }

    public List<Map<String, Object>> tplList(Long materialId) {
        List<ProcessTemplate> templates = templateMapper.selectList(new LambdaQueryWrapper<ProcessTemplate>()
                .eq(ProcessTemplate::getMaterialId, materialId)
                .orderByAsc(ProcessTemplate::getProcessSeq));
        return templates.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("processSeq", t.getProcessSeq());
            m.put("processName", t.getProcessName());
            m.put("requirement", t.getRequirement());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void tplSave(TplSaveDto dto) {
        if (dto.getMaterialId() == null) {
            throw new BizException("请选择产品物料");
        }
        templateMapper.delete(new LambdaQueryWrapper<ProcessTemplate>()
                .eq(ProcessTemplate::getMaterialId, dto.getMaterialId()));
        if (dto.getItems() != null) {
            int seq = 1;
            for (TplSaveDto.Item item : dto.getItems()) {
                if (item.getProcessName() == null || item.getProcessName().isEmpty()) {
                    continue;
                }
                ProcessTemplate t = new ProcessTemplate();
                t.setMaterialId(dto.getMaterialId());
                t.setProcessSeq(item.getProcessSeq() == null ? seq : item.getProcessSeq());
                t.setProcessName(item.getProcessName());
                t.setRequirement(item.getRequirement() == null ? "" : item.getRequirement());
                templateMapper.insert(t);
                seq++;
            }
        }
        operLogService.log("生产工单", "维护工序模板，产品物料ID " + dto.getMaterialId());
    }

    public List<Map<String, Object>> operatorOptions() {
        SysRole operatorRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, "OPERATOR"));
        if (operatorRole == null) {
            return new ArrayList<>();
        }
        List<SysUser> users = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, operatorRole.getId())
                .eq(SysUser::getStatus, 1));
        return users.stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("realName", u.getRealName());
            m.put("username", u.getUsername());
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> productOptions() {
        List<WarehouseMaterial> products = materialMapper.selectList(new LambdaQueryWrapper<WarehouseMaterial>()
                .eq(WarehouseMaterial::getMaterialType, 3));
        return products.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("materialCode", p.getMaterialCode());
            m.put("materialName", p.getMaterialName());
            m.put("baseSite", p.getBaseSite());
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> baseSiteOptions() {
        List<SysDict> dicts = dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(SysDict::getDictType, "base_site")
                .orderByAsc(SysDict::getSort));
        return dicts.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("label", d.getDictLabel());
            m.put("value", d.getDictValue());
            return m;
        }).collect(Collectors.toList());
    }

    public PageResult<Map<String, Object>> todoList(String baseSite, int pageNum, int pageSize) {
        LambdaQueryWrapper<ProductionWorkOrder> wrapper = new LambdaQueryWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getWorkStatus, STATUS_CHECK)
                .eq(baseSite != null && !baseSite.isEmpty(), ProductionWorkOrder::getBaseSite, baseSite)
                .orderByAsc(ProductionWorkOrder::getCreateTime);
        Page<ProductionWorkOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, WarehouseMaterial> materials = materialMap();
        Map<Long, SysUser> users = userMap();
        List<Map<String, Object>> list = new ArrayList<>();
        for (ProductionWorkOrder order : page.getRecords()) {
            Map<String, Object> view = orderView(order, materials, users);
            view.put("summary", summary(processesOf(order.getId()), reportsOf(order.getId())));
            list.add(view);
        }
        return new PageResult<>(page.getTotal(), list);
    }

    @Transactional
    public void audit(AuditDto dto) {
        ProductionWorkOrder order = requireOrder(dto.getWorkOrderId());
        if (order.getWorkStatus() != STATUS_CHECK) {
            throw new BizException("只有待质检状态的工单可以审核");
        }
        if (dto.getResult() == null || (dto.getResult() != 1 && dto.getResult() != 2)) {
            throw new BizException("请选择质检结果（合格/不合格）");
        }
        if (dto.getResult() == 2 && dto.getBadReasonId() == null) {
            throw new BizException("质检不合格必须选择质检不良原因");
        }
        List<WorkReportCheck> reports = reportsOf(order.getId());
        if (reports.isEmpty()) {
            throw new BizException("该工单没有报工记录，无法质检");
        }
        Date now = new Date();
        String checker = UserContext.name();
        for (WorkReportCheck report : reports) {
            report.setCheckResult(dto.getResult());
            report.setCheckUser(checker);
            report.setCheckTime(now);
            report.setCheckRemark(dto.getRemark() == null ? "" : dto.getRemark());
            report.setCheckBadReasonId(dto.getResult() == 2 ? dto.getBadReasonId() : null);
            reportMapper.updateById(report);
        }

        if (dto.getResult() == 1) {
            order.setWorkStatus(STATUS_DONE);
            orderMapper.updateById(order);
            BigDecimal finished = finishedNum(order.getId());
            if (finished.compareTo(BigDecimal.ZERO) > 0) {
                InBoundDto in = new InBoundDto();
                in.setMaterialId(order.getMaterialId());
                in.setOperNum(finished);
                in.setBatchNo(order.getProductBatch());
                in.setWorkOrderId(order.getId());
                in.setRemark("质检合格成品入库（工单 " + order.getWorkOrderNo() + "）");
                stockService.inBound(in);
            }
        } else {
            order.setWorkStatus(STATUS_PRODUCING);
            orderMapper.updateById(order);
            processMapper.update(null, new LambdaUpdateWrapper<WorkOrderProcess>()
                    .set(WorkOrderProcess::getProcessStatus, 0)
                    .eq(WorkOrderProcess::getWorkOrderId, order.getId()));
            reportMapper.update(null, new LambdaUpdateWrapper<WorkReportCheck>()
                    .set(WorkReportCheck::getCheckResult, 0)
                    .eq(WorkReportCheck::getWorkOrderId, order.getId()));
        }
        operLogService.log("质量管控", "工单质检：" + order.getWorkOrderNo()
                + (dto.getResult() == 1 ? " 合格" : " 不合格，退回重工"));
    }

    /** 成品数量：最后一道工序的良品数量 */
    public BigDecimal finishedNum(Long orderId) {
        List<WorkOrderProcess> processes = processesOf(orderId);
        if (processes.isEmpty()) {
            return BigDecimal.ZERO;
        }
        WorkOrderProcess last = processes.stream()
                .max((a, b) -> a.getProcessSeq().compareTo(b.getProcessSeq()))
                .orElse(processes.get(0));
        return reportsOf(orderId).stream()
                .filter(r -> r.getProcessId() != null && r.getProcessId().equals(last.getId()))
                .map(r -> nvl(r.getGoodNum()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, Object> orderView(ProductionWorkOrder order, Map<Long, WarehouseMaterial> materials,
                                         Map<Long, SysUser> users) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", order.getId());
        view.put("workOrderNo", order.getWorkOrderNo());
        view.put("baseSite", order.getBaseSite());
        view.put("materialId", order.getMaterialId());
        view.put("planNum", order.getPlanNum());
        view.put("operatorUserId", order.getOperatorUserId());
        view.put("workStatus", order.getWorkStatus());
        view.put("productBatch", order.getProductBatch());
        view.put("createTime", order.getCreateTime());
        WarehouseMaterial material = materials.get(order.getMaterialId());
        view.put("materialCode", material == null ? "" : material.getMaterialCode());
        view.put("materialName", material == null ? "" : material.getMaterialName());
        SysUser operator = order.getOperatorUserId() == null ? null : users.get(order.getOperatorUserId());
        view.put("operatorName", operator == null ? "" : operator.getRealName());
        return view;
    }

    private Map<String, Object> reportView(WorkReportCheck report) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", report.getId());
        m.put("workOrderId", report.getWorkOrderId());
        m.put("processId", report.getProcessId());
        m.put("processName", report.getProcessName());
        m.put("goodNum", report.getGoodNum());
        m.put("badNum", report.getBadNum());
        m.put("badReasonDictId", report.getBadReasonDictId());
        m.put("badReason", report.getBadReasonDictId() == null ? "" : dictLabel(report.getBadReasonDictId()));
        m.put("checkBadReasonId", report.getCheckBadReasonId());
        m.put("photoUrl", report.getPhotoUrl());
        m.put("reportTime", report.getReportTime());
        m.put("checkResult", report.getCheckResult());
        m.put("checkUser", report.getCheckUser());
        m.put("checkTime", report.getCheckTime());
        m.put("checkRemark", report.getCheckRemark());
        return m;
    }

    private String dictLabel(Long dictId) {
        SysDict dict = dictMapper.selectById(dictId);
        return dict == null ? "" : dict.getDictLabel();
    }

    public List<WorkOrderProcess> processesOf(Long orderId) {
        return processMapper.selectList(new LambdaQueryWrapper<WorkOrderProcess>()
                .eq(WorkOrderProcess::getWorkOrderId, orderId)
                .orderByAsc(WorkOrderProcess::getProcessSeq));
    }

    public List<WorkReportCheck> reportsOf(Long orderId) {
        return reportMapper.selectList(new LambdaQueryWrapper<WorkReportCheck>()
                .eq(WorkReportCheck::getWorkOrderId, orderId)
                .orderByAsc(WorkReportCheck::getProcessId));
    }

    public List<WorkReportCheck> reportsOf(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return new ArrayList<>();
        }
        return reportMapper.selectList(new LambdaQueryWrapper<WorkReportCheck>()
                .in(WorkReportCheck::getWorkOrderId, orderIds)
                .orderByAsc(WorkReportCheck::getWorkOrderId));
    }

    public Map<Long, WarehouseMaterial> materialMap() {
        return materialMapper.selectList(null).stream()
                .collect(Collectors.toMap(WarehouseMaterial::getId, m -> m));
    }

    public Map<Long, SysUser> userMap() {
        return userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));
    }

    public ProductionWorkOrder requireOrder(Long id) {
        ProductionWorkOrder order = id == null ? null : orderMapper.selectById(id);
        if (order == null) {
            throw new BizException("生产工单不存在");
        }
        return order;
    }

    private String genOrderNo() {
        String no;
        int guard = 0;
        do {
            no = "WO-" + DateUtil.format(new Date(), "yyyyMMdd") + "-" + RandomUtil.randomNumbers(4);
            guard++;
        } while (orderMapper.selectCount(new LambdaQueryWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getWorkOrderNo, no)) > 0 && guard < 10);
        return no;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public List<StockBatch> batchesOf(Long materialId) {
        return batchMapper.selectList(new LambdaQueryWrapper<StockBatch>()
                .eq(StockBatch::getWarehouseMaterialId, materialId)
                .orderByAsc(StockBatch::getCreateTime));
    }
}
