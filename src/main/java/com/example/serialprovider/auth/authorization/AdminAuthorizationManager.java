package com.example.serialprovider.auth.authorization;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.AuthenticationState;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class AdminAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final AuthenticationSession authenticationSession;

    public AdminAuthorizationManager(AuthenticationSession authenticationSession) {
        this.authenticationSession = authenticationSession;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, 
                                     RequestAuthorizationContext requestContext) {
        
        String requestPath = requestContext.getRequest().getRequestURI();
        
        // For demo purposes, allow all admin endpoints without authentication
        // This matches the original .permitAll() behavior for admin endpoints
        // In production, you would add proper authentication and authorization checks
        
        // Allow all admin endpoints for demonstration
        if (requestPath.startsWith("/admin/")) {
            return new AuthorizationDecision(true);
        }
        
        // Additional custom logic for production could include:
        Authentication auth = authentication.get();
        if (auth != null && auth.isAuthenticated()) {
            // Check if user has completed full authentication
            if (authenticationSession.getState() == AuthenticationState.FULLY_AUTHENTICATED) {
                return new AuthorizationDecision(true);
            }
            
            // Check for admin roles, IP restrictions, time-based access, etc.
            // Example: auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))
        }
        
        return new AuthorizationDecision(false);
    }
}