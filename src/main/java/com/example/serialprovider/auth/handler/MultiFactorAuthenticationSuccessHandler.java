package com.example.serialprovider.auth.handler;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.step.AuthenticationStep;
import com.example.serialprovider.auth.step.AuthenticationStepChain;
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
import java.util.Optional;

@Component
public class MultiFactorAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthenticationSession authenticationSession;
    private final AuthenticationStepChain stepChain;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuthenticationSuccessHandler dashboardSuccessHandler;

    public MultiFactorAuthenticationSuccessHandler(AuthenticationSession authenticationSession,
                                                 AuthenticationStepChain stepChain) {
        this.authenticationSession = authenticationSession;
        this.stepChain = stepChain;
        this.dashboardSuccessHandler = new SimpleUrlAuthenticationSuccessHandler("/dashboard");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException, ServletException {
        
        // Use the step chain to determine what happens next
        Optional<AuthenticationStep> nextStepOpt = stepChain.getNextStep(authenticationSession);
        
        if (nextStepOpt.isPresent()) {
            // There are more authentication steps to complete
            AuthenticationStep nextStep = nextStepOpt.get();
            
            // Store intermediate authentication to prevent access to protected resources
            SecurityContextHolder.getContext().setAuthentication(
                createIntermediateAuthentication(authentication, "PARTIAL_AUTH")
            );
            
            // Send JSON response with next step information
            sendJsonResponse(response, 
                nextStep.getSuccessMessage(), 
                nextStep.getStepId(),
                false // Not fully authenticated yet
            );
        } else {
            // All authentication steps completed
            if (stepChain.isFullyAuthenticated(authenticationSession)) {
                sendJsonResponse(response, 
                    "Authentication completed successfully!", 
                    "dashboard",
                    true // Fully authenticated
                );
            } else {
                // Something went wrong - redirect to login
                sendJsonResponse(response, 
                    "Please start authentication.", 
                    "login",
                    false
                );
            }
        }
    }
    
    /**
     * Create an intermediate authentication token that indicates partial completion.
     * This prevents access to protected resources until full authentication is complete.
     */
    private Authentication createIntermediateAuthentication(Authentication originalAuth, String step) {
        return new org.springframework.security.authentication.AnonymousAuthenticationToken(
            step, originalAuth.getPrincipal(), originalAuth.getAuthorities()
        );
    }
    
    /**
     * Send a standardized JSON response with authentication progress information
     */
    private void sendJsonResponse(HttpServletResponse response, String message, String nextStep, boolean fullyAuthenticated) throws IOException {
        response.setContentType("application/json");
        
        // Get authentication progress
        AuthenticationStepChain.AuthenticationProgress progress = stepChain.getProgress(authenticationSession);
        
        Map<String, Object> responseBody = Map.of(
            "message", message,
            "nextStep", nextStep,
            "fullyAuthenticated", fullyAuthenticated,
            "progress", Map.of(
                "completedSteps", progress.getCompletedSteps(),
                "totalSteps", progress.getTotalSteps(),
                "percentage", Math.round(progress.getProgressPercentage())
            ),
            "user", authenticationSession.getUsername() != null ? authenticationSession.getUsername() : "null",
            "flowDescription", stepChain.getFlowDescription()
        );
        
        objectMapper.writeValue(response.getWriter(), responseBody);
    }
}