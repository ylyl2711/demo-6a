package com.project.stockwork.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.project.stockwork.common.BizException;
import com.project.stockwork.common.CommonResult;
import com.project.stockwork.common.PageResult;
import com.project.stockwork.common.RequireRole;
import com.project.stockwork.dto.DictSaveDto;
import com.project.stockwork.dto.UserSaveDto;
import com.project.stockwork.entity.ProductionWorkOrder;
import com.project.stockwork.entity.SysDict;
import com.project.stockwork.entity.SysOperLog;
import com.project.stockwork.entity.SysRole;
import com.project.stockwork.entity.SysUser;
import com.project.stockwork.mapper.ProductionWorkOrderMapper;
import com.project.stockwork.mapper.SysDictMapper;
import com.project.stockwork.mapper.SysOperLogMapper;
import com.project.stockwork.mapper.SysRoleMapper;
import com.project.stockwork.mapper.SysUserMapper;
import com.project.stockwork.service.OperLogService;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "系统管理")
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class SystemController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysDictMapper dictMapper;
    private final SysOperLogMapper operLogMapper;
    private final ProductionWorkOrderMapper orderMapper;
    private final OperLogService operLogService;

    @ApiOperation("用户列表")
    @GetMapping("/user/list")
    public CommonResult<PageResult<Map<String, Object>>> userList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isEmpty(),
                        w -> w.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword))
                .eq(roleId != null, SysUser::getRoleId, roleId)
                .orderByAsc(SysUser::getId);
        Page<SysUser> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, SysRole> roles = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getId, r -> r));
        List<Map<String, Object>> list = new ArrayList<>();
        for (SysUser user : page.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", user.getId());
            m.put("username", user.getUsername());
            m.put("realName", user.getRealName());
            m.put("roleId", user.getRoleId());
            SysRole role = roles.get(user.getRoleId());
            m.put("roleName", role == null ? "" : role.getRoleName());
            m.put("roleCode", role == null ? "" : role.getRoleCode());
            m.put("status", user.getStatus());
            m.put("createTime", user.getCreateTime());
            list.add(m);
        }
        return CommonResult.ok(new PageResult<>(page.getTotal(), list));
    }

    @ApiOperation("新增/编辑用户")
    @PostMapping("/user/save")
    public CommonResult<Void> saveUser(@RequestBody UserSaveDto dto) {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()) {
            throw new BizException("用户名不能为空");
        }
        if (dto.getRoleId() == null) {
            throw new BizException("请选择用户角色");
        }
        String username = dto.getUsername().trim();
        SysUser sameName = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (dto.getId() == null) {
            if (sameName != null) {
                throw new BizException("用户名已存在");
            }
            if (dto.getPassword() == null || dto.getPassword().isEmpty()) {
                throw new BizException("新增用户必须设置密码");
            }
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPassword(DigestUtil.md5Hex(dto.getPassword()));
            user.setRealName(dto.getRealName() == null ? "" : dto.getRealName());
            user.setRoleId(dto.getRoleId());
            user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
            user.setCreateTime(new Date());
            userMapper.insert(user);
            operLogService.log("系统管理", "新增用户：" + username);
        } else {
            SysUser user = userMapper.selectById(dto.getId());
            if (user == null) {
                throw new BizException("用户不存在");
            }
            if (sameName != null && !sameName.getId().equals(user.getId())) {
                throw new BizException("用户名已存在");
            }
            user.setUsername(username);
            user.setRealName(dto.getRealName() == null ? "" : dto.getRealName());
            user.setRoleId(dto.getRoleId());
            if (dto.getStatus() != null) {
                user.setStatus(dto.getStatus());
            }
            if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
                user.setPassword(DigestUtil.md5Hex(dto.getPassword()));
            }
            userMapper.updateById(user);
            operLogService.log("系统管理", "编辑用户：" + username);
        }
        return CommonResult.ok();
    }

    @ApiOperation("启用/禁用用户")
    @PostMapping("/user/status")
    public CommonResult<Void> changeStatus(@RequestParam Long id, @RequestParam Integer status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
        operLogService.log("系统管理", (status == 1 ? "启用" : "禁用") + "用户：" + user.getUsername());
        return CommonResult.ok();
    }

    @ApiOperation("删除用户")
    @PostMapping("/user/delete")
    public CommonResult<Void> deleteUser(@RequestParam Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        Long count = orderMapper.selectCount(new LambdaQueryWrapper<ProductionWorkOrder>()
                .eq(ProductionWorkOrder::getOperatorUserId, id));
        if (count != null && count > 0) {
            throw new BizException("该用户已分配生产工单，不能删除，请改为禁用");
        }
        userMapper.deleteById(id);
        operLogService.log("系统管理", "删除用户：" + user.getUsername());
        return CommonResult.ok();
    }

    @ApiOperation("角色列表")
    @GetMapping("/role/list")
    public CommonResult<List<SysRole>> roleList() {
        return CommonResult.ok(roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getId)));
    }

    @ApiOperation("保存角色菜单权限")
    @PostMapping("/role/perm")
    public CommonResult<Void> savePerm(@RequestParam Long roleId, @RequestParam String menus) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException("角色不存在");
        }
        role.setMenuPerms(menus);
        roleMapper.updateById(role);
        operLogService.log("系统管理", "调整角色权限：" + role.getRoleName());
        return CommonResult.ok();
    }

    @ApiOperation("字典列表")
    @GetMapping("/dict/list")
    public CommonResult<List<SysDict>> dictList(@RequestParam(required = false) String dictType) {
        return CommonResult.ok(dictMapper.selectList(new LambdaQueryWrapper<SysDict>()
                .eq(dictType != null && !dictType.isEmpty(), SysDict::getDictType, dictType)
                .orderByAsc(SysDict::getDictType)
                .orderByAsc(SysDict::getSort)));
    }

    @ApiOperation("新增/编辑字典")
    @PostMapping("/dict/save")
    public CommonResult<Void> saveDict(@RequestBody DictSaveDto dto) {
        if (dto.getDictType() == null || dto.getDictLabel() == null || dto.getDictValue() == null) {
            throw new BizException("字典类型、名称、值不能为空");
        }
        SysDict dict = dto.getId() == null ? new SysDict() : dictMapper.selectById(dto.getId());
        if (dict == null) {
            throw new BizException("字典数据不存在");
        }
        dict.setDictType(dto.getDictType());
        dict.setDictLabel(dto.getDictLabel());
        dict.setDictValue(dto.getDictValue());
        dict.setSort(dto.getSort() == null ? 0 : dto.getSort());
        if (dto.getId() == null) {
            dictMapper.insert(dict);
        } else {
            dictMapper.updateById(dict);
        }
        operLogService.log("系统管理", "保存字典：" + dict.getDictType() + "/" + dict.getDictLabel());
        return CommonResult.ok();
    }

    @ApiOperation("删除字典")
    @PostMapping("/dict/delete")
    public CommonResult<Void> deleteDict(@RequestParam Long id) {
        SysDict dict = dictMapper.selectById(id);
        if (dict == null) {
            throw new BizException("字典数据不存在");
        }
        dictMapper.deleteById(id);
        operLogService.log("系统管理", "删除字典：" + dict.getDictLabel());
        return CommonResult.ok();
    }

    @ApiOperation("操作日志查询")
    @GetMapping("/log/list")
    public CommonResult<PageResult<SysOperLog>> logList(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String operUser,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<SysOperLog> page = operLogMapper.selectPage(new Page<>(pageNum, pageSize),
                logWrapper(startTime, endTime, operUser));
        return CommonResult.ok(new PageResult<>(page.getTotal(), page.getRecords()));
    }

    @ApiOperation("导出操作日志")
    @GetMapping("/log/export")
    public void exportLog(@RequestParam(required = false) String startTime,
                          @RequestParam(required = false) String endTime,
                          @RequestParam(required = false) String operUser,
                          HttpServletResponse response) {
        List<SysOperLog> logs = operLogMapper.selectList(logWrapper(startTime, endTime, operUser)
                .last("limit 5000"));
        XSSFWorkbook workbook = ExcelUtil.create("操作日志",
                new String[]{"操作人", "操作时间", "操作模块", "操作内容", "IP 地址"});
        Sheet sheet = workbook.getSheetAt(0);
        int rowIndex = 1;
        for (SysOperLog log : logs) {
            ExcelUtil.fillRow(sheet, rowIndex++, log.getOperUser(),
                    log.getOperTime() == null ? "" : DateUtil.formatDateTime(log.getOperTime()),
                    log.getOperModule(), log.getOperContent(), log.getIpAddress());
        }
        ExcelUtil.write(response, workbook, "操作日志.xlsx");
    }

    private LambdaQueryWrapper<SysOperLog> logWrapper(String startTime, String endTime, String operUser) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<SysOperLog>()
                .ge(startTime != null && !startTime.isEmpty(), SysOperLog::getOperTime,
                        startTime == null || startTime.isEmpty() ? null : DateUtil.parse(startTime + " 00:00:00"))
                .le(endTime != null && !endTime.isEmpty(), SysOperLog::getOperTime,
                        endTime == null || endTime.isEmpty() ? null : DateUtil.parse(endTime + " 23:59:59"))
                .like(operUser != null && !operUser.isEmpty(), SysOperLog::getOperUser, operUser)
                .orderByDesc(SysOperLog::getOperTime);
        return wrapper;
    }
}
