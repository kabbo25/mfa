package com.example.serialprovider.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class MultiFactorAuthenticationFilterOld extends OncePerRequestFilter {
    
    private final AuthenticationSession authenticationSession;
    
    public MultiFactorAuthenticationFilterOld(AuthenticationSession authenticationSession) {
        this.authenticationSession = authenticationSession;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Allow auth endpoints, h2-console, view controllers, and static resources
        if (requestPath.startsWith("/auth/") || 
            requestPath.startsWith("/h2-console/") ||
            requestPath.equals("/") ||
            requestPath.equals("/login") ||
            requestPath.equals("/otp") ||
            requestPath.equals("/onboarding") ||
            requestPath.equals("/dashboard") ||
            requestPath.endsWith(".html") ||
            requestPath.startsWith("/js/") ||
            requestPath.startsWith("/css/") ||
            requestPath.startsWith("/favicon.ico")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Check session expiry
        if (authenticationSession.isExpired()) {
            authenticationSession.reset();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Session expired\"}");
            return;
        }
        
        // Check if user is fully authenticated
        if (authenticationSession.getState() != AuthenticationState.FULLY_AUTHENTICATED) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Authentication incomplete\"}");
            return;
        }
        
        // Set authentication in security context
        PreAuthenticatedAuthenticationToken authentication = 
            new PreAuthenticatedAuthenticationToken(
                authenticationSession.getUsername(), 
                null, 
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        filterChain.doFilter(request, response);
    }
}