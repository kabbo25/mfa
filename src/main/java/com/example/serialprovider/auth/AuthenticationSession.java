package com.example.serialprovider.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.time.LocalDateTime;

@Component
@SessionScope
public class AuthenticationSession {
    private String username;
    private AuthenticationState state = AuthenticationState.UNAUTHENTICATED;
    private LocalDateTime lastActivity = LocalDateTime.now();
    private String otpCode;
    private boolean onboardingCompleted = false;
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public AuthenticationState getState() {
        return state;
    }
    
    public void setState(AuthenticationState state) {
        this.state = state;
        this.lastActivity = LocalDateTime.now();
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public String getOtpCode() {
        return otpCode;
    }
    
    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
    
    public boolean isOnboardingCompleted() {
        return onboardingCompleted;
    }
    
    public void setOnboardingCompleted(boolean onboardingCompleted) {
        this.onboardingCompleted = onboardingCompleted;
    }
    
    public void reset() {
        this.username = null;
        this.state = AuthenticationState.UNAUTHENTICATED;
        this.lastActivity = LocalDateTime.now();
        this.otpCode = null;
        this.onboardingCompleted = false;
    }
    
    public boolean isExpired() {
        return lastActivity.isBefore(LocalDateTime.now().minusMinutes(30));
    }
    
    public boolean canProgressTo(AuthenticationState targetState) {
        return switch (targetState) {
            case UNAUTHENTICATED -> true;
            case USERNAME_PASSWORD_VERIFIED -> state == AuthenticationState.UNAUTHENTICATED;
            case OTP_VERIFIED -> state == AuthenticationState.USERNAME_PASSWORD_VERIFIED;
            case FULLY_AUTHENTICATED -> state == AuthenticationState.OTP_VERIFIED;
        };
    }
}