package org.supermarket.modules.system.dto;

import lombok.Data;

@Data
public class UserUpdateDTO {
    private String realName;
    private String phone;
    private String avatar;
}
