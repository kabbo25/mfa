package com.example.serialprovider.service;

import org.springframework.stereotype.Service;

@Service
public class UserCredentialsService {
    
    public boolean validateCredentials(String username, String password) {
        // Simple hardcoded validation for demonstration
        // In production, this would check against a database with hashed passwords
        return "admin".equals(username) && "password123".equals(password) ||
               "user".equals(username) && "userpass".equals(password);
    }
    
    public boolean userExists(String username) {
        return "admin".equals(username) || "user".equals(username);
    }
}