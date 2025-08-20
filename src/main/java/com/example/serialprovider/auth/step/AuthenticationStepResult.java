package com.example.serialprovider.auth.step;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * Result of processing an authentication step
 */
public class AuthenticationStepResult {
    
    private final boolean success;
    private final boolean completed;
    private final String message;
    private final String nextStep;
    private final Authentication authentication;
    private final List<GrantedAuthority> grantedAuthorities;
    private final Throwable error;
    
    private AuthenticationStepResult(Builder builder) {
        this.success = builder.success;
        this.completed = builder.completed;
        this.message = builder.message;
        this.nextStep = builder.nextStep;
        this.authentication = builder.authentication;
        this.grantedAuthorities = builder.grantedAuthorities;
        this.error = builder.error;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getNextStep() {
        return nextStep;
    }
    
    public Authentication getAuthentication() {
        return authentication;
    }
    
    public List<GrantedAuthority> getGrantedAuthorities() {
        return grantedAuthorities;
    }
    
    public Throwable getError() {
        return error;
    }
    
    public static Builder success() {
        return new Builder().success(true);
    }
    
    public static Builder failure() {
        return new Builder().success(false);
    }
    
    public static class Builder {
        private boolean success;
        private boolean completed = false;
        private String message;
        private String nextStep;
        private Authentication authentication;
        private List<GrantedAuthority> grantedAuthorities;
        private Throwable error;
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder completed(boolean completed) {
            this.completed = completed;
            return this;
        }
        
        public Builder message(String message) {
            this.message = message;
            return this;
        }
        
        public Builder nextStep(String nextStep) {
            this.nextStep = nextStep;
            return this;
        }
        
        public Builder authentication(Authentication authentication) {
            this.authentication = authentication;
            return this;
        }
        
        public Builder grantedAuthorities(List<GrantedAuthority> authorities) {
            this.grantedAuthorities = authorities;
            return this;
        }
        
        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }
        
        public AuthenticationStepResult build() {
            return new AuthenticationStepResult(this);
        }
    }
}