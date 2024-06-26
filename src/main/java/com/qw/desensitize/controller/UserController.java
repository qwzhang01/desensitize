package com.qw.desensitize.controller;

import com.qw.desensitize.common.R;
import com.qw.desensitize.dto.UserDto;
import com.qw.desensitize.entity.User;
import com.qw.desensitize.mapper.UserMapper;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理
 *
 * @author avinzhang
 */
@AllArgsConstructor
@RestController
@RequestMapping("api/user")
public class UserController {

    private final UserMapper mapper;

    @GetMapping("info")
    public R<UserDto> info(@RequestParam String userId) {
        // 获取数据库数据
        User user = mapper.selectById(userId);
        if (user == null) {
            return R.error();
        }
        // 转化为dto
        UserDto dto = new UserDto();
        BeanUtils.copyProperties(user, dto);

        // 标注需要脱敏
        dto.setSensitiveFlag(true);

        return R.success(dto);
    }

    @PostMapping("save")
    public R save(@RequestBody UserDto user) {
        User dto = new User();
        BeanUtils.copyProperties(user, dto);
        mapper.insert(dto);
        return R.success();
    }

    /**
     * 保存，保存后还需要使用
     * @param userDto
     * @return
     */
    @PostMapping("save-error")
    public R<UserDto> saveError(@RequestBody UserDto userDto) {
        // 保存
        User user = new User();
        BeanUtils.copyProperties(userDto, user);
        mapper.insert(user);

        // 使用
        BeanUtils.copyProperties(user, userDto);
        return R.success(userDto);
    }
}
