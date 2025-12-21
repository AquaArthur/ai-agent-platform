package org.demo.core.model.dto;

import lombok.Data;

/**
 * 用户注册请求DTO
 */
@Data
public class RegisterRequest {

    private String username;

    private String password;

    private String nickname;
}
