package org.supermarket.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.supermarket.modules.system.entity.SysUser;

public interface SysUserService extends IService<SysUser>, UserDetailsService {

    // 根据用户ID加载用户（含权限），供 JWT 过滤器使用
    SysUser loadUserById(Long userId);
}