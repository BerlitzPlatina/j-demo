package com.example.orm.jpa.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;
import com.example.orm.jpa.entity.User;

import com.example.orm.jpa.repository.UserDao;

@RestController
public class ApiController {
    private final UserDao userDao;

    public ApiController(UserDao userDao) {
        this.userDao = userDao;
    }

    @GetMapping("/users/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        User user = userDao.findById(id).orElse(null);
        return Map.of("code", 200, "msg", "成功", "data", user);
    }
}
