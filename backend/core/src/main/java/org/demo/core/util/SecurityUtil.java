package org.demo.core.util;

import org.demo.core.model.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security工具类
 * 用于从Spring Security上下文中获取当前用户信息
 */
public class SecurityUtil {

    /**
     * 获取当前登录用户
     * @return 当前用户对象，如果未登录则返回null
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        
        if (principal instanceof User) {
            return (User) principal;
        }
        
        return null;
    }

    /**
     * 获取当前登录用户ID
     * @return 当前用户ID，如果未登录则返回null
     */
    public static String getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前登录用户名
     * @return 当前用户名，如果未登录则返回null
     */
    public static String getCurrentUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 检查当前用户是否为管理员
     * @return 如果是管理员返回true，否则返回false
     */
    public static boolean isAdmin() {
        User user = getCurrentUser();
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }
}
