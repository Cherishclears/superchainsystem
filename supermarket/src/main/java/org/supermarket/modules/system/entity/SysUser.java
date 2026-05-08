package org.supermarket.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.supermarket.common.entity.BaseEntity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity implements UserDetails {

    private String username;

    @JsonIgnore
    private String password;

    private String realName;
    private String phone;
    private String avatar;
    private Long storeId;      // 所属门店ID，总部用户为null
    private Integer userType;  // 1总部 2门店员工
    private Integer status;    // 1启用 0禁用
    private LocalDateTime lastLoginTime;

    // 用户权限列表，不存数据库，登录后动态赋值
    @JsonIgnore
    private transient List<String> permissions;

    // ---- UserDetails 接口实现 ----

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null) return List.of();
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return status == 1; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return status == 1; }
}