package com.example.serialprovider.service;

import org.springframework.stereotype.Service;

@Service
public class OnboardingService {
    
    public boolean processOnboarding(String username, OnboardingData data) {
        // Simple validation for demonstration
        if (data.getFullName() == null || data.getFullName().trim().isEmpty()) {
            return false;
        }
        if (data.getEmail() == null || !data.getEmail().contains("@")) {
            return false;
        }
        
        // In production, this would save user profile information to database
        System.out.println("Onboarding completed for user " + username + 
                          " with name: " + data.getFullName() + 
                          " and email: " + data.getEmail());
        return true;
    }
    
    public static class OnboardingData {
        private String fullName;
        private String email;
        private String phoneNumber;
        
        public String getFullName() {
            return fullName;
        }
        
        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getPhoneNumber() {
            return phoneNumber;
        }
        
        public void setPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
        }
    }
}