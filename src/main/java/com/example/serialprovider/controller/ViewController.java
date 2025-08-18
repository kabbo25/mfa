package com.example.serialprovider.controller;

import com.example.serialprovider.auth.AuthenticationSession;
import com.example.serialprovider.auth.AuthenticationState;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {
    
    private final AuthenticationSession authenticationSession;
    
    public ViewController(AuthenticationSession authenticationSession) {
        this.authenticationSession = authenticationSession;
    }
    
    @GetMapping("/")
    public String index() {
        // Check current authentication state and redirect accordingly
        AuthenticationState currentState = authenticationSession.getState();
        
        return switch (currentState) {
            case FULLY_AUTHENTICATED -> "redirect:/dashboard.html";
            case OTP_VERIFIED -> "redirect:/onboarding.html";
            case USERNAME_PASSWORD_VERIFIED -> "redirect:/otp.html";
            case UNAUTHENTICATED -> "index.html";
        };
    }
    
    @GetMapping("/login")
    public String login() {
        AuthenticationState currentState = authenticationSession.getState();
        
        // If already authenticated, redirect to appropriate page
        return switch (currentState) {
            case FULLY_AUTHENTICATED -> "redirect:/dashboard.html";
            case OTP_VERIFIED -> "redirect:/onboarding.html";
            case USERNAME_PASSWORD_VERIFIED -> "redirect:/otp.html";
            case UNAUTHENTICATED -> "login.html";
        };
    }
    
    @GetMapping("/otp")
    public String otp() {
        AuthenticationState currentState = authenticationSession.getState();
        
        return switch (currentState) {
            case FULLY_AUTHENTICATED -> "redirect:/dashboard.html";
            case OTP_VERIFIED -> "redirect:/onboarding.html";
            case USERNAME_PASSWORD_VERIFIED -> "otp.html";
            case UNAUTHENTICATED -> "redirect:/login.html";
        };
    }
    
    @GetMapping("/onboarding")
    public String onboarding() {
        AuthenticationState currentState = authenticationSession.getState();
        
        return switch (currentState) {
            case FULLY_AUTHENTICATED -> "redirect:/dashboard.html";
            case OTP_VERIFIED -> "onboarding.html";
            case USERNAME_PASSWORD_VERIFIED -> "redirect:/otp.html";
            case UNAUTHENTICATED -> "redirect:/login.html";
        };
    }
    
    @GetMapping("/dashboard")
    public String dashboard() {
        AuthenticationState currentState = authenticationSession.getState();
        
        return switch (currentState) {
            case FULLY_AUTHENTICATED -> "dashboard.html";
            case OTP_VERIFIED -> "redirect:/onboarding.html";
            case USERNAME_PASSWORD_VERIFIED -> "redirect:/otp.html";
            case UNAUTHENTICATED -> "redirect:/login.html";
        };
    }
}