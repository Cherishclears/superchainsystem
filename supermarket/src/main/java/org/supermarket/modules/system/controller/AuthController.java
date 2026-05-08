package org.supermarket.modules.system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.supermarket.common.result.Result;
import org.supermarket.common.result.ResultCode;
import org.supermarket.common.utils.JwtUtils;
import org.supermarket.modules.system.dto.LoginDTO;
import org.supermarket.modules.system.dto.LoginVO;
import org.supermarket.modules.system.entity.SysUser;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
    System.out.println("===> login 方法被调用, username=" + dto.getUsername());

    Authentication authentication;
    try {
        authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
    } catch (Exception e) {
        // 调试用
        System.out.println("===> 认证失败异常类型: " + e.getClass().getName());
        System.out.println("===> 认证失败异常信息: " + e.getMessage());
        // 先直接返回一个明确的业务错误给前端
        return Result.fail(ResultCode.USERNAME_OR_PASSWORD_ERROR);
    }

    //
        SysUser user = (SysUser) authentication.getPrincipal();

        // 生成 JWT Token
        String token = jwtUtils.generateToken(
                user.getId(),
                user.getUsername(),
                Map.of("storeId", String.valueOf(user.getStoreId()),
                        "userType", user.getUserType()));

        LoginVO vo = LoginVO.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .avatar(user.getAvatar())
                .storeId(user.getStoreId())
                .userType(user.getUserType())
                .permissions(user.getPermissions())
                .build();

        return Result.ok("登录成功", vo);
    }

    @PostMapping("/logout")
    public Result<String> logout() {
        return Result.ok("已退出登录");
    }
}