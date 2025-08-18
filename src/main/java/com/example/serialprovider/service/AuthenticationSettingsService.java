package com.example.serialprovider.service;

import com.example.serialprovider.entity.AuthenticationSettings;
import com.example.serialprovider.repository.AuthenticationSettingsRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticationSettingsService {
    
    private final AuthenticationSettingsRepository repository;
    
    public AuthenticationSettingsService(AuthenticationSettingsRepository repository) {
        this.repository = repository;
    }
    
    @Cacheable("authSettings")
    public AuthenticationSettings getCurrentSettings() {
        return repository.findDefaultOrCreate();
    }
    
    @Transactional
    @CacheEvict(value = "authSettings", allEntries = true)
    public AuthenticationSettings updateSettings(boolean otpEnabled, boolean onboardingEnabled) {
        AuthenticationSettings settings = repository.findDefaultSettings()
                .orElse(new AuthenticationSettings());
        
        settings.setSettingName("DEFAULT");
        settings.setOtpEnabled(otpEnabled);
        settings.setOnboardingEnabled(onboardingEnabled);
        settings.setDescription("Default authentication settings - Updated");
        
        return repository.save(settings);
    }
    
    @Transactional
    @CacheEvict(value = "authSettings", allEntries = true)
    public void initializeDefaultSettings() {
        if (repository.findDefaultSettings().isEmpty()) {
            AuthenticationSettings defaultSettings = new AuthenticationSettings(true, true);
            defaultSettings.setSettingName("DEFAULT");
            defaultSettings.setDescription("Default authentication settings - 3FA enabled");
            repository.save(defaultSettings);
        }
    }
    
    // Convenient methods for checking individual settings
    public boolean isOtpEnabled() {
        return getCurrentSettings().isOtpEnabled();
    }
    
    public boolean isOnboardingEnabled() {
        return getCurrentSettings().isOnboardingEnabled();
    }
    
    // Get next step in authentication flow
    public String getNextStep(String currentStep) {
        AuthenticationSettings settings = getCurrentSettings();
        
        return switch (currentStep) {
            case "password" -> {
                if (settings.isOtpEnabled()) {
                    yield "otp";
                } else if (settings.isOnboardingEnabled()) {
                    yield "onboarding";
                } else {
                    yield "dashboard";
                }
            }
            case "otp" -> {
                if (settings.isOnboardingEnabled()) {
                    yield "onboarding";
                } else {
                    yield "dashboard";
                }
            }
            case "onboarding" -> "dashboard";
            default -> "login";
        };
    }
    
    // Check if a step is required based on current settings
    public boolean isStepRequired(String step) {
        AuthenticationSettings settings = getCurrentSettings();
        
        return switch (step) {
            case "password" -> true; // Password is always required
            case "otp" -> settings.isOtpEnabled();
            case "onboarding" -> settings.isOnboardingEnabled();
            default -> false;
        };
    }
}