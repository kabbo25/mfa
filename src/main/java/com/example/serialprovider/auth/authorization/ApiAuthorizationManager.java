package com.example.serialprovider.auth.authorization;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.AuthenticationState;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ApiAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    private final AuthenticationSession authenticationSession;

    public ApiAuthorizationManager(AuthenticationSession authenticationSession) {
        this.authenticationSession = authenticationSession;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, 
                                     RequestAuthorizationContext requestContext) {
        
        Authentication auth = authentication.get();
        
        if (auth == null || !auth.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        
        // Enhanced API access control beyond simple authority check
        
        // 1. Check if user has completed full authentication flow
        if (authenticationSession.getState() != AuthenticationState.FULLY_AUTHENTICATED) {
            return new AuthorizationDecision(false);
        }
        
        // 2. Verify session is not expired
        if (authenticationSession.isExpired()) {
            return new AuthorizationDecision(false);
        }
        
        // 3. Check for FULLY_AUTHENTICATED authority (backward compatibility)
        boolean hasRequiredAuthority = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("FULLY_AUTHENTICATED"::equals);
            
        if (!hasRequiredAuthority) {
            return new AuthorizationDecision(false);
        }
        
        // 4. Additional API-specific security checks
        String requestPath = requestContext.getRequest().getRequestURI();
        String httpMethod = requestContext.getRequest().getMethod();
        
        // Example: Restrict certain sensitive API operations
        if (requestPath.contains("/api/admin") && !"GET".equals(httpMethod)) {
            // Could add additional admin validation here
            // For now, allow if fully authenticated
        }
        
        // Future enhancements could include:
        // - Role-based access control for different API endpoints
        // - Rate limiting per user/IP
        // - API key validation for external integrations
        // - Scope-based permissions (read vs write)
        // - Time-based access restrictions
        // - Geographical restrictions
        
        return new AuthorizationDecision(true);
    }
}