package com.example.serialprovider.auth.provider;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.AuthenticationState;
import com.example.serialprovider.auth.token.OtpAuthenticationToken;
import com.example.serialprovider.service.AuthenticationSettingsService;
import com.example.serialprovider.service.OtpService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OtpAuthenticationProvider implements AuthenticationProvider {

    private final OtpService otpService;
    private final AuthenticationSession authenticationSession;
    private final AuthenticationSettingsService settingsService;

    public OtpAuthenticationProvider(OtpService otpService,
                                   AuthenticationSession authenticationSession,
                                   AuthenticationSettingsService settingsService) {
        this.otpService = otpService;
        this.authenticationSession = authenticationSession;
        this.settingsService = settingsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String otpCode = (String) authentication.getCredentials();

        // Check if OTP is enabled
        if (!settingsService.isOtpEnabled()) {
            throw new BadCredentialsException("OTP authentication is disabled");
        }

        // Validate that step 1 is completed
        if (authenticationSession.getState() != AuthenticationState.USERNAME_PASSWORD_VERIFIED ||
            !username.equals(authenticationSession.getUsername())) {
            authenticationSession.reset();
            throw new BadCredentialsException("Must complete username/password step first");
        }

        // Validate OTP
        if (!otpService.validateOtp(authenticationSession.getOtpCode(), otpCode)) {
            authenticationSession.reset();
            throw new BadCredentialsException("Invalid OTP code");
        }

        // Update session state
        authenticationSession.setState(AuthenticationState.OTP_VERIFIED);

        // Determine authorities based on what's next
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("STEP_1_COMPLETED"),
            new SimpleGrantedAuthority("STEP_2_COMPLETED")
        );
        
        // If onboarding is disabled, mark as fully authenticated
        if (!settingsService.isOnboardingEnabled()) {
            authenticationSession.setState(AuthenticationState.FULLY_AUTHENTICATED);
            authorities = List.of(
                new SimpleGrantedAuthority("STEP_1_COMPLETED"),
                new SimpleGrantedAuthority("STEP_2_COMPLETED"),
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("FULLY_AUTHENTICATED")
            );
        }

        // Return authentication token
        return new OtpAuthenticationToken(
            username,
            null, // Clear OTP
            authorities
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return OtpAuthenticationToken.class.isAssignableFrom(authentication);
    }
}