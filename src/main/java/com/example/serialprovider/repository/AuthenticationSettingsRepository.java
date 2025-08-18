package com.example.serialprovider.repository;

import com.example.serialprovider.entity.AuthenticationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthenticationSettingsRepository extends JpaRepository<AuthenticationSettings, Long> {
    
    Optional<AuthenticationSettings> findBySettingName(String settingName);
    
    @Query("SELECT a FROM AuthenticationSettings a WHERE a.settingName = 'DEFAULT'")
    Optional<AuthenticationSettings> findDefaultSettings();
    
    default AuthenticationSettings findDefaultOrCreate() {
        return findDefaultSettings().orElse(new AuthenticationSettings(true, true));
    }
}