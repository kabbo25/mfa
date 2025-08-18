package com.example.serialprovider.auth.provider;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.AuthenticationState;
import com.example.serialprovider.auth.token.UsernamePasswordAuthenticationToken;
import com.example.serialprovider.service.AuthenticationSettingsService;
import com.example.serialprovider.service.OtpService;
import com.example.serialprovider.service.UserCredentialsService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PasswordAuthenticationProvider implements AuthenticationProvider {

    private final UserCredentialsService userCredentialsService;
    private final OtpService otpService;
    private final AuthenticationSession authenticationSession;
    private final AuthenticationSettingsService settingsService;

    public PasswordAuthenticationProvider(UserCredentialsService userCredentialsService,
                                        OtpService otpService,
                                        AuthenticationSession authenticationSession,
                                        AuthenticationSettingsService settingsService) {
        this.userCredentialsService = userCredentialsService;
        this.otpService = otpService;
        this.authenticationSession = authenticationSession;
        this.settingsService = settingsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        // Validate credentials
        if (!userCredentialsService.validateCredentials(username, password)) {
            throw new BadCredentialsException("Invalid credentials");
        }

        // Update session state
        authenticationSession.setUsername(username);
        authenticationSession.setState(AuthenticationState.USERNAME_PASSWORD_VERIFIED);

        // Check if OTP is enabled and generate if needed
        if (settingsService.isOtpEnabled()) {
            String otpCode = otpService.generateOtp();
            authenticationSession.setOtpCode(otpCode);
            otpService.sendOtp(username, otpCode);
        }

        // Determine authorities based on what's completed and what's next
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("STEP_1_COMPLETED"));
        
        // If no more steps are required, mark as fully authenticated
        if (!settingsService.isOtpEnabled() && !settingsService.isOnboardingEnabled()) {
            authenticationSession.setState(AuthenticationState.FULLY_AUTHENTICATED);
            authorities = List.of(
                new SimpleGrantedAuthority("STEP_1_COMPLETED"),
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("FULLY_AUTHENTICATED")
            );
        }

        // Return authentication token
        return new UsernamePasswordAuthenticationToken(
            username, 
            null, // Clear password
            authorities
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}