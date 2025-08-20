package com.example.serialprovider.auth.step;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.service.AuthenticationSettingsService;
import com.example.serialprovider.service.OnboardingService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OnboardingAuthenticationStep implements AuthenticationStep {
    
    public static final String STEP_ID = "onboarding";
    private static final int ORDER = 3;
    
    private final OnboardingService onboardingService;
    private final AuthenticationSettingsService settingsService;
    
    public OnboardingAuthenticationStep(OnboardingService onboardingService,
                                      AuthenticationSettingsService settingsService) {
        this.onboardingService = onboardingService;
        this.settingsService = settingsService;
    }
    
    @Override
    public String getStepId() {
        return STEP_ID;
    }
    
    @Override
    public String getStepName() {
        return "Profile Onboarding";
    }
    
    @Override
    public int getOrder() {
        return ORDER;
    }
    
    @Override
    public boolean isEnabled() {
        return settingsService.isOnboardingEnabled();
    }
    
    @Override
    public boolean isCompleted(AuthenticationSession session) {
        return session.getCompletedSteps().contains(STEP_ID);
    }
    
    @Override
    public boolean canAccess(AuthenticationSession session) {
        // Can only access onboarding step if:
        // 1. Onboarding is enabled
        // 2. Required previous steps are completed
        // 3. Session is not expired
        if (!isEnabled() || session.isExpired()) {
            return false;
        }
        
        // Must complete password first
        if (!session.getCompletedSteps().contains("password")) {
            return false;
        }
        
        // If OTP is enabled, must complete OTP step first
        if (settingsService.isOtpEnabled() && !session.getCompletedSteps().contains("otp")) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public AuthenticationStepResult processAuthentication(Authentication authentication, AuthenticationSession session) {
        try {
            if (!canAccess(session)) {
                String requiredStep = settingsService.isOtpEnabled() ? "OTP" : "password";
                return AuthenticationStepResult.failure()
                    .message("Must complete " + requiredStep + " step first")
                    .error(new RuntimeException("Invalid step access"))
                    .build();
            }
            
            String username = session.getUsername();
            OnboardingService.OnboardingData onboardingData = 
                (OnboardingService.OnboardingData) authentication.getCredentials();
            
            // Process onboarding
            if (!onboardingService.processOnboarding(username, onboardingData)) {
                return AuthenticationStepResult.failure()
                    .message("Invalid onboarding data")
                    .error(new RuntimeException("Onboarding validation failed"))
                    .build();
            }
            
            // Update session
            session.addCompletedStep(STEP_ID);
            session.setOnboardingCompleted(true);
            
            // Onboarding is always the final step
            String nextStep = getNextStep(session);
            boolean isFullyCompleted = true; // Onboarding is always the final step
            
            // Build authorities - onboarding completion means full authentication
            List<GrantedAuthority> authorities;
            
            if (settingsService.isOtpEnabled()) {
                // 3-step flow: Password + OTP + Onboarding
                authorities = List.of(
                    new SimpleGrantedAuthority("STEP_1_COMPLETED"),
                    new SimpleGrantedAuthority("STEP_2_COMPLETED"),
                    new SimpleGrantedAuthority("STEP_3_COMPLETED"),
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("FULLY_AUTHENTICATED")
                );
            } else {
                // 2-step flow: Password + Onboarding (no OTP)
                authorities = List.of(
                    new SimpleGrantedAuthority("STEP_1_COMPLETED"),
                    new SimpleGrantedAuthority("STEP_3_COMPLETED"),
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("FULLY_AUTHENTICATED")
                );
            }
            
            return AuthenticationStepResult.success()
                .completed(isFullyCompleted)
                .message(getSuccessMessage())
                .nextStep(nextStep)
                .grantedAuthorities(authorities)
                .build();
                
        } catch (Exception e) {
            return AuthenticationStepResult.failure()
                .message("Onboarding failed: " + e.getMessage())
                .error(e)
                .build();
        }
    }
    
    @Override
    public String getNextStep(AuthenticationSession session) {
        return null; // Onboarding is always the final step
    }
    
    @Override
    public String getStepUrl() {
        return "/auth/onboard";
    }
    
    @Override
    public String getSuccessMessage() {
        return "Authentication completed successfully!";
    }
    
    @Override
    public void resetStep(AuthenticationSession session) {
        session.removeCompletedStep(STEP_ID);
        session.setOnboardingCompleted(false);
    }
}