package com.example.serialprovider.config;

import com.example.serialprovider.service.AuthenticationSettingsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AuthenticationSettingsService settingsService;

    public DataInitializer(AuthenticationSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Initialize default authentication settings
        settingsService.initializeDefaultSettings();
        System.out.println("✅ Authentication settings initialized: " + 
                          settingsService.getCurrentSettings().getFlowDescription());
    }
}