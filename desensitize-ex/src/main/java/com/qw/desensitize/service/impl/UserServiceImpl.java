package com.qw.desensitize.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qw.desensitize.entity.User;
import com.qw.desensitize.mapper.UserMapper;
import com.qw.desensitize.service.UserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 自定义字段信息表 服务实现类
 * </p>
 *
 * @author avinzhang
 * @since 2025-10-03
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
