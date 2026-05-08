package org.supermarket.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.supermarket.common.exception.BusinessException;
import org.supermarket.common.result.ResultCode;
import org.supermarket.modules.system.entity.SysUser;
import org.supermarket.modules.system.mapper.SysUserMapper;
import org.supermarket.modules.system.service.SysUserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        try {
            List<String> permissions = baseMapper.selectPermissionsByUserId(user.getId());
            user.setPermissions(permissions);
        } catch (Exception e) {
            log.warn("查询用户权限失败: " + e.getMessage());
            user.setPermissions(List.of());
        }
        return user;
    }

    @Override
    public SysUser loadUserById(Long userId) {
        SysUser user = getById(userId);
        if (user == null) throw new BusinessException(ResultCode.USER_NOT_FOUND);
        try {
            List<String> permissions = baseMapper.selectPermissionsByUserId(userId);
            user.setPermissions(permissions);
        } catch (Exception e) {
            log.warn("查询用户权限失败: " + e.getMessage());
            user.setPermissions(List.of());
        }
        return user;
    }
}