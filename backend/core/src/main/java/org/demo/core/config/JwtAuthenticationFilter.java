package org.demo.core.config;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.core.model.entity.User;
import org.demo.core.service.UserService;
import org.demo.core.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT认证过滤器
 * 从请求头中提取JWT令牌，验证并设置到Spring Security上下文中
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Value("${jwt.header}")
    private String tokenHeader;

    @Value("${jwt.prefix}")
    private String tokenPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // 从请求头中获取token
        String header = request.getHeader(tokenHeader);
        
        if (header == null || !header.startsWith(tokenPrefix)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 提取token（去掉"Bearer "前缀）
        String token = header.substring(tokenPrefix.length());

        try {
            // 从token中获取用户名
            String username = jwtUtil.getUsernameFromToken(token);

            // 如果用户名不为空且当前SecurityContext中没有认证信息
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 从数据库查询用户
                User user = userService.findByUsername(username);
                
                if (user != null && jwtUtil.validateToken(token, username)) {
                    // 验证token有效性
                    
                    // 创建认证对象
                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().toUpperCase());
                    UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(user, null, Collections.singletonList(authority));
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 设置到Spring Security上下文中
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("User '{}' authenticated successfully", username);
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to authenticate user from JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
