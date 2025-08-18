package com.example.serialprovider.auth;

public enum AuthenticationState {
    UNAUTHENTICATED,
    USERNAME_PASSWORD_VERIFIED,
    OTP_VERIFIED,
    FULLY_AUTHENTICATED
}