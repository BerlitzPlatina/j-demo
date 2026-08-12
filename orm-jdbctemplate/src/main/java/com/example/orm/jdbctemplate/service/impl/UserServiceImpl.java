package com.example.orm.jdbctemplate.service.impl;

import com.example.orm.jdbctemplate.constant.Const;
import com.example.orm.jdbctemplate.dao.UserDao;
import com.example.orm.jdbctemplate.entity.User;
import com.example.orm.jdbctemplate.service.IUserService;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.beans.PropertyDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * User Service Implement
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-10-15 13:53
 */
@Service
public class UserServiceImpl implements IUserService {
    private final UserDao userDao;

    @Autowired
    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * 保存用户
     *
     * @param user 用户实体
     * @return 保存成功 {@code true} 保存失败 {@code false}
     */
    @Override
    public Boolean save(User user) {
        String rawPass = user.getPassword();
        String salt = UUID.randomUUID().toString().replace("-", "");
        String pass = DigestUtils.md5DigestAsHex((rawPass + Const.SALT_PREFIX + salt).getBytes(StandardCharsets.UTF_8));
        user.setPassword(pass);
        user.setSalt(salt);
        return userDao.insert(user) > 0;
    }

    /**
     * 删除用户
     *
     * @param id 主键id
     * @return 删除成功 {@code true} 删除失败 {@code false}
     */
    @Override
    public Boolean delete(Long id) {
        return userDao.delete(id) > 0;
    }

    /**
     * 更新用户
     *
     * @param user 用户实体
     * @param id   主键id
     * @return 更新成功 {@code true} 更新失败 {@code false}
     */
    @Override
    public Boolean update(User user, Long id) {
        User exist = getUser(id);
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            String rawPass = user.getPassword();
            String salt = UUID.randomUUID().toString().replace("-", "");
            String pass = DigestUtils
                    .md5DigestAsHex((rawPass + Const.SALT_PREFIX + salt).getBytes(StandardCharsets.UTF_8));
            user.setPassword(pass);
            user.setSalt(salt);
        }
        copyNonNullProperties(user, exist);
        exist.setLastUpdateTime(new Date());
        return userDao.update(exist, id) > 0;
    }

    /**
     * 获取单个用户
     *
     * @param id 主键id
     * @return 单个用户对象
     */
    @Override
    public User getUser(Long id) {
        return userDao.findOneById(id);
    }

    /**
     * 获取用户列表
     *
     * @param user 用户实体
     * @return 用户列表
     */
    @Override
    public List<User> getUser(User user) {
        return userDao.findByExample(user);
    }

    private void copyNonNullProperties(Object source, Object target) {
        BeanWrapper src = new BeanWrapperImpl(source);
        Set<String> nullNames = new HashSet<>();
        for (PropertyDescriptor pd : src.getPropertyDescriptors()) {
            if (src.getPropertyValue(pd.getName()) == null) {
                nullNames.add(pd.getName());
            }
        }
        org.springframework.beans.BeanUtils.copyProperties(source, target, nullNames.toArray(new String[0]));
    }
}
