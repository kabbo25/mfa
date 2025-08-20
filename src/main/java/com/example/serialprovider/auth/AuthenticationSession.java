package com.example.serialprovider.auth;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@SessionScope
public class AuthenticationSession {
    private String username;
    private Set<String> completedSteps = new HashSet<>();
    private LocalDateTime lastActivity = LocalDateTime.now();
    private String otpCode;
    private boolean onboardingCompleted = false;
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public Set<String> getCompletedSteps() {
        return new HashSet<>(completedSteps);
    }
    
    public void addCompletedStep(String stepId) {
        this.completedSteps.add(stepId);
        this.lastActivity = LocalDateTime.now();
    }
    
    public void removeCompletedStep(String stepId) {
        this.completedSteps.remove(stepId);
        this.lastActivity = LocalDateTime.now();
    }
    
    public boolean hasCompletedStep(String stepId) {
        return completedSteps.contains(stepId);
    }
    
    // Legacy method for backward compatibility
    @Deprecated
    public AuthenticationState getState() {
        if (completedSteps.isEmpty()) {
            return AuthenticationState.UNAUTHENTICATED;
        } else if (completedSteps.contains("password") && !completedSteps.contains("otp") && !completedSteps.contains("onboarding")) {
            return AuthenticationState.USERNAME_PASSWORD_VERIFIED;
        } else if (completedSteps.contains("otp") && !completedSteps.contains("onboarding")) {
            return AuthenticationState.OTP_VERIFIED;
        } else if (onboardingCompleted) {
            return AuthenticationState.FULLY_AUTHENTICATED;
        }
        return AuthenticationState.UNAUTHENTICATED;
    }
    
    // Legacy method for backward compatibility
    @Deprecated
    public void setState(AuthenticationState state) {
        this.lastActivity = LocalDateTime.now();
        // Convert state to completed steps for backward compatibility
        switch (state) {
            case UNAUTHENTICATED -> completedSteps.clear();
            case USERNAME_PASSWORD_VERIFIED -> {
                completedSteps.clear();
                completedSteps.add("password");
            }
            case OTP_VERIFIED -> {
                completedSteps.clear();
                completedSteps.add("password");
                completedSteps.add("otp");
            }
            case FULLY_AUTHENTICATED -> {
                completedSteps.add("password");
                if (completedSteps.contains("otp")) {
                    completedSteps.add("otp");
                }
                completedSteps.add("onboarding");
                onboardingCompleted = true;
            }
        }
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
        this.completedSteps.clear();
        this.lastActivity = LocalDateTime.now();
        this.otpCode = null;
        this.onboardingCompleted = false;
    }
    
    public boolean isExpired() {
        return lastActivity.isBefore(LocalDateTime.now().minusMinutes(30));
    }
    
    // Legacy method for backward compatibility
    @Deprecated
    public boolean canProgressTo(AuthenticationState targetState) {
        return switch (targetState) {
            case UNAUTHENTICATED -> true;
            case USERNAME_PASSWORD_VERIFIED -> completedSteps.isEmpty();
            case OTP_VERIFIED -> completedSteps.contains("password") && !completedSteps.contains("otp");
            case FULLY_AUTHENTICATED -> completedSteps.contains("password") && 
                (!completedSteps.contains("otp") || completedSteps.contains("otp")) &&
                !onboardingCompleted;
        };
    }
    
    /**
     * Check if user can access a specific authentication step
     */
    public boolean canAccessStep(String stepId) {
        return switch (stepId) {
            case "password" -> true; // Always accessible as first step
            case "otp" -> completedSteps.contains("password");
            case "onboarding" -> completedSteps.contains("password") && 
                                (!completedSteps.contains("otp") || completedSteps.contains("otp"));
            default -> false;
        };
    }
    
    /**
     * Get authentication progress information
     */
    public String getProgressDescription() {
        if (completedSteps.isEmpty()) {
            return "Not authenticated";
        }
        
        StringBuilder progress = new StringBuilder("Completed: ");
        progress.append(String.join(", ", completedSteps));
        
        if (onboardingCompleted) {
            progress.append(" (Fully authenticated)");
        }
        
        return progress.toString();
    }
}