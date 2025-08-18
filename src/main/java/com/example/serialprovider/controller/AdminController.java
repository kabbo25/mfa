package com.example.serialprovider.controller;

import com.example.serialprovider.entity.AuthenticationSettings;
import com.example.serialprovider.service.AuthenticationSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/auth-settings")
public class AdminController {

    private final AuthenticationSettingsService settingsService;

    public AdminController(AuthenticationSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public ResponseEntity<AuthenticationSettings> getCurrentSettings() {
        return ResponseEntity.ok(settingsService.getCurrentSettings());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSettingsStatus() {
        AuthenticationSettings settings = settingsService.getCurrentSettings();
        return ResponseEntity.ok(Map.of(
            "otpEnabled", settings.isOtpEnabled(),
            "onboardingEnabled", settings.isOnboardingEnabled(),
            "flowDescription", settings.getFlowDescription(),
            "settingName", settings.getSettingName(),
            "description", settings.getDescription()
        ));
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateSettings(@RequestBody SettingsUpdateRequest request) {
        try {
            AuthenticationSettings updated = settingsService.updateSettings(
                request.isOtpEnabled(), 
                request.isOnboardingEnabled()
            );
            
            return ResponseEntity.ok(Map.of(
                "message", "Settings updated successfully",
                "settings", updated,
                "flowDescription", updated.getFlowDescription()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to update settings: " + e.getMessage()));
        }
    }

    @PostMapping("/enable-otp")
    public ResponseEntity<Map<String, Object>> enableOtp() {
        AuthenticationSettings current = settingsService.getCurrentSettings();
        AuthenticationSettings updated = settingsService.updateSettings(true, current.isOnboardingEnabled());
        
        return ResponseEntity.ok(Map.of(
            "message", "OTP authentication enabled",
            "flowDescription", updated.getFlowDescription()
        ));
    }

    @PostMapping("/disable-otp")
    public ResponseEntity<Map<String, Object>> disableOtp() {
        AuthenticationSettings current = settingsService.getCurrentSettings();
        AuthenticationSettings updated = settingsService.updateSettings(false, current.isOnboardingEnabled());
        
        return ResponseEntity.ok(Map.of(
            "message", "OTP authentication disabled",
            "flowDescription", updated.getFlowDescription()
        ));
    }

    @PostMapping("/enable-onboarding")
    public ResponseEntity<Map<String, Object>> enableOnboarding() {
        AuthenticationSettings current = settingsService.getCurrentSettings();
        AuthenticationSettings updated = settingsService.updateSettings(current.isOtpEnabled(), true);
        
        return ResponseEntity.ok(Map.of(
            "message", "Onboarding authentication enabled",
            "flowDescription", updated.getFlowDescription()
        ));
    }

    @PostMapping("/disable-onboarding")
    public ResponseEntity<Map<String, Object>> disableOnboarding() {
        AuthenticationSettings current = settingsService.getCurrentSettings();
        AuthenticationSettings updated = settingsService.updateSettings(current.isOtpEnabled(), false);
        
        return ResponseEntity.ok(Map.of(
            "message", "Onboarding authentication disabled",
            "flowDescription", updated.getFlowDescription()
        ));
    }

    @GetMapping("/flow-combinations")
    public ResponseEntity<Map<String, Object>> getFlowCombinations() {
        return ResponseEntity.ok(Map.of(
            "combinations", Map.of(
                "password_only", Map.of(
                    "otp", false,
                    "onboarding", false,
                    "description", "Password only (1 step)",
                    "flow", "Password → Dashboard"
                ),
                "password_otp", Map.of(
                    "otp", true,
                    "onboarding", false,
                    "description", "Password + OTP (2 steps)",
                    "flow", "Password → OTP → Dashboard"
                ),
                "password_onboarding", Map.of(
                    "otp", false,
                    "onboarding", true,
                    "description", "Password + Onboarding (2 steps)",
                    "flow", "Password → Onboarding → Dashboard"
                ),
                "full_3fa", Map.of(
                    "otp", true,
                    "onboarding", true,
                    "description", "Password + OTP + Onboarding (3 steps)",
                    "flow", "Password → OTP → Onboarding → Dashboard"
                )
            )
        ));
    }

    // DTO for settings update
    public static class SettingsUpdateRequest {
        private boolean otpEnabled;
        private boolean onboardingEnabled;

        public boolean isOtpEnabled() {
            return otpEnabled;
        }

        public void setOtpEnabled(boolean otpEnabled) {
            this.otpEnabled = otpEnabled;
        }

        public boolean isOnboardingEnabled() {
            return onboardingEnabled;
        }

        public void setOnboardingEnabled(boolean onboardingEnabled) {
            this.onboardingEnabled = onboardingEnabled;
        }
    }
}