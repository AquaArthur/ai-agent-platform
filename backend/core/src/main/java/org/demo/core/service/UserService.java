package org.demo.core.service;

import org.demo.core.model.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户实体
     */
    User findByUsername(String username);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户实体
     */
    User findByEmail(String email);

    /**
     * 创建新用户
     * @param user 用户实体
     * @return 创建后的用户实体
     */
    User createUser(User user);

    /**
     * 更新用户信息
     * @param user 用户实体
     * @return 更新后的用户实体
     */
    User updateUser(User user);

    /**
     * 更新最后登录信息
     * @param userId 用户ID
     * @param ip 登录IP
     */
    void updateLastLogin(String userId, String ip);
}
