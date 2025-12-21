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
}
