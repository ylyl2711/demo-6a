package com.project.stockwork.controller;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.project.stockwork.common.BizException;
import com.project.stockwork.common.CommonResult;
import com.project.stockwork.common.LoginUser;
import com.project.stockwork.common.UserContext;
import com.project.stockwork.dto.LoginDto;
import com.project.stockwork.entity.SysRole;
import com.project.stockwork.entity.SysUser;
import com.project.stockwork.mapper.SysRoleMapper;
import com.project.stockwork.mapper.SysUserMapper;
import com.project.stockwork.service.OperLogService;
import com.project.stockwork.util.JwtUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Api(tags = "登录认证")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final JwtUtil jwtUtil;
    private final OperLogService operLogService;

    @ApiOperation("登录（Web 与 H5 共用）")
    @PostMapping("/login")
    public CommonResult<Map<String, Object>> login(@RequestBody LoginDto dto) {
        if (dto.getUsername() == null || dto.getUsername().trim().isEmpty()
                || dto.getPassword() == null || dto.getPassword().isEmpty()) {
            throw new BizException("请输入用户名和密码");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername().trim()));
        if (user == null || !DigestUtil.md5Hex(dto.getPassword()).equalsIgnoreCase(user.getPassword())) {
            throw new BizException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用，请联系系统管理员");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null) {
            throw new BizException("账号未绑定角色，请联系系统管理员");
        }
        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRealName(),
                role.getRoleCode(), role.getRoleName(), null);
        String token = jwtUtil.createToken(loginUser);
        operLogService.logAs(user.getRealName(), "系统管理", "用户登录：" + user.getUsername());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo(user, role));
        return CommonResult.ok(result);
    }

    @ApiOperation("当前登录用户信息")
    @GetMapping("/me")
    public CommonResult<Map<String, Object>> me() {
        LoginUser current = UserContext.require();
        SysUser user = userMapper.selectById(current.getId());
        if (user == null) {
            throw new BizException(401, "账号不存在");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        return CommonResult.ok(userInfo(user, role));
    }

    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public CommonResult<Void> logout() {
        return CommonResult.ok();
    }

    private Map<String, Object> userInfo(SysUser user, SysRole role) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("roleId", user.getRoleId());
        info.put("roleCode", role == null ? "" : role.getRoleCode());
        info.put("roleName", role == null ? "" : role.getRoleName());
        info.put("perms", role == null || role.getMenuPerms() == null
                ? "" : role.getMenuPerms());
        return info;
    }
}
