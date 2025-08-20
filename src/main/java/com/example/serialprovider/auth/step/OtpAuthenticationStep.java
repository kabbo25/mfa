package com.example.serialprovider.auth.step;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.service.AuthenticationSettingsService;
import com.example.serialprovider.service.OtpService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OtpAuthenticationStep implements AuthenticationStep {
    
    public static final String STEP_ID = "otp";
    private static final int ORDER = 2;
    
    private final OtpService otpService;
    private final AuthenticationSettingsService settingsService;
    
    public OtpAuthenticationStep(OtpService otpService,
                               AuthenticationSettingsService settingsService) {
        this.otpService = otpService;
        this.settingsService = settingsService;
    }
    
    @Override
    public String getStepId() {
        return STEP_ID;
    }
    
    @Override
    public String getStepName() {
        return "One-Time Password Verification";
    }
    
    @Override
    public int getOrder() {
        return ORDER;
    }
    
    @Override
    public boolean isEnabled() {
        return settingsService.isOtpEnabled();
    }
    
    @Override
    public boolean isCompleted(AuthenticationSession session) {
        return session.getCompletedSteps().contains(STEP_ID);
    }
    
    @Override
    public boolean canAccess(AuthenticationSession session) {
        // Can only access OTP step if:
        // 1. OTP is enabled
        // 2. Password step is completed
        // 3. Session is not expired
        return isEnabled() && 
               session.getCompletedSteps().contains("password") &&
               !session.isExpired();
    }
    
    @Override
    public AuthenticationStepResult processAuthentication(Authentication authentication, AuthenticationSession session) {
        try {
            if (!canAccess(session)) {
                return AuthenticationStepResult.failure()
                    .message("Must complete password authentication first")
                    .error(new RuntimeException("Invalid step access"))
                    .build();
            }
            
            String username = session.getUsername();
            String otpCode = (String) authentication.getCredentials();
            
            // Validate OTP
            if (!otpService.validateOtp(username, otpCode)) {
                return AuthenticationStepResult.failure()
                    .message("Invalid OTP code")
                    .error(new RuntimeException("Invalid OTP"))
                    .build();
            }
            
            // Update session
            session.addCompletedStep(STEP_ID);
            
            // Determine next step
            String nextStep = getNextStep(session);
            boolean isFullyCompleted = nextStep == null;
            
            // Build authorities
            List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("STEP_1_COMPLETED"),
                new SimpleGrantedAuthority("STEP_2_COMPLETED")
            );
            
            if (isFullyCompleted) {
                authorities = List.of(
                    new SimpleGrantedAuthority("STEP_1_COMPLETED"),
                    new SimpleGrantedAuthority("STEP_2_COMPLETED"),
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
                .message("OTP verification failed: " + e.getMessage())
                .error(e)
                .build();
        }
    }
    
    @Override
    public String getNextStep(AuthenticationSession session) {
        if (settingsService.isOnboardingEnabled()) {
            return "onboarding";
        }
        return null; // No more steps - fully authenticated
    }
    
    @Override
    public String getStepUrl() {
        return "/auth/otp";
    }
    
    @Override
    public String getSuccessMessage() {
        if (settingsService.isOnboardingEnabled()) {
            return "OTP verified. Please complete onboarding.";
        }
        return "Authentication completed successfully!";
    }
    
    @Override
    public void resetStep(AuthenticationSession session) {
        session.removeCompletedStep(STEP_ID);
        session.setOtpCode(null);
    }
}