package com.example.thymeleaf.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import com.example.thymeleaf.model.User;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/user")
@Slf4j
public class UserController {
    @PostMapping("/login")
    public ModelAndView login(User user, HttpServletRequest request) {
        ModelAndView mv = new ModelAndView();

        if ("admin".equals(user.getName()) && "123456".equals(user.getPassword())) {
            request.getSession().setAttribute("user", user);
            mv.setViewName("redirect:/");
        } else {
            mv.setViewName("page/login");
            mv.addObject("error", "Tài khoản hoặc mật khẩu không đúng");
            mv.addObject("user", user);
        }

        return mv;
    }

    @GetMapping("/login")
    public ModelAndView login() {
        return new ModelAndView("page/login");
    }
}
