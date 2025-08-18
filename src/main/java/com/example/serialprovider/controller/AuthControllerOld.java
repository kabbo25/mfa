package com.example.serialprovider.controller;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.AuthenticationState;
import com.example.serialprovider.service.OnboardingService;
import com.example.serialprovider.service.OtpService;
import com.example.serialprovider.service.UserCredentialsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

public class AuthControllerOld {
    
    private final UserCredentialsService userCredentialsService;
    private final OtpService otpService;
    private final OnboardingService onboardingService;
    private final AuthenticationSession authenticationSession;
    
    public AuthControllerOld(UserCredentialsService userCredentialsService,
                         OtpService otpService,
                         OnboardingService onboardingService,
                         AuthenticationSession authenticationSession) {
        this.userCredentialsService = userCredentialsService;
        this.otpService = otpService;
        this.onboardingService = onboardingService;
        this.authenticationSession = authenticationSession;
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        // Check if we can progress to this state
        if (!authenticationSession.canProgressTo(AuthenticationState.USERNAME_PASSWORD_VERIFIED)) {
            authenticationSession.reset();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid authentication state", "currentState", authenticationSession.getState()));
        }
        
        // Validate credentials
        if (!userCredentialsService.validateCredentials(request.getUsername(), request.getPassword())) {
            authenticationSession.reset();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid credentials"));
        }
        
        // Update session state
        authenticationSession.setUsername(request.getUsername());
        authenticationSession.setState(AuthenticationState.USERNAME_PASSWORD_VERIFIED);
        
        // Generate and send OTP
        String otpCode = otpService.generateOtp();
        authenticationSession.setOtpCode(otpCode);
        otpService.sendOtp(request.getUsername(), otpCode);
        
        return ResponseEntity.ok(Map.of(
            "message", "Credentials verified. OTP sent.",
            "nextStep", "otp",
            "currentState", authenticationSession.getState()
        ));
    }
    
    @PostMapping("/otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody OtpRequest request) {
        // Check if we can progress to this state
        if (!authenticationSession.canProgressTo(AuthenticationState.OTP_VERIFIED)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Must complete username/password step first", "currentState", authenticationSession.getState()));
        }
        
        // Validate OTP
        if (!otpService.validateOtp(authenticationSession.getOtpCode(), request.getOtp())) {
            authenticationSession.reset();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid OTP"));
        }
        
        // Update session state
        authenticationSession.setState(AuthenticationState.OTP_VERIFIED);
        
        return ResponseEntity.ok(Map.of(
            "message", "OTP verified. Please complete onboarding.",
            "nextStep", "onboarding",
            "currentState", authenticationSession.getState()
        ));
    }
    
    @PostMapping("/onboard")
    public ResponseEntity<Map<String, Object>> completeOnboarding(@RequestBody OnboardingService.OnboardingData data) {
        // Check if we can progress to this state
        if (!authenticationSession.canProgressTo(AuthenticationState.FULLY_AUTHENTICATED)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Must complete OTP step first", "currentState", authenticationSession.getState()));
        }
        
        // Process onboarding
        if (!onboardingService.processOnboarding(authenticationSession.getUsername(), data)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid onboarding data"));
        }
        
        // Complete authentication
        authenticationSession.setState(AuthenticationState.FULLY_AUTHENTICATED);
        authenticationSession.setOnboardingCompleted(true);
        
        return ResponseEntity.ok(Map.of(
            "message", "Authentication completed successfully!",
            "user", authenticationSession.getUsername(),
            "currentState", authenticationSession.getState()
        ));
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        String username = authenticationSession.getUsername();
        return ResponseEntity.ok(Map.of(
            "user", username != null ? username : "null",
            "currentState", authenticationSession.getState(),
            "isExpired", authenticationSession.isExpired(),
            "onboardingCompleted", authenticationSession.isOnboardingCompleted()
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
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    public static class OtpRequest {
        private String otp;
        
        public String getOtp() {
            return otp;
        }
        
        public void setOtp(String otp) {
            this.otp = otp;
        }
    }
}