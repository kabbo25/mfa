package com.example.serialprovider.auth.handler;

import com.example.serialprovider.auth.AuthenticationSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class MultiFactorAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final AuthenticationSession authenticationSession;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MultiFactorAuthenticationFailureHandler(AuthenticationSession authenticationSession) {
        this.authenticationSession = authenticationSession;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                      AuthenticationException exception) throws IOException {
        
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        String errorMessage = exception.getMessage();
        
        // For certain errors, reset the session
        if (exception instanceof BadCredentialsException) {
            String message = exception.getMessage();
            if (message.contains("Invalid credentials") || 
                message.contains("Invalid OTP") ||
                message.contains("Invalid onboarding data")) {
                authenticationSession.reset();
            }
        }

        Map<String, Object> responseBody = Map.of(
            "error", errorMessage,
            "currentState", authenticationSession.getState()
        );

        objectMapper.writeValue(response.getWriter(), responseBody);
    }
}