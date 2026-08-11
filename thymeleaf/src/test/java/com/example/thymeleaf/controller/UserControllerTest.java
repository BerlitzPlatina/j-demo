package com.example.thymeleaf.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;

import com.example.thymeleaf.model.User;

class UserControllerTest {

    private final UserController userController = new UserController();

    @Test
    void loginWithValidCredentialsShouldRedirectToHome() {
        User user = new User();
        user.setName("admin");
        user.setPassword("123456");
        MockHttpServletRequest request = new MockHttpServletRequest();

        ModelAndView view = userController.login(user, request);

        assertThat(view.getViewName()).isEqualTo("redirect:/");
        assertThat(request.getSession().getAttribute("user")).isSameAs(user);
    }

    @Test
    void loginWithInvalidCredentialsShouldShowLoginPage() {
        User user = new User();
        user.setName("admin");
        user.setPassword("wrong");
        MockHttpServletRequest request = new MockHttpServletRequest();

        ModelAndView view = userController.login(user, request);

        assertThat(view.getViewName()).isEqualTo("page/login");
        assertThat(view.getModel().get("error")).isEqualTo("Tài khoản hoặc mật khẩu không đúng");
        assertThat(request.getSession().getAttribute("user")).isNull();
    }
}
