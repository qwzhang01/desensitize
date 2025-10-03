package com.qw.desensitize.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qw.desensitize.entity.WhiteList;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 对象白名单owner表 Mapper 接口
 * </p>
 *
 * @author avinzhang
 * @since 2025-10-03
 */
@Mapper
public interface WhiteListMapper extends BaseMapper<WhiteList> {

}
