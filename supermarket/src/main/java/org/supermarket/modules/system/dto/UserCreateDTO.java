package org.supermarket.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserCreateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String realName;
    private String phone;

    @NotNull(message = "用户类型不能为空")
    private Integer userType;  // 1总部 2门店管理员 3收银员

    private Long storeId;      // 门店ID，非总部用户必填
}
