package com.example.serialprovider.config;

import com.example.serialprovider.auth.MultiFactorAuthenticationFilterOld;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

public class SecurityConfigOld {
    
    private final MultiFactorAuthenticationFilterOld multiFactorAuthenticationFilter;
    
    public SecurityConfigOld(MultiFactorAuthenticationFilterOld multiFactorAuthenticationFilter) {
        this.multiFactorAuthenticationFilter = multiFactorAuthenticationFilter;
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/", "/login", "/otp", "/onboarding", "/dashboard", "/login.html", "/otp.html", "/onboarding.html", "/dashboard.html", "/js/**", "/css/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(multiFactorAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .headers(headers -> headers.frameOptions().disable()) // For H2 console
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());
            
        return http.build();
    }
}