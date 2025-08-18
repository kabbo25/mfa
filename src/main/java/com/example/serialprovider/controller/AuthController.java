package com.example.serialprovider.controller;

import com.example.serialprovider.auth.MultiFactorAuthenticationManager;
import com.example.serialprovider.auth.handler.MultiFactorAuthenticationFailureHandler;
import com.example.serialprovider.auth.handler.MultiFactorAuthenticationSuccessHandler;
import com.example.serialprovider.auth.token.OnboardingAuthenticationToken;
import com.example.serialprovider.auth.token.OtpAuthenticationToken;
import com.example.serialprovider.auth.token.UsernamePasswordAuthenticationToken;
import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.service.AuthenticationSettingsService;
import com.example.serialprovider.service.OnboardingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final MultiFactorAuthenticationManager authenticationManager;
    private final MultiFactorAuthenticationSuccessHandler successHandler;
    private final MultiFactorAuthenticationFailureHandler failureHandler;
    private final AuthenticationSession authenticationSession;
    private final AuthenticationSettingsService settingsService;

    public AuthController(MultiFactorAuthenticationManager authenticationManager,
                           MultiFactorAuthenticationSuccessHandler successHandler,
                           MultiFactorAuthenticationFailureHandler failureHandler,
                           AuthenticationSession authenticationSession,
                           AuthenticationSettingsService settingsService) {
        this.authenticationManager = authenticationManager;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.authenticationSession = authenticationSession;
        this.settingsService = settingsService;
    }

    @PostMapping("/login")
    public void login(@RequestBody LoginRequest request, 
                     HttpServletRequest servletRequest,
                     HttpServletResponse servletResponse) throws IOException {
        try {
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
            
            Authentication result = authenticationManager.authenticate(authToken);
            successHandler.onAuthenticationSuccess(servletRequest, servletResponse, result);
            
        } catch (AuthenticationException e) {
            failureHandler.onAuthenticationFailure(servletRequest, servletResponse, e);
        }
    }

    @PostMapping("/otp")
    public void verifyOtp(@RequestBody OtpRequest request,
                         HttpServletRequest servletRequest,
                         HttpServletResponse servletResponse) throws IOException {
        
        // Check if OTP step is enabled
        if (!settingsService.isOtpEnabled()) {
            servletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"error\":\"OTP authentication is disabled\"}");
            return;
        }
        
        try {
            OtpAuthenticationToken authToken = 
                new OtpAuthenticationToken(authenticationSession.getUsername(), request.getOtp());
            
            Authentication result = authenticationManager.authenticate(authToken);
            successHandler.onAuthenticationSuccess(servletRequest, servletResponse, result);
            
        } catch (AuthenticationException e) {
            failureHandler.onAuthenticationFailure(servletRequest, servletResponse, e);
        }
    }

    @PostMapping("/onboard")
    public void completeOnboarding(@RequestBody OnboardingService.OnboardingData data,
                                  HttpServletRequest servletRequest,
                                  HttpServletResponse servletResponse) throws IOException {
        
        // Check if onboarding step is enabled
        if (!settingsService.isOnboardingEnabled()) {
            servletResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            servletResponse.setContentType("application/json");
            servletResponse.getWriter().write("{\"error\":\"Onboarding authentication is disabled\"}");
            return;
        }
        
        try {
            OnboardingAuthenticationToken authToken = 
                new OnboardingAuthenticationToken(authenticationSession.getUsername(), data);
            
            Authentication result = authenticationManager.authenticate(authToken);
            successHandler.onAuthenticationSuccess(servletRequest, servletResponse, result);
            
        } catch (AuthenticationException e) {
            failureHandler.onAuthenticationFailure(servletRequest, servletResponse, e);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        String username = authenticationSession.getUsername();
        return ResponseEntity.ok(Map.of(
            "user", username != null ? username : "null",
            "currentState", authenticationSession.getState(),
            "isExpired", authenticationSession.isExpired(),
            "onboardingCompleted", authenticationSession.isOnboardingCompleted(),
            "settings", Map.of(
                "otpEnabled", settingsService.isOtpEnabled(),
                "onboardingEnabled", settingsService.isOnboardingEnabled(),
                "flowDescription", settingsService.getCurrentSettings().getFlowDescription()
            )
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        authenticationSession.reset();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    // Request DTOs
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class OtpRequest {
        private String otp;

        public String getOtp() { return otp; }
        public void setOtp(String otp) { this.otp = otp; }
    }
}