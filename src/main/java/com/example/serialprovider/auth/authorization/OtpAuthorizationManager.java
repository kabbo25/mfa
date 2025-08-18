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
public class OtpAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final AuthenticationSession authenticationSession;
    private final AuthenticationSettingsService settingsService;

    public OtpAuthorizationManager(AuthenticationSession authenticationSession,
                                 AuthenticationSettingsService settingsService) {
        this.authenticationSession = authenticationSession;
        this.settingsService = settingsService;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, 
                                     RequestAuthorizationContext requestContext) {
        
        Authentication auth = authentication.get();
        
        // Check if OTP is enabled in settings
        if (!settingsService.isOtpEnabled()) {
            return new AuthorizationDecision(false);
        }
        
        // Check authentication state progression
        AuthenticationState currentState = authenticationSession.getState();
        
        // Allow access to OTP endpoints only if:
        // 1. User has completed password authentication
        // 2. OTP is enabled in settings
        // 3. Session is not expired
        if (currentState == AuthenticationState.USERNAME_PASSWORD_VERIFIED && 
            !authenticationSession.isExpired()) {
            return new AuthorizationDecision(true);
        }
        
        // Additional security checks could include:
        // - Rate limiting OTP attempts
        // - IP address validation
        // - Time window restrictions for OTP submission
        // - Device fingerprinting
        
        return new AuthorizationDecision(false);
    }
}