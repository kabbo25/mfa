package com.example.serialprovider.auth.handler;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.AuthenticationState;
import com.example.serialprovider.service.AuthenticationSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class MultiFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationSession authenticationSession;
    private final AuthenticationSettingsService settingsService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthenticationSuccessHandler primarySuccessHandler;
    private final AuthenticationSuccessHandler otpSuccessHandler;
    private final AuthenticationSuccessHandler onboardingSuccessHandler;

    public MultiFactorAuthenticationSuccessHandler(AuthenticationSession authenticationSession,
                                                 AuthenticationSettingsService settingsService) {
        this.authenticationSession = authenticationSession;
        this.settingsService = settingsService;
        this.primarySuccessHandler = new SimpleUrlAuthenticationSuccessHandler("/dashboard");
        this.otpSuccessHandler = new SimpleUrlAuthenticationSuccessHandler("/otp");
        this.onboardingSuccessHandler = new SimpleUrlAuthenticationSuccessHandler("/onboarding");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        AuthenticationState currentState = authenticationSession.getState();
        
        // Intelligent routing based on authentication state and user settings
        // Similar to the TwoFactorAuthenticationSuccessHandler pattern
        
        if (currentState == AuthenticationState.USERNAME_PASSWORD_VERIFIED) {
            // After password authentication, check what's next
            if (multiFactorRequired()) {
                String nextStep = getNextRequiredStep();
                if ("otp".equals(nextStep)) {
                    // Store intermediate authentication for OTP step
                    SecurityContextHolder.getContext().setAuthentication(
                        createIntermediateAuthentication(authentication, "PASSWORD_VERIFIED")
                    );
                    sendJsonResponse(response, "Credentials verified. OTP sent.", "otp");
                    return;
                } else if ("onboarding".equals(nextStep)) {
                    // Store intermediate authentication for onboarding step
                    SecurityContextHolder.getContext().setAuthentication(
                        createIntermediateAuthentication(authentication, "PASSWORD_VERIFIED")
                    );
                    sendJsonResponse(response, "Credentials verified. Please complete onboarding.", "onboarding");
                    return;
                }
            }
            // If no additional steps required, proceed to dashboard
            this.primarySuccessHandler.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        
        if (currentState == AuthenticationState.OTP_VERIFIED) {
            // After OTP verification, check if onboarding is required
            if (settingsService.isOnboardingEnabled()) {
                // Store intermediate authentication for onboarding step
                SecurityContextHolder.getContext().setAuthentication(
                    createIntermediateAuthentication(authentication, "OTP_VERIFIED")
                );
                sendJsonResponse(response, "OTP verified. Please complete onboarding.", "onboarding");
                return;
            }
            // If no onboarding required, proceed to dashboard
            this.primarySuccessHandler.onAuthenticationSuccess(request, response, authentication);
            return;
        }
        
        if (currentState == AuthenticationState.FULLY_AUTHENTICATED) {
            // All authentication steps completed, send JSON response
            sendJsonResponse(response, "Authentication completed successfully!", "dashboard");
            return;
        }
        
        // Default case - redirect to login
        sendJsonResponse(response, "Please start authentication.", "login");
    }
    
    private boolean multiFactorRequired() {
        return settingsService.isOtpEnabled() || settingsService.isOnboardingEnabled();
    }
    
    private String getNextRequiredStep() {
        if (settingsService.isOtpEnabled()) {
            return "otp";
        } else if (settingsService.isOnboardingEnabled()) {
            return "onboarding";
        }
        return "dashboard";
    }
    
    private Authentication createIntermediateAuthentication(Authentication originalAuth, String step) {
        // Create an intermediate authentication token that indicates partial completion
        // This prevents access to protected resources until full authentication is complete
        return new org.springframework.security.authentication.AnonymousAuthenticationToken(
            step, originalAuth.getPrincipal(), originalAuth.getAuthorities()
        );
    }
    
    private void sendJsonResponse(HttpServletResponse response, String message, String nextStep) throws IOException {
        response.setContentType("application/json");
        Map<String, Object> responseBody = Map.of(
            "message", message,
            "nextStep", nextStep,
            "currentState", authenticationSession.getState(),
            "user", authenticationSession.getUsername() != null ? authenticationSession.getUsername() : "null"
        );
        objectMapper.writeValue(response.getWriter(), responseBody);
    }
}