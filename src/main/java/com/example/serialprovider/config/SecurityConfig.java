package com.example.serialprovider.config;

import com.example.serialprovider.auth.StepBasedAuthenticationManager;
import com.example.serialprovider.auth.authorization.AdminAuthorizationManager;
import com.example.serialprovider.auth.authorization.ApiAuthorizationManager;
import com.example.serialprovider.auth.authorization.OnboardingAuthorizationManager;
import com.example.serialprovider.auth.authorization.OtpAuthorizationManager;
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

    private final StepBasedAuthenticationManager authenticationManager;
    private final MultiFactorAuthenticationSuccessHandler successHandler;
    private final MultiFactorAuthenticationFailureHandler failureHandler;
    private final AdminAuthorizationManager adminAuthorizationManager;
    private final ApiAuthorizationManager apiAuthorizationManager;
    private final OtpAuthorizationManager otpAuthorizationManager;
    private final OnboardingAuthorizationManager onboardingAuthorizationManager;

    public SecurityConfig(StepBasedAuthenticationManager authenticationManager,
                           MultiFactorAuthenticationSuccessHandler successHandler,
                           MultiFactorAuthenticationFailureHandler failureHandler,
                           AdminAuthorizationManager adminAuthorizationManager,
                           ApiAuthorizationManager apiAuthorizationManager,
                           OtpAuthorizationManager otpAuthorizationManager,
                           OnboardingAuthorizationManager onboardingAuthorizationManager) {
        this.authenticationManager = authenticationManager;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.adminAuthorizationManager = adminAuthorizationManager;
        this.apiAuthorizationManager = apiAuthorizationManager;
        this.otpAuthorizationManager = otpAuthorizationManager;
        this.onboardingAuthorizationManager = onboardingAuthorizationManager;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/admin/**").access(adminAuthorizationManager)
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/", "/login", "/otp", "/onboarding", "/dashboard", 
                               "/*.html", "/js/**", "/css/**", "/favicon.ico").permitAll()
                .requestMatchers("/otp").access(otpAuthorizationManager)
                .requestMatchers("/onboarding").access(onboardingAuthorizationManager)
                .requestMatchers("/api/**").access(apiAuthorizationManager)
                .anyRequest().authenticated()
            )
            .authenticationManager(authenticationManager)
            .headers(headers -> headers.frameOptions().disable()) // For H2 console
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}