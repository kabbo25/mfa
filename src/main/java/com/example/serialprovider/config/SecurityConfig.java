package com.example.serialprovider.config;

import com.example.serialprovider.auth.MultiFactorAuthenticationManager;
import com.example.serialprovider.auth.handler.MultiFactorAuthenticationFailureHandler;
import com.example.serialprovider.auth.handler.MultiFactorAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity(debug = false)
public class SecurityConfig {

    private final MultiFactorAuthenticationManager authenticationManager;
    private final MultiFactorAuthenticationSuccessHandler successHandler;
    private final MultiFactorAuthenticationFailureHandler failureHandler;

    public SecurityConfig(MultiFactorAuthenticationManager authenticationManager,
                           MultiFactorAuthenticationSuccessHandler successHandler,
                           MultiFactorAuthenticationFailureHandler failureHandler) {
        this.authenticationManager = authenticationManager;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/admin/**").permitAll() // For demo purposes - in production add proper admin auth
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/", "/login", "/otp", "/onboarding", "/dashboard", 
                               "/*.html", "/js/**", "/css/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/**").hasAuthority("FULLY_AUTHENTICATED")
                .anyRequest().authenticated()
            )
            .authenticationManager(authenticationManager)
            .headers(headers -> headers.frameOptions().disable()) // For H2 console
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}