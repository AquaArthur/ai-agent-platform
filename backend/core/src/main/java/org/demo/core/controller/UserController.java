package org.demo.core.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.core.api.ApiResponse;
import org.demo.core.model.dto.LoginRequest;
import org.demo.core.model.dto.RegisterRequest;
import org.demo.core.model.entity.User;
import org.demo.core.model.vo.AuthResponse;
import org.demo.core.service.UserService;
import org.demo.core.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 用户认证控制器
 * 提供用户登录、注册等认证相关接口
 */
@Slf4j
@Tag(name = "用户认证", description = "提供用户登录、注册等认证功能接口")
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "使用用户名和密码登录，成功后返回JWT令牌")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@RequestBody LoginRequest request) {
        // 查询用户
        User user = userService.findByUsername(request.getUsername());

        if (user == null) {
            return ApiResponse.fail(401, "用户名或密码错误");
        }

        // 检查账户状态
        if (!"active".equals(user.getStatus())) {
            return ApiResponse.fail(403, "账户已被锁定或未激活");
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ApiResponse.fail(401, "用户名或密码错误");
        }

        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUsername());

        // 构造响应
        AuthResponse response = new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRole()
        );

        log.info("User '{}' logged in successfully", user.getUsername());
        return ApiResponse.ok(response);
    }

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "注册新用户账号")
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@RequestBody RegisterRequest request) {
        // 检查用户名是否已存在
        if (userService.findByUsername(request.getUsername()) != null) {
            return ApiResponse.fail(400, "用户名已存在");
        }


        // 创建新用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setRole("user");
        user.setStatus("active");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        // 保存用户
        userService.createUser(user);

        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getUsername());

        // 构造响应
        AuthResponse response = new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getRole()
        );

        log.info("User '{}' registered successfully", user.getUsername());
        return ApiResponse.ok(response);
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
