package com.example.serialprovider.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProtectedController {
    
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
            "message", "Welcome to your dashboard!",
            "user", authentication.getName(),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
            "message", "User profile data",
            "user", authentication.getName(),
            "authorities", authentication.getAuthorities()
        ));
    }
}