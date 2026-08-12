package com.example.orm.jdbctemplate.controller;

import com.example.orm.jdbctemplate.entity.User;
import com.example.orm.jdbctemplate.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

import java.util.List;

/**
 * <p>
 * User Controller
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-10-15 13:58
 */
@RestController
@Slf4j
public class UserController {
    private final IUserService userService;

    @Autowired
    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/user")
    public Map<String, Object> save(@RequestBody User user) {
        Boolean save = userService.save(user);
        return Map.of("code", save ? 200 : 500, "msg", save ? "成功" : "失败", "data",
                save ? user : null);
    }

    @DeleteMapping("/user/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Boolean delete = userService.delete(id);
        return Map.of("code", delete ? 200 : 500, "msg", delete ? "成功" : "失败");
    }

    @PutMapping("/user/{id}")
    public Map<String, Object> update(@RequestBody User user, @PathVariable Long id) {
        Boolean update = userService.update(user, id);
        return Map.of("code", update ? 200 : 500, "msg", update ? "成功" : "失败", "data",
                update ? user : null);
    }

    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(@PathVariable Long id) {
        User user = userService.getUser(id);
        return Map.of("code", 200, "msg", "成功", "data", user);
    }

    @GetMapping("/user")
    public Map<String, Object> getUser(User user) {
        List<User> userList = userService.getUser(user);
        return Map.of("code", 200, "msg", "成功", "data", userList);
    }
}
