package com.qw.desensitize.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qw.desensitize.common.R;
import com.qw.desensitize.dto.UserDto;
import com.qw.desensitize.dto.UserParam;
import com.qw.desensitize.entity.User;
import com.qw.desensitize.mapper.UserMapper;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import io.github.qwzhang01.desensitize.domain.MaskContext;
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

    @GetMapping("page")
    public R<Page<?>> page() {

        UserParam userParam = new UserParam();
        userParam.setPhoneNo("13900139002");

//        Page<UserDto> userPage = mapper.list(new Page<>(1, 10), userParam);


        QueryWrapper<User> query1 = Wrappers.query();
        query1.eq("phoneNo", userParam.getPhoneNo());
        Page<User> userPage = mapper.selectPage(new Page<>(1, 10), query1);

//        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
//        query.eq(User::getPhoneNo, new Encrypt("13900139002"));
//        Page<User> userPage = mapper.selectPage(new Page<>(1, 10), query);
        return R.ok(userPage);
    }

    @GetMapping("info")
    public R<UserDto> info(@RequestParam String userId) {
        // 获取数据库数据
        User user = mapper.selectById(userId);
        if (user == null) {
            return R.error("用户不存在");
        }
        // 转化为dto
        UserDto dto = new UserDto();
        BeanUtils.copyProperties(user, dto);

        // 标注需要脱敏
        MaskContext.start();

        return R.success(dto);
    }

    @PostMapping("save")
    public R<User> save(@RequestBody UserDto user) {
        User dto = new User();
        BeanUtils.copyProperties(user, dto);
        mapper.insert(dto);
        return R.success(dto);
    }


    @PostMapping("update")
    public R<User> update(@RequestBody UserDto user) {
        User dto = new User();
        BeanUtils.copyProperties(user, dto);
        mapper.updateById(dto);
        return R.success(dto);
    }

    /**
     * 保存，保存后还需要使用
     *
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
