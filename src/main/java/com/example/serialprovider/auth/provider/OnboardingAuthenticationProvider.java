package com.example.serialprovider.auth.provider;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.AuthenticationState;
import com.example.serialprovider.auth.token.OnboardingAuthenticationToken;
import com.example.serialprovider.service.AuthenticationSettingsService;
import com.example.serialprovider.service.OnboardingService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OnboardingAuthenticationProvider implements AuthenticationProvider {

    private final OnboardingService onboardingService;
    private final AuthenticationSession authenticationSession;
    private final AuthenticationSettingsService settingsService;

    public OnboardingAuthenticationProvider(OnboardingService onboardingService,
                                          AuthenticationSession authenticationSession,
                                          AuthenticationSettingsService settingsService) {
        this.onboardingService = onboardingService;
        this.authenticationSession = authenticationSession;
        this.settingsService = settingsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        OnboardingService.OnboardingData onboardingData = 
            (OnboardingService.OnboardingData) authentication.getCredentials();

        // Check if onboarding is enabled
        if (!settingsService.isOnboardingEnabled()) {
            throw new BadCredentialsException("Onboarding authentication is disabled");
        }

        // Validate previous step completed based on current settings
        AuthenticationState requiredPreviousState = settingsService.isOtpEnabled() 
            ? AuthenticationState.OTP_VERIFIED 
            : AuthenticationState.USERNAME_PASSWORD_VERIFIED;
            
        if (authenticationSession.getState() != requiredPreviousState ||
            !username.equals(authenticationSession.getUsername())) {
            String requiredStep = settingsService.isOtpEnabled() ? "OTP" : "username/password";
            throw new BadCredentialsException("Must complete " + requiredStep + " step first");
        }

        // Process onboarding
        if (!onboardingService.processOnboarding(username, onboardingData)) {
            throw new BadCredentialsException("Invalid onboarding data");
        }

        // Update session state - fully authenticated!
        authenticationSession.setState(AuthenticationState.FULLY_AUTHENTICATED);
        authenticationSession.setOnboardingCompleted(true);

        // Build authorities based on completed steps
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("STEP_1_COMPLETED"),
            new SimpleGrantedAuthority("STEP_3_COMPLETED"), // Onboarding completed
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority("FULLY_AUTHENTICATED")
        );
        
        // Add step 2 authority if OTP was enabled
        if (settingsService.isOtpEnabled()) {
            authorities = List.of(
                new SimpleGrantedAuthority("STEP_1_COMPLETED"),
                new SimpleGrantedAuthority("STEP_2_COMPLETED"),
                new SimpleGrantedAuthority("STEP_3_COMPLETED"),
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("FULLY_AUTHENTICATED")
            );
        }

        // Return fully authenticated token
        return new OnboardingAuthenticationToken(
            username,
            null, // Clear onboarding data
            authorities
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OnboardingAuthenticationToken.class.isAssignableFrom(authentication);
    }
}