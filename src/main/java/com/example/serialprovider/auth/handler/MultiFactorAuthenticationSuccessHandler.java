package com.example.serialprovider.auth.handler;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.AuthenticationState;
import com.example.serialprovider.service.AuthenticationSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class MultiFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationSession authenticationSession;
    private final AuthenticationSettingsService settingsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MultiFactorAuthenticationSuccessHandler(AuthenticationSession authenticationSession,
                                                 AuthenticationSettingsService settingsService) {
        this.authenticationSession = authenticationSession;
        this.settingsService = settingsService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        AuthenticationState currentState = authenticationSession.getState();
        response.setContentType("application/json");
        
        String nextStep;
        String message;
        
        // Use dynamic routing based on current state and settings
        switch (currentState) {
            case USERNAME_PASSWORD_VERIFIED -> {
                nextStep = settingsService.getNextStep("password");
                if ("otp".equals(nextStep)) {
                    message = "Credentials verified. OTP sent.";
                } else if ("onboarding".equals(nextStep)) {
                    message = "Credentials verified. Please complete onboarding.";
                } else {
                    message = "Authentication completed successfully!";
                }
            }
            case OTP_VERIFIED -> {
                nextStep = settingsService.getNextStep("otp");
                if ("onboarding".equals(nextStep)) {
                    message = "OTP verified. Please complete onboarding.";
                } else {
                    message = "Authentication completed successfully!";
                }
            }
            case FULLY_AUTHENTICATED -> {
                nextStep = "dashboard";
                message = "Authentication completed successfully!";
            }
            default -> {
                nextStep = "login";
                message = "Please start authentication.";
            }
        }

        Map<String, Object> responseBody = Map.of(
            "message", message,
            "nextStep", nextStep,
            "currentState", currentState,
            "user", authenticationSession.getUsername() != null ? authenticationSession.getUsername() : "null"
        );

        objectMapper.writeValue(response.getWriter(), responseBody);
    }
}