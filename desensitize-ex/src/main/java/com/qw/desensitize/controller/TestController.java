package com.qw.desensitize.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("api/mytest")
public class TestController {

    private final Environment environment;

    @GetMapping("db-config")
    public RespEntity<Map<String, String>> testDbConfig() {
        Map<String, String> map = new HashMap<>(7);
        map.put("driverClassName", "com.mysql.cj.jdbc.Driver");
        map.put("url", environment.getProperty("spring.datasource.url"));
        map.put("userName", environment.getProperty("spring.datasource.username"));
        map.put("packages", "com.qw.desensitize");
        map.put("password", environment.getProperty("spring.datasource.password"));
        map.put("tables", "");
        map.put("prefix", "");
        return new RespEntity<>(200, "", map);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RespEntity<T> {
        private int status;
        private String error;
        private T data;
    }
}