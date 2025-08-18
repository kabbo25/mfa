package com.example.serialprovider.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "auth_settings")
public class AuthenticationSettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "otp_enabled", nullable = false)
    private Boolean otpEnabled = true;
    
    @Column(name = "onboarding_enabled", nullable = false)
    private Boolean onboardingEnabled = true;
    
    @Column(name = "setting_name", unique = true)
    private String settingName = "DEFAULT";
    
    @Column(name = "description")
    private String description = "Default authentication settings";
    
    // Constructors
    public AuthenticationSettings() {}
    
    public AuthenticationSettings(Boolean otpEnabled, Boolean onboardingEnabled) {
        this.otpEnabled = otpEnabled;
        this.onboardingEnabled = onboardingEnabled;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Boolean getOtpEnabled() {
        return otpEnabled;
    }
    
    public void setOtpEnabled(Boolean otpEnabled) {
        this.otpEnabled = otpEnabled;
    }
    
    public Boolean getOnboardingEnabled() {
        return onboardingEnabled;
    }
    
    public void setOnboardingEnabled(Boolean onboardingEnabled) {
        this.onboardingEnabled = onboardingEnabled;
    }
    
    public String getSettingName() {
        return settingName;
    }
    
    public void setSettingName(String settingName) {
        this.settingName = settingName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    // Helper methods
    public boolean isOtpEnabled() {
        return otpEnabled != null && otpEnabled;
    }
    
    public boolean isOnboardingEnabled() {
        return onboardingEnabled != null && onboardingEnabled;
    }
    
    // Get authentication flow description
    public String getFlowDescription() {
        if (isOtpEnabled() && isOnboardingEnabled()) {
            return "Password + OTP + Onboarding (3 steps)";
        } else if (isOtpEnabled()) {
            return "Password + OTP (2 steps)";
        } else if (isOnboardingEnabled()) {
            return "Password + Onboarding (2 steps)";
        } else {
            return "Password only (1 step)";
        }
    }
    
    @Override
    public String toString() {
        return "AuthenticationSettings{" +
                "id=" + id +
                ", otpEnabled=" + otpEnabled +
                ", onboardingEnabled=" + onboardingEnabled +
                ", settingName='" + settingName + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}