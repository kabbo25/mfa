# Dynamic Multi-Factor Authentication Architecture

## Overview

This system implements a **configurable multi-step authentication flow** using **Spring Security's Authentication Provider Pattern**. The number of authentication steps can be dynamically configured through a settings table, supporting 1-3 authentication factors:

- **Password Only** (1 step)
- **Password + OTP** (2 steps) 
- **Password + Onboarding** (2 steps)
- **Password + OTP + Onboarding** (3 steps)

The system uses Spring Security's built-in authentication mechanism with multiple providers that adapt based on configuration.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Client Request                                │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────────┐
│                      AuthController                                     │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐                │
│  │/auth/login  │ │/auth/otp    │ │/auth/onboard        │                │
│  │             │ │             │ │                     │                │
│  │Creates:     │ │Creates:     │ │Creates:             │                │
│  │Username     │ │OtpAuth      │ │OnboardingAuth       │                │
│  │PasswordToken│ │Token        │ │Token                │                │
│  └─────────────┘ └─────────────┘ └─────────────────────┘                │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────────┐
│                MultiFactorAuthenticationManager                         │
│                                                                         │
│  Routes authentication tokens to appropriate providers                  │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────────┐
│                    Authentication Providers Chain                       │
│                                                                         │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────────────────┐│
│  │    Password     │ │      OTP        │ │      Onboarding             ││
│  │   Provider      │ │   Provider      │ │      Provider               ││
│  │                 │ │                 │ │                             ││
│  │• Validates      │ │• Validates OTP  │ │• Validates profile data     ││
│  │  credentials    │ │• Checks step 1  │ │• Checks step 2 completed    ││
│  │• Generates OTP  │ │  completed      │ │• Marks fully authenticated  ││
│  │• Updates state  │ │• Updates state  │ │• Updates state              ││
│  │  to STEP_1      │ │  to STEP_2      │ │  to FULLY_AUTHENTICATED     ││
│  └─────────────────┘ └─────────────────┘ └─────────────────────────────┘│
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────────┐
│                    Success/Failure Handlers                             │
│                                                                         │
│  ┌─────────────────────────┐ ┌─────────────────────────────────────────┐ │
│  │   Success Handler       │ │      Failure Handler                    │ │
│  │                         │ │                                         │ │
│  │• Returns next step      │ │• Handles authentication errors         │ │
│  │• Sends appropriate      │ │• Resets session on critical failures   │ │
│  │  response               │ │• Returns error messages                 │ │
│  └─────────────────────────┘ └─────────────────────────────────────────┘ │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────────────┐
│                      AuthenticationSession                              │
│                                                                         │
│  • Tracks current authentication state                                  │
│  • Stores temporary data (username, OTP, etc.)                          │
│  • Manages session timeout and expiry                                   │
│  • Prevents step jumping with state validation                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Key Components Explained

### 1. Authentication Tokens

**Custom tokens represent each step of authentication:**

```java
// Step 1: Username/Password
UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);

// Step 2: OTP Verification  
OtpAuthenticationToken token = new OtpAuthenticationToken(username, otpCode);

// Step 3: Onboarding
OnboardingAuthenticationToken token = new OnboardingAuthenticationToken(username, profileData);
```

**Why tokens?** Each authentication step needs different data. Custom tokens allow Spring Security to route requests to the correct provider.

### 2. Authentication Providers

**Each provider handles one authentication step:**

#### PasswordAuthenticationProvider
```java
@Component
public class PasswordAuthenticationProvider implements AuthenticationProvider {
    
    public Authentication authenticate(Authentication auth) {
        // 1. Validate username/password
        // 2. Generate OTP and send it
        // 3. Update session state to STEP_1_COMPLETED
        // 4. Return partially authenticated token
    }
    
    public boolean supports(Class<?> auth) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(auth);
    }
}
```

#### OtpAuthenticationProvider
```java
@Component  
public class OtpAuthenticationProvider implements AuthenticationProvider {
    
    public Authentication authenticate(Authentication auth) {
        // 1. Check if Step 1 is completed (security enforcement)
        // 2. Validate OTP code
        // 3. Update session state to STEP_2_COMPLETED
        // 4. Return partially authenticated token
    }
    
    public boolean supports(Class<?> auth) {
        return OtpAuthenticationToken.class.isAssignableFrom(auth);
    }
}
```

#### OnboardingAuthenticationProvider
```java
@Component
public class OnboardingAuthenticationProvider implements AuthenticationProvider {
    
    public Authentication authenticate(Authentication auth) {
        // 1. Check if Step 2 is completed (security enforcement)
        // 2. Validate profile data
        // 3. Update session state to FULLY_AUTHENTICATED
        // 4. Return fully authenticated token with ROLE_USER
    }
    
    public boolean supports(Class<?> auth) {
        return OnboardingAuthenticationToken.class.isAssignableFrom(auth);
    }
}
```

### 3. Authentication Manager

**Routes tokens to correct providers:**

```java
@Component
public class MultiFactorAuthenticationManager implements AuthenticationManager {
    
    private final List<AuthenticationProvider> providers;
    
    public Authentication authenticate(Authentication auth) {
        // Find provider that supports this token type
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(auth.getClass())) {
                return provider.authenticate(auth);
            }
        }
        throw new ProviderNotFoundException("No provider found for " + auth.getClass());
    }
}
```

### 4. Session State Management

**AuthenticationSession tracks progress:**

```java
public enum AuthenticationState {
    UNAUTHENTICATED,
    USERNAME_PASSWORD_VERIFIED,  // Step 1 complete
    OTP_VERIFIED,               // Step 2 complete  
    FULLY_AUTHENTICATED         // Step 3 complete - full access
}

@Component
@SessionScope
public class AuthenticationSession {
    private String username;
    private AuthenticationState state = UNAUTHENTICATED;
    private String otpCode;
    private LocalDateTime lastActivity;
    
    // Prevents step jumping
    public boolean canProgressTo(AuthenticationState targetState) {
        return switch (targetState) {
            case USERNAME_PASSWORD_VERIFIED -> state == UNAUTHENTICATED;
            case OTP_VERIFIED -> state == USERNAME_PASSWORD_VERIFIED;
            case FULLY_AUTHENTICATED -> state == OTP_VERIFIED;
            default -> true;
        };
    }
}
```

## Dynamic Authentication Flows

### Configuration-Based Flow Selection

The system supports 4 different authentication flows based on settings:

#### Flow 1: Password Only (1 step)
```
Settings: OTP=OFF, Onboarding=OFF
Flow: Password → Dashboard
```

#### Flow 2: Password + OTP (2 steps)  
```
Settings: OTP=ON, Onboarding=OFF
Flow: Password → OTP → Dashboard
```

#### Flow 3: Password + Onboarding (2 steps)
```
Settings: OTP=OFF, Onboarding=ON  
Flow: Password → Onboarding → Dashboard
```

#### Flow 4: Full 3-Factor (3 steps)
```
Settings: OTP=ON, Onboarding=ON
Flow: Password → OTP → Onboarding → Dashboard
```

## Authentication Flow Step-by-Step

### Step 1: Username/Password Login

1. **Client** sends POST to `/auth/login` with username/password
2. **AuthController** creates `UsernamePasswordAuthenticationToken`
3. **AuthenticationManager** routes to `PasswordAuthenticationProvider`
4. **Provider** validates credentials, generates OTP, updates session to `USERNAME_PASSWORD_VERIFIED`
5. **Success Handler** returns JSON: `{"nextStep": "otp", "message": "OTP sent"}`

### Step 2: OTP Verification

1. **Client** sends POST to `/auth/otp` with OTP code
2. **AuthController** creates `OtpAuthenticationToken`
3. **AuthenticationManager** routes to `OtpAuthenticationProvider` 
4. **Provider** checks Step 1 completed, validates OTP, updates session to `OTP_VERIFIED`
5. **Success Handler** returns JSON: `{"nextStep": "onboarding", "message": "OTP verified"}`

### Step 3: Profile Onboarding

1. **Client** sends POST to `/auth/onboard` with profile data
2. **AuthController** creates `OnboardingAuthenticationToken`
3. **AuthenticationManager** routes to `OnboardingAuthenticationProvider`
4. **Provider** checks Step 2 completed, validates profile, updates session to `FULLY_AUTHENTICATED`
5. **Success Handler** returns JSON: `{"nextStep": "dashboard", "message": "Authentication complete"}`

## Security Features

### 1. Step Jumping Prevention
Each provider validates that previous steps are completed:
```java
if (authenticationSession.getState() != AuthenticationState.OTP_VERIFIED) {
    throw new BadCredentialsException("Must complete OTP step first");
}
```

### 2. Session Timeout
Session automatically expires after 30 minutes:
```java
public boolean isExpired() {
    return lastActivity.isBefore(LocalDateTime.now().minusMinutes(30));
}
```

### 3. Failure Handling
Critical failures reset the entire session:
```java
if (message.contains("Invalid credentials") || message.contains("Invalid OTP")) {
    authenticationSession.reset(); // Start over from Step 1
}
```

### 4. Protected Resources
API endpoints require full authentication:
```java
.requestMatchers("/api/**").hasAuthority("FULLY_AUTHENTICATED")
```

## Benefits of This Architecture

### ✅ **Spring Security Native**
- Uses framework's built-in authentication flow
- Leverages existing security infrastructure
- Easier integration with other Spring Security features

### ✅ **Clean Separation of Concerns**
- Each provider handles exactly one authentication step
- Clear responsibility boundaries
- Easy to modify individual steps

### ✅ **Testable**
- Each provider can be unit tested independently
- Mock dependencies easily
- Clear input/output contracts

### ✅ **Extensible**
- Add new authentication steps by creating new providers
- Modify existing steps without affecting others
- Support multiple authentication methods

### ✅ **Secure**
- Step jumping prevention built into each provider
- Session state validation enforced
- Automatic session cleanup on failures

## Comparison: Old vs New Architecture

| Aspect | Old (Filter-based) | New (Provider-based) |
|--------|-------------------|---------------------|
| **Approach** | Custom filter intercepts requests | Spring Security authentication flow |
| **Authentication Logic** | Mixed in controllers | Separated into providers |
| **Step Enforcement** | Manual checks in filter | Built into each provider |
| **Testing** | Complex integration tests | Simple unit tests per provider |
| **Framework Alignment** | Fighting against Spring Security | Using Spring Security patterns |
| **Extensibility** | Modify filter and controllers | Add new providers |
| **Error Handling** | Manual in controllers | Built-in success/failure handlers |

## File Structure

```
src/main/java/com/example/serialprovider/
├── auth/
│   ├── AuthenticationState.java           # Enum for tracking progress
│   ├── AuthenticationSession.java         # Session state management
│   ├── MultiFactorAuthenticationManager.java  # Routes tokens to providers
│   ├── provider/                          # Authentication providers
│   │   ├── PasswordAuthenticationProvider.java
│   │   ├── OtpAuthenticationProvider.java
│   │   └── OnboardingAuthenticationProvider.java
│   ├── token/                            # Custom authentication tokens
│   │   ├── UsernamePasswordAuthenticationToken.java
│   │   ├── OtpAuthenticationToken.java
│   │   └── OnboardingAuthenticationToken.java
│   └── handler/                          # Success/failure handlers
│       ├── MultiFactorAuthenticationSuccessHandler.java
│       └── MultiFactorAuthenticationFailureHandler.java
├── config/
│   └── SecurityConfig.java               # Spring Security configuration
├── controller/
│   ├── AuthController.java              # Authentication endpoints
│   ├── ProtectedController.java         # Protected API endpoints
│   └── ViewController.java              # Page routing
└── service/                             # Business logic services
    ├── UserCredentialsService.java
    ├── OtpService.java
    └── OnboardingService.java
```

