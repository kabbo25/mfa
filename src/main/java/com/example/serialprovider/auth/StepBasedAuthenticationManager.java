package com.example.serialprovider.auth;

import com.example.serialprovider.auth.step.AuthenticationStepChain;
import com.example.serialprovider.auth.step.AuthenticationStepResult;
import com.example.serialprovider.auth.token.OnboardingAuthenticationToken;
import com.example.serialprovider.auth.token.OtpAuthenticationToken;
import com.example.serialprovider.auth.token.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Generic authentication manager that uses the step chain architecture.
 * Routes authentication requests to appropriate steps and handles their results.
 */
@Component
public class StepBasedAuthenticationManager implements AuthenticationManager {

    private final AuthenticationStepChain stepChain;
    private final AuthenticationSession authenticationSession;

    public StepBasedAuthenticationManager(AuthenticationStepChain stepChain,
                                        AuthenticationSession authenticationSession) {
        this.stepChain = stepChain;
        this.authenticationSession = authenticationSession;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        
        // Determine which step this authentication belongs to
        String stepId = determineStepId(authentication);
        
        if (stepId == null) {
            throw new BadCredentialsException("Unknown authentication type: " + authentication.getClass().getSimpleName());
        }
        
        // Process the authentication through the step chain
        AuthenticationStepResult result = stepChain.processStep(stepId, authentication, authenticationSession);
        
        if (!result.isSuccess()) {
            String message = result.getMessage() != null ? result.getMessage() : "Authentication failed";
            throw new BadCredentialsException(message, result.getError());
        }
        
        // Create and return the authentication result
        if (result.getAuthentication() != null) {
            return result.getAuthentication();
        }
        
        // Create a new authentication token with the granted authorities
        return createAuthenticationToken(authentication, result);
    }
    
    /**
     * Determine which authentication step this authentication belongs to
     */
    private String determineStepId(Authentication authentication) {
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return "password";
        } else if (authentication instanceof OtpAuthenticationToken) {
            return "otp";
        } else if (authentication instanceof OnboardingAuthenticationToken) {
            return "onboarding";
        }
        return null;
    }
    
    /**
     * Create an appropriate authentication token based on the step result
     */
    private Authentication createAuthenticationToken(Authentication originalAuth, AuthenticationStepResult result) {
        if (originalAuth instanceof UsernamePasswordAuthenticationToken) {
            return new UsernamePasswordAuthenticationToken(
                originalAuth.getPrincipal(),
                null, // Clear credentials for security
                result.getGrantedAuthorities()
            );
        } else if (originalAuth instanceof OtpAuthenticationToken) {
            return new OtpAuthenticationToken(
                originalAuth.getPrincipal().toString(),
                null, // Clear credentials for security
                result.getGrantedAuthorities()
            );
        } else if (originalAuth instanceof OnboardingAuthenticationToken) {
            return new OnboardingAuthenticationToken(
                originalAuth.getPrincipal().toString(),
                null, // Clear credentials for security
                result.getGrantedAuthorities()
            );
        }
        
        // Fallback - should not happen
        throw new BadCredentialsException("Cannot create authentication token for type: " + originalAuth.getClass().getSimpleName());
    }
}