package com.example.rbac.security.config;

import com.example.rbac.security.service.CustomUserDetailsService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Security 配置
 * </p>
 * <p>
 * Written for Spring Security 7: {@code WebSecurityConfigurerAdapter} was removed in 6.0, so the
 * configuration is expressed as a {@link SecurityFilterChain} bean instead of overridden methods.
 * </p>
 *
 * @author yangkai.shen
 * @date Created in 2018-12-07 16:46
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CustomConfig.class)
public class SecurityConfig {

    private final CustomConfig customConfig;
    private final AccessDeniedHandler accessDeniedHandler;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RbacAuthorityService rbacAuthorityService;

    public SecurityConfig(CustomConfig customConfig, AccessDeniedHandler accessDeniedHandler, CustomUserDetailsService customUserDetailsService, JwtAuthenticationFilter jwtAuthenticationFilter, RbacAuthorityService rbacAuthorityService) {
        this.customConfig = customConfig;
        this.accessDeniedHandler = accessDeniedHandler;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rbacAuthorityService = rbacAuthorityService;
    }

    @Bean
    public BCryptPasswordEncoder encoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Replaces the old {@code configure(AuthenticationManagerBuilder)} override. AuthController
     * injects this bean to authenticate a login request itself.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(encoder());
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        RequestMatcher[] ignored = ignoredMatchers();

        // @formatter:off
        http.cors(cors -> {})
                // 关闭 CSRF
                .csrf(csrf -> csrf.disable())
                // 登录行为由自己实现，参考 AuthController#login
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                // 登出行为由自己实现，参考 AuthController#logout
                .logout(logout -> logout.disable())

                // 认证请求
                .authorizeHttpRequests(auth -> auth
                        // The urls configured under custom.ignores are open to everyone. In the old
                        // code these were registered through WebSecurity#ignoring, which skipped the
                        // filter chain entirely; permitAll keeps them reachable while still letting
                        // the chain run.
                        .requestMatchers(ignored).permitAll()
                        // 所有其他请求都需要登录访问，并通过 RBAC 动态 url 认证
                        .anyRequest().access(rbacAuthorizationManager()))

                // Session 管理，因为使用了JWT，所以这里不管理Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 异常处理
                .exceptionHandling(exception -> exception.accessDeniedHandler(accessDeniedHandler));
        // @formatter:on

        // 添加自定义 JWT 过滤器
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Delegates the decision to {@link RbacAuthorityService}. The old configuration did this with
     * the SpEL string {@code @rbacAuthorityService.hasPermission(request,authentication)}; an
     * {@link AuthorizationManager} does the same thing and is checked by the compiler.
     *
     * @return authorization manager granting access only to authenticated users that hold a
     * matching permission
     */
    private AuthorizationManager<RequestAuthorizationContext> rbacAuthorizationManager() {
        return (authentication, context) -> {
            Authentication auth = authentication.get();
            if (auth == null || !auth.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            return new AuthorizationDecision(rbacAuthorityService.hasPermission(context.getRequest(), auth));
        };
    }

    /**
     * 放行所有不需要登录就可以访问的请求，参见 AuthController
     *
     * @return matchers for every url listed under the custom.ignores configuration
     */
    private RequestMatcher[] ignoredMatchers() {
        IgnoreConfig ignores = customConfig.getIgnores();
        List<RequestMatcher> matchers = new ArrayList<>();

        PathPatternRequestMatcher.Builder builder = PathPatternRequestMatcher.withDefaults();

        ignores.getGet().forEach(url -> matchers.add(builder.matcher(HttpMethod.GET, url)));
        ignores.getPost().forEach(url -> matchers.add(builder.matcher(HttpMethod.POST, url)));
        ignores.getDelete().forEach(url -> matchers.add(builder.matcher(HttpMethod.DELETE, url)));
        ignores.getPut().forEach(url -> matchers.add(builder.matcher(HttpMethod.PUT, url)));
        ignores.getHead().forEach(url -> matchers.add(builder.matcher(HttpMethod.HEAD, url)));
        ignores.getPatch().forEach(url -> matchers.add(builder.matcher(HttpMethod.PATCH, url)));
        ignores.getOptions().forEach(url -> matchers.add(builder.matcher(HttpMethod.OPTIONS, url)));
        ignores.getTrace().forEach(url -> matchers.add(builder.matcher(HttpMethod.TRACE, url)));

        // 按照请求格式忽略，不考虑请求方法
        ignores.getPattern().forEach(url -> matchers.add(builder.matcher(url)));

        return matchers.toArray(new RequestMatcher[0]);
    }
}
