package com.example.rbac.security.config;

import com.example.rbac.security.common.Status;
import com.example.rbac.security.exception.SecurityException;
import com.example.rbac.security.service.CustomUserDetailsService;
import com.example.rbac.security.util.JwtUtil;
import com.example.rbac.security.util.ResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Jwt 认证过滤器
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-12-10 15:15
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CustomConfig customConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (checkIgnores(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = jwtUtil.getJwtFromRequest(request);

        if (StringUtils.hasText(jwt)) {
            try {
                String username = jwtUtil.getUsernameFromJWT(jwt);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
            } catch (SecurityException e) {
                ResponseUtil.renderJson(response, e);
            }
        } else {
            ResponseUtil.renderJson(response, Status.UNAUTHORIZED, null);
        }

    }

    /**
     * 请求是否不需要进行权限拦截
     *
     * @param request 当前请求
     * @return true - 忽略，false - 不忽略
     */
    private boolean checkIgnores(HttpServletRequest request) {
        String method = request.getMethod();

        // HttpMethod.resolve was removed in Spring 6; valueOf never returns null, it builds an
        // instance for methods it does not know about.
        HttpMethod httpMethod = HttpMethod.valueOf(method);

        IgnoreConfig config = customConfig.getIgnores();

        // HttpMethod is no longer an enum in Spring 6, so a switch is not possible here.
        Map<HttpMethod, List<String>> ignoresByMethod = Map.of(
                HttpMethod.GET, config.getGet(),
                HttpMethod.PUT, config.getPut(),
                HttpMethod.HEAD, config.getHead(),
                HttpMethod.POST, config.getPost(),
                HttpMethod.PATCH, config.getPatch(),
                HttpMethod.TRACE, config.getTrace(),
                HttpMethod.DELETE, config.getDelete(),
                HttpMethod.OPTIONS, config.getOptions());

        Set<String> ignores = new HashSet<>(ignoresByMethod.getOrDefault(httpMethod, List.of()));
        ignores.addAll(config.getPattern());

        if (!CollectionUtils.isEmpty(ignores)) {
            for (String ignore : ignores) {
                // AntPathRequestMatcher was removed in Spring Security 7.
                RequestMatcher matcher = PathPatternRequestMatcher.withDefaults().matcher(httpMethod, ignore);
                if (matcher.matches(request)) {
                    return true;
                }
            }
        }

        return false;
    }

}
