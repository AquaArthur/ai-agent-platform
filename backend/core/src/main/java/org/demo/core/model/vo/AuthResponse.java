package org.demo.core.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "认证响应")
public class AuthResponse {

    @Schema(description = "JWT令牌")
    private String token;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "用户ID")
    private String userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色")
    private String role;

    public AuthResponse(String token, String userId, String username, String nickname, String role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
    }
}
