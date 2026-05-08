package org.supermarket.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.supermarket.modules.system.entity.SysUser;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    // 查询用户拥有的所有权限标识
    @Select("""
            SELECT DISTINCT p.perm_code
            FROM sys_user u
            JOIN sys_user_role ur ON u.id = ur.user_id
            JOIN sys_role_permission rp ON ur.role_id = rp.role_id
            JOIN sys_permission p ON rp.perm_id = p.id
            WHERE u.id = #{userId} AND u.is_deleted = 0
            """)
    List<String> selectPermissionsByUserId(Long userId);
}