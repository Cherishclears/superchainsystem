package org.supermarket.modules.system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private Long storeId;
    private Integer userType;
    private List<String> permissions;
}