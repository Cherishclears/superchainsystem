package org.supermarket.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.supermarket.common.exception.BusinessException;
import org.supermarket.common.result.Result;
import org.supermarket.common.result.ResultCode;
import org.supermarket.common.utils.SecurityUtils;
import org.supermarket.modules.system.dto.PasswordUpdateDTO;
import org.supermarket.modules.system.dto.UserCreateDTO;
import org.supermarket.modules.system.dto.UserUpdateDTO;
import org.supermarket.modules.system.entity.SysUser;
import org.supermarket.modules.system.service.SysUserService;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;
    private final PasswordEncoder passwordEncoder;

    // 获取当前用户信息
    @GetMapping("/info")
    public Result<SysUser> getCurrentUserInfo() {
        SysUser user = SecurityUtils.getCurrentUser();
        return Result.ok(user);
    }

    // 修改个人信息
    @PutMapping("/info")
    public Result<Void> updateInfo(@RequestBody UserUpdateDTO dto) {
        SysUser user = SecurityUtils.getCurrentUser();
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setAvatar(dto.getAvatar());
        sysUserService.updateById(user);
        return Result.ok();
    }

    // 修改密码
    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordUpdateDTO dto) {
        SysUser user = SecurityUtils.getCurrentUser();
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserService.updateById(user);
        return Result.ok();
    }

    // 分页查询用户列表（仅总部可用）
    @GetMapping("/page")
    public Result<Page<SysUser>> page(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer userType) {
        if (!SecurityUtils.isHQ()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysUser::getUsername, username);
        }
        if (userType != null) {
            wrapper.eq(SysUser::getUserType, userType);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> page = sysUserService.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(page);
    }

    // 新增用户（仅总部可用）
    @PostMapping
    public Result<Void> createUser(@Valid @RequestBody UserCreateDTO dto) {
        if (!SecurityUtils.isHQ()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        // 检查用户名是否已存在
        long count = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setUserType(dto.getUserType());
        user.setStoreId(dto.getStoreId());
        user.setStatus(1);
        sysUserService.save(user);
        return Result.ok();
    }

    // 启用/禁用用户（仅总部可用）
    @PutMapping("/{id}/status/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        if (!SecurityUtils.isHQ()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setStatus(status);
        sysUserService.updateById(user);
        return Result.ok();
    }

    // 重置密码（仅总部可用）
    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        if (!SecurityUtils.isHQ()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(passwordEncoder.encode("123456"));
        sysUserService.updateById(user);
        return Result.ok();
    }
}
