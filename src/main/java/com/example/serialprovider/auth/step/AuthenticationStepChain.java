package com.example.serialprovider.auth.step;

import com.example.serialprovider.auth.AuthenticationSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Manages the chain of authentication steps and coordinates their execution.
 * Uses Chain of Responsibility pattern to handle dynamic authentication flows.
 */
@Component
public class AuthenticationStepChain {
    
    private final List<AuthenticationStep> allSteps;
    
    public AuthenticationStepChain(List<AuthenticationStep> authenticationSteps) {
        // Sort steps by order to ensure proper sequence
        this.allSteps = authenticationSteps.stream()
            .sorted(Comparator.comparingInt(AuthenticationStep::getOrder))
            .toList();
    }
    
    /**
     * Get all currently enabled authentication steps in order
     */
    public List<AuthenticationStep> getEnabledSteps() {
        return allSteps.stream()
            .filter(AuthenticationStep::isEnabled)
            .toList();
    }
    
    /**
     * Get an authentication step by its ID
     */
    public Optional<AuthenticationStep> getStepById(String stepId) {
        return allSteps.stream()
            .filter(step -> step.getStepId().equals(stepId))
            .findFirst();
    }
    
    /**
     * Get the first enabled step (usually password)
     */
    public Optional<AuthenticationStep> getFirstStep() {
        return getEnabledSteps().stream().findFirst();
    }
    
    /**
     * Get the next step that should be executed based on current session state
     */
    public Optional<AuthenticationStep> getNextStep(AuthenticationSession session) {
        List<AuthenticationStep> enabledSteps = getEnabledSteps();
        
        for (AuthenticationStep step : enabledSteps) {
            // If this step is not completed and can be accessed, it's the next step
            if (!step.isCompleted(session) && step.canAccess(session)) {
                return Optional.of(step);
            }
        }
        
        return Optional.empty(); // All steps completed
    }
    
    /**
     * Check if all enabled authentication steps are completed
     */
    public boolean isFullyAuthenticated(AuthenticationSession session) {
        List<AuthenticationStep> enabledSteps = getEnabledSteps();
        
        return enabledSteps.stream()
            .allMatch(step -> step.isCompleted(session));
    }
    
    /**
     * Process authentication for a specific step
     */
    public AuthenticationStepResult processStep(String stepId, Authentication authentication, AuthenticationSession session) {
        Optional<AuthenticationStep> stepOpt = getStepById(stepId);
        
        if (stepOpt.isEmpty()) {
            return AuthenticationStepResult.failure()
                .message("Unknown authentication step: " + stepId)
                .error(new RuntimeException("Step not found"))
                .build();
        }
        
        AuthenticationStep step = stepOpt.get();
        
        if (!step.isEnabled()) {
            return AuthenticationStepResult.failure()
                .message("Authentication step is disabled: " + stepId)
                .error(new RuntimeException("Step disabled"))
                .build();
        }
        
        if (!step.canAccess(session)) {
            return AuthenticationStepResult.failure()
                .message("Cannot access step " + stepId + " in current state")
                .error(new RuntimeException("Invalid step access"))
                .build();
        }
        
        return step.processAuthentication(authentication, session);
    }
    
    /**
     * Get the step that should handle a given URL/endpoint
     */
    public Optional<AuthenticationStep> getStepByUrl(String url) {
        return allSteps.stream()
            .filter(step -> step.getStepUrl().equals(url))
            .findFirst();
    }
    
    /**
     * Reset all steps and clear session
     */
    public void resetAllSteps(AuthenticationSession session) {
        allSteps.forEach(step -> step.resetStep(session));
        session.reset();
    }
    
    /**
     * Get authentication flow description for debugging/monitoring
     */
    public String getFlowDescription() {
        List<AuthenticationStep> enabledSteps = getEnabledSteps();
        
        if (enabledSteps.isEmpty()) {
            return "No authentication steps enabled";
        }
        
        String stepNames = enabledSteps.stream()
            .map(AuthenticationStep::getStepName)
            .reduce((a, b) -> a + " → " + b)
            .orElse("");
            
        return String.format("%s (%d steps)", stepNames, enabledSteps.size());
    }
    
    /**
     * Get current authentication progress for a session
     */
    public AuthenticationProgress getProgress(AuthenticationSession session) {
        List<AuthenticationStep> enabledSteps = getEnabledSteps();
        int totalSteps = enabledSteps.size();
        int completedSteps = (int) enabledSteps.stream()
            .filter(step -> step.isCompleted(session))
            .count();
            
        return new AuthenticationProgress(
            completedSteps,
            totalSteps,
            isFullyAuthenticated(session),
            getNextStep(session).map(AuthenticationStep::getStepId).orElse(null)
        );
    }
    
    /**
     * Represents authentication progress
     */
    public static class AuthenticationProgress {
        private final int completedSteps;
        private final int totalSteps;
        private final boolean fullyAuthenticated;
        private final String nextStepId;
        
        public AuthenticationProgress(int completedSteps, int totalSteps, boolean fullyAuthenticated, String nextStepId) {
            this.completedSteps = completedSteps;
            this.totalSteps = totalSteps;
            this.fullyAuthenticated = fullyAuthenticated;
            this.nextStepId = nextStepId;
        }
        
        public int getCompletedSteps() { return completedSteps; }
        public int getTotalSteps() { return totalSteps; }
        public boolean isFullyAuthenticated() { return fullyAuthenticated; }
        public String getNextStepId() { return nextStepId; }
        
        public double getProgressPercentage() {
            return totalSteps == 0 ? 0.0 : (double) completedSteps / totalSteps * 100;
        }
    }
}