package com.example.serialprovider.auth.step;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.service.AuthenticationSettingsService;
import com.example.serialprovider.service.UserCredentialsService;
import com.example.serialprovider.service.OtpService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class PasswordAuthenticationStep implements AuthenticationStep {
    
    public static final String STEP_ID = "password";
    private static final int ORDER = 1;
    
    private final UserCredentialsService credentialsService;
    private final OtpService otpService;
    private final AuthenticationSettingsService settingsService;
    
    public PasswordAuthenticationStep(UserCredentialsService credentialsService,
                                    OtpService otpService,
                                    AuthenticationSettingsService settingsService) {
        this.credentialsService = credentialsService;
        this.otpService = otpService;
        this.settingsService = settingsService;
    }
    
    @Override
    public String getStepId() {
        return STEP_ID;
    }
    
    @Override
    public String getStepName() {
        return "Username/Password Authentication";
    }
    
    @Override
    public int getOrder() {
        return ORDER;
    }
    
    @Override
    public boolean isEnabled() {
        // Password authentication is always enabled (it's the foundation)
        return true;
    }
    
    @Override
    public boolean isCompleted(AuthenticationSession session) {
        return session.getCompletedSteps().contains(STEP_ID);
    }
    
    @Override
    public boolean canAccess(AuthenticationSession session) {
        // Password step is always accessible as the first step
        return true;
    }
    
    @Override
    public AuthenticationStepResult processAuthentication(Authentication authentication, AuthenticationSession session) {
        try {
            String username = authentication.getName();
            String password = (String) authentication.getCredentials();
            
            // Validate credentials
            if (!credentialsService.validateCredentials(username, password)) {
                return AuthenticationStepResult.failure()
                    .message("Invalid username or password")
                    .error(new RuntimeException("Invalid credentials"))
                    .build();
            }
            
            // Update session
            session.setUsername(username);
            session.addCompletedStep(STEP_ID);
            
            // Generate OTP if next step requires it
            if (settingsService.isOtpEnabled()) {
                String otpCode = otpService.generateOtp();
                otpService.sendOtp(username, otpCode);
                session.setOtpCode(otpCode);
            }
            
            // Determine next step
            String nextStep = getNextStep(session);
            boolean isFullyCompleted = nextStep == null;
            
            // Build authorities
            List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("STEP_1_COMPLETED")
            );
            
            if (isFullyCompleted) {
                authorities = List.of(
                    new SimpleGrantedAuthority("STEP_1_COMPLETED"),
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
                .message("Authentication failed: " + e.getMessage())
                .error(e)
                .build();
        }
    }
    
    @Override
    public String getNextStep(AuthenticationSession session) {
        if (settingsService.isOtpEnabled()) {
            return "otp";
        } else if (settingsService.isOnboardingEnabled()) {
            return "onboarding";
        }
        return null; // No more steps - fully authenticated
    }
    
    @Override
    public String getStepUrl() {
        return "/auth/login";
    }
    
    @Override
    public String getSuccessMessage() {
        if (settingsService.isOtpEnabled()) {
            return "Credentials verified. OTP sent.";
        } else if (settingsService.isOnboardingEnabled()) {
            return "Credentials verified. Please complete onboarding.";
        }
        return "Authentication completed successfully!";
    }
    
    @Override
    public void resetStep(AuthenticationSession session) {
        session.removeCompletedStep(STEP_ID);
        session.setUsername(null);
        // Don't clear OTP here as it might be needed for other steps
    }
}