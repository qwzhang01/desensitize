package com.qw.desensitize.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qw.desensitize.dto.UserDto;
import com.qw.desensitize.dto.UserParam;
import com.qw.desensitize.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 自定义字段信息表 Mapper 接口
 * </p>
 *
 * @author avinzhang
 * @since 2025-10-03
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    Page<UserDto> listObj(Page<User> page, @Param("userParam") UserParam userParam);

    Page<UserDto> listParam(Page<User> page, @Param("phone") String userParam);

    Page<UserDto> listParamIndex(Page<User> page, String phone);
}