## Dynamic Configuration Features

### Settings Management API

The system provides REST endpoints for managing authentication settings:

#### Get Current Settings
```bash
curl -X GET http://localhost:8080/admin/auth-settings/status
```
Response:
```json
{
  "otpEnabled": true,
  "onboardingEnabled": true, 
  "flowDescription": "Password + OTP + Onboarding (3 steps)",
  "settingName": "DEFAULT"
}
```

#### Update Settings
```bash
curl -X POST http://localhost:8080/admin/auth-settings/update \
  -H "Content-Type: application/json" \
  -d '{"otpEnabled": false, "onboardingEnabled": true}'
```

#### Quick Toggle Commands
```bash
# Disable OTP (Password + Onboarding only)
curl -X POST http://localhost:8080/admin/auth-settings/disable-otp

# Enable OTP  
curl -X POST http://localhost:8080/admin/auth-settings/enable-otp

# Disable Onboarding
curl -X POST http://localhost:8080/admin/auth-settings/disable-onboarding

# Enable Onboarding
curl -X POST http://localhost:8080/admin/auth-settings/enable-onboarding
```

### Real-time Flow Adaptation

The authentication flow adapts immediately to settings changes:

1. **Settings changed** → Cache invalidated
2. **Next request** → Providers check new settings  
3. **Authentication flow** → Routes based on enabled steps
4. **Success handler** → Returns appropriate next step

### Provider Behavior Based on Settings

#### PasswordAuthenticationProvider
- Always executes (password required)
- Generates OTP only if `otpEnabled=true`
- Marks as fully authenticated if no other steps enabled

#### OtpAuthenticationProvider  
- Throws error if `otpEnabled=false`
- Marks as fully authenticated if `onboardingEnabled=false`

#### OnboardingAuthenticationProvider
- Throws error if `onboardingEnabled=false` 
- Validates previous step based on OTP setting
- Always marks as fully authenticated when successful

## Usage Examples

### Testing Different Flow Configurations

#### Test Flow 1: Password Only
```bash
# Configure password-only authentication
curl -X POST http://localhost:8080/admin/auth-settings/update \
  -H "Content-Type: application/json" \
  -d '{"otpEnabled": false, "onboardingEnabled": false}'

# Test login - should go directly to dashboard
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' \
  --cookie-jar cookies.txt

# Response: {"nextStep": "dashboard", "message": "Authentication completed successfully!"}
```

#### Test Flow 2: Password + OTP
```bash
# Configure password + OTP authentication
curl -X POST http://localhost:8080/admin/auth-settings/update \
  -H "Content-Type: application/json" \
  -d '{"otpEnabled": true, "onboardingEnabled": false}'

# Test login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' \
  --cookie-jar cookies.txt

# Response: {"nextStep": "otp", "message": "Credentials verified. OTP sent."}

# Submit OTP (check console for code)
curl -X POST http://localhost:8080/auth/otp \
  -H "Content-Type: application/json" \
  -d '{"otp":"123456"}' \
  --cookie cookies.txt

# Response: {"nextStep": "dashboard", "message": "Authentication completed successfully!"}
```

### Testing with curl

```bash
# Step 1: Login
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' \
  --cookie-jar cookies.txt

# Step 2: OTP (check console for OTP code)
curl -X POST http://localhost:8080/auth/otp \
  -H "Content-Type: application/json" \
  -d '{"otp":"123456"}' \
  --cookie cookies.txt

# Step 3: Onboarding  
curl -X POST http://localhost:8080/auth/onboard \
  -H "Content-Type: application/json" \
  -d '{"fullName":"John Doe","email":"john@example.com"}' \
  --cookie cookies.txt

# Access protected resource
curl -X GET http://localhost:8080/api/dashboard \
  --cookie cookies.txt
```

This architecture provides a robust, maintainable, and secure 3-factor authentication system that follows Spring Security best practices.