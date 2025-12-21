package org.demo.core.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户注册请求DTO
 */
@Data
@Schema(description = "用户注册请求")
public class RegisterRequest {

    @Schema(description = "用户名", example = "newuser")
    private String username;

    @Schema(description = "密码", example = "password123")
    private String password;

    @Schema(description = "昵称", example = "新用户")
    private String nickname;
}
