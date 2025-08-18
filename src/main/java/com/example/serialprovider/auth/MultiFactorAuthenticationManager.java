package com.example.serialprovider.auth;

import com.example.serialprovider.auth.provider.OnboardingAuthenticationProvider;
import com.example.serialprovider.auth.provider.OtpAuthenticationProvider;
import com.example.serialprovider.auth.provider.PasswordAuthenticationProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MultiFactorAuthenticationManager implements AuthenticationManager {

    private final List<AuthenticationProvider> providers;

    public MultiFactorAuthenticationManager(PasswordAuthenticationProvider passwordProvider,
                                          OtpAuthenticationProvider otpProvider,
                                          OnboardingAuthenticationProvider onboardingProvider) {
        this.providers = List.of(passwordProvider, otpProvider, onboardingProvider);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Class<? extends Authentication> toTest = authentication.getClass();
        
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(toTest)) {
                return provider.authenticate(authentication);
            }
        }
        
        throw new ProviderNotFoundException(
            "No AuthenticationProvider found for " + toTest.getName()
        );
    }
}