package com.yuxiang.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                // 暂时沿用商品接口的配置，后续接入 CSRF Token
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        "/api/products",
                        "/api/products/**"
                ))

                .authorizeHttpRequests(auth -> auth
                        // 页面样式、脚本可以公开访问
                        .requestMatchers(
                                "/style.css",
                                "/app.js",
                                "/favicon.ico"
                        ).permitAll()

                        // 商品页面和接口都需要登录
                        .anyRequest().authenticated()
                )

                // 使用 Spring Security 自带的登录页
                .formLogin(form -> form
                        .defaultSuccessUrl("/index.html", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder) {

        UserDetails admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin123!"))
                .roles("ADMIN")
                .build();

        UserDetails user = User.builder()
                .username("user")
                .password(passwordEncoder.encode("User123!"))
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user);
    }
}