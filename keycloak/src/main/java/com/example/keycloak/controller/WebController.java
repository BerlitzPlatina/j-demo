package com.example.keycloak.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.TreeMap;

/**
 * <p>
 * Thymeleaf pages for the browser login. Unlike {@link ApiController}, the user here is identified
 * by the HTTP session that the Authorization Code flow established, not by a bearer header.
 * </p>
 *
 * @author NamHoang
 */
@Controller
public class WebController {

    @GetMapping("/")
    public String index(@AuthenticationPrincipal OidcUser user) {
        return user == null ? "index" : "redirect:/home";
    }

    /**
     * Shown after a successful login. The access token on the page is the one Keycloak returned
     * from the code exchange; it can be sent to {@code /api/**} as {@code Authorization: Bearer ...}.
     */
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal OidcUser user,
                       @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client,
                       Model model) {
        model.addAttribute("username", user.getPreferredUsername());
        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("claims", new TreeMap<>(user.getIdToken().getClaims()));
        model.addAttribute("accessToken", client.getAccessToken().getTokenValue());
        model.addAttribute("expiresAt", client.getAccessToken().getExpiresAt());
        return "home";
    }
}
