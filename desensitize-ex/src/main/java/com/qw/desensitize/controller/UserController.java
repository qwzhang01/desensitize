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
import com.qw.desensitize.strategy.JoinDataScopeStrategy;
import com.qw.desensitize.strategy.WhereDataScopeStrategy;
import io.github.qwzhang01.desensitize.context.MaskContext;
import io.github.qwzhang01.desensitize.domain.Encrypt;
import io.github.qwzhang01.desensitize.scope.DataScopeHelper;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

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

    @GetMapping("page-mapper-obj")
    public R<Page<?>> pageMapperObj() {
        UserParam userParam = new UserParam();
        userParam.setPhone("13900139002");
        Page<UserDto> userPage = DataScopeHelper
                .strategy(JoinDataScopeStrategy.class)
                .execute(() -> mapper.listObj(new Page<>(1, 10), userParam));

        MaskContext.start();
        return R.ok(userPage);
    }

    @GetMapping("page-mapper-param")
    public R<Page<?>> pageMapperParam() {
        Page<UserDto> userPage = DataScopeHelper
                .strategy(WhereDataScopeStrategy.class)
                .execute(() -> mapper.listParam(new Page<>(1, 10), "13900139002"));
        MaskContext.start();
        return R.ok(userPage);
    }

    @GetMapping("page-mapper-param-index")
    public R<Page<?>> pageMapperParamIndex() {
        Page<UserDto> userPage = mapper.listParamIndex(new Page<>(1, 10), "13900139002");
        MaskContext.start();
        return R.ok(userPage);
    }

    @GetMapping("page-wrapper")
    public R<Page<?>> pageWrapper() {
        UserParam userParam = new UserParam();
        userParam.setPhone("13900139002");
        QueryWrapper<User> query1 = Wrappers.query();
        query1.eq("phoneNo", userParam.getPhone());
        Page<User> userPage = mapper.selectPage(new Page<>(1, 10), query1);
        MaskContext.start();
        return R.ok(userPage);
    }

    @GetMapping("page-lambda-wrapper")
    public R<Page<?>> pageLambdaWrapper() {
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
        query.eq(User::getPhoneNo, "13900139002");
        Page<User> userPage = mapper.selectPage(new Page<>(1, 10), query);
        MaskContext.start();
        return R.ok(userPage);
    }

    @GetMapping("page-type-handler")
    public R<Page<?>> pageTypeHandler() {
        LambdaQueryWrapper<User> query = Wrappers.lambdaQuery();
        query.eq(User::getPhoneNo, new Encrypt("13900139002"));
        Page<User> userPage = mapper.selectPage(new Page<>(1, 10), query);
        MaskContext.start();
        return R.ok(userPage);
    }

    @GetMapping("infos")
    public R<List<UserDto>> infos(@RequestParam String userId) {
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

        return R.success(Collections.singletonList(dto));
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
        MaskContext.start();
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
