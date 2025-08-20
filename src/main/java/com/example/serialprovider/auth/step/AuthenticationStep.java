package com.example.serialprovider.auth.step;

import com.example.serialprovider.auth.AuthenticationSession;
import org.springframework.security.core.Authentication;

/**
 * Generic interface for authentication steps in a multi-factor authentication flow.
 * Each step is self-contained and knows its own configuration, validation logic, and next step.
 */
public interface AuthenticationStep {
    
    /**
     * Unique identifier for this authentication step
     */
    String getStepId();
    
    /**
     * Human-readable name for this step
     */
    String getStepName();
    
    /**
     * Order/priority of this step in the authentication flow
     */
    int getOrder();
    
    /**
     * Whether this step is currently enabled/required
     */
    boolean isEnabled();
    
    /**
     * Whether this step has been completed in the current session
     */
    boolean isCompleted(AuthenticationSession session);
    
    /**
     * Whether the user can access this step based on current session state
     */
    boolean canAccess(AuthenticationSession session);
    
    /**
     * Process the authentication for this step
     * @param authentication The authentication attempt
     * @param session Current authentication session
     * @return Result of the authentication processing
     */
    AuthenticationStepResult processAuthentication(Authentication authentication, AuthenticationSession session);
    
    /**
     * Get the next step after successful completion of this step
     * @param session Current authentication session
     * @return Next step identifier, or null if this is the final step
     */
    String getNextStep(AuthenticationSession session);
    
    /**
     * Get the URL/endpoint for this authentication step
     */
    String getStepUrl();
    
    /**
     * Get user-friendly message after successful completion
     */
    String getSuccessMessage();
    
    /**
     * Reset/clear any step-specific data from the session
     */
    void resetStep(AuthenticationSession session);
}