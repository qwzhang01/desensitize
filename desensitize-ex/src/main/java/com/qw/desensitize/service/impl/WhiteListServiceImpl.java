package com.qw.desensitize.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qw.desensitize.entity.WhiteList;
import com.qw.desensitize.mapper.WhiteListMapper;
import com.qw.desensitize.service.WhiteListService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 对象白名单owner表 服务实现类
 * </p>
 *
 * @author avinzhang
 * @since 2025-10-03
 */
@Service
public class WhiteListServiceImpl extends ServiceImpl<WhiteListMapper, WhiteList> implements WhiteListService {

}
