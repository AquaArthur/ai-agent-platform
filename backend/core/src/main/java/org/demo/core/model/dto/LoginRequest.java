package org.demo.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户登录请求DTO
 */
@Data
@Schema(description = "用户登录请求")
public class LoginRequest {

    @Schema(description = "用户名", example = "home_creator")
    private String username;

    @Schema(description = "密码", example = "password123")
    private String password;
}
