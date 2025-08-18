package com.example.serialprovider.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpService {
    private final SecureRandom random = new SecureRandom();
    
    public String generateOtp() {
        int otp = 100000 + random.nextInt(900000); // 6-digit OTP
        return String.valueOf(otp);
    }
    
    public boolean validateOtp(String sessionOtp, String providedOtp) {
        return sessionOtp != null && sessionOtp.equals(providedOtp);
    }
    
    public void sendOtp(String username, String otpCode) {
        // In a real implementation, this would send OTP via SMS/Email
        System.out.println("OTP for user " + username + ": " + otpCode);
        // For demo purposes, we'll just log it
    }
}