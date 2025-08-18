package com.example.serialprovider.auth.authorization;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.AuthenticationState;
import com.example.serialprovider.service.AuthenticationSettingsService;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class OnboardingAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final AuthenticationSession authenticationSession;
    private final AuthenticationSettingsService settingsService;

    public OnboardingAuthorizationManager(AuthenticationSession authenticationSession,
                                        AuthenticationSettingsService settingsService) {
        this.authenticationSession = authenticationSession;
        this.settingsService = settingsService;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, 
                                     RequestAuthorizationContext requestContext) {
        
        Authentication auth = authentication.get();
        
        // Check if onboarding is enabled in settings
        if (!settingsService.isOnboardingEnabled()) {
            return new AuthorizationDecision(false);
        }
        
        AuthenticationState currentState = authenticationSession.getState();
        
        // Allow access to onboarding endpoints based on flow configuration:
        // - If OTP enabled: must complete password + OTP first
        // - If OTP disabled: must complete password first
        boolean canAccess = false;
        
        if (settingsService.isOtpEnabled()) {
            // Full 3-step flow: Password -> OTP -> Onboarding
            canAccess = (currentState == AuthenticationState.OTP_VERIFIED);
        } else {
            // 2-step flow: Password -> Onboarding (skip OTP)
            canAccess = (currentState == AuthenticationState.USERNAME_PASSWORD_VERIFIED);
        }
        
        // Additional security validation
        if (canAccess && !authenticationSession.isExpired()) {
            return new AuthorizationDecision(true);
        }
        
        // Additional security features could include:
        // - Profile completion requirements
        // - Terms of service acceptance validation
        // - Age verification requirements
        // - Data privacy consent validation
        
        return new AuthorizationDecision(false);
    }
}