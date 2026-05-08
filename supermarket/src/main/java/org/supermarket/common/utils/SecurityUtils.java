package org.supermarket.common.utils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.supermarket.modules.system.entity.SysUser;
import org.supermarket.modules.system.service.SysUserService;

@Component
public class SecurityUtils {

    private static SysUserService sysUserService;

    @Autowired
    public void setSysUserService(SysUserService service) {
        sysUserService = service;
    }

    public static String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public static SysUser getCurrentUser() {
        String username = getCurrentUsername();
        return sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
    }

    public static boolean isHQ() {
        SysUser user = getCurrentUser();
        return user != null && user.getUserType() == 1;
    }

    public static Long getCurrentStoreId() {
        SysUser user = getCurrentUser();
        return user != null ? user.getStoreId() : null;
    }
}