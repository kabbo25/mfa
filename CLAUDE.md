# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.5.4 application implementing a **configurable multi-factor authentication system** using Java 21. The system supports dynamic authentication flows with 1-3 authentication factors:

- **Password Only** (1 step)
- **Password + OTP** (2 steps) 
- **Password + Onboarding** (2 steps)
- **Password + OTP + Onboarding** (3 steps)

The architecture uses Spring Security's Authentication Provider Pattern with custom providers for each authentication step, session-based state management, and a REST API for dynamic configuration.

## Build System & Commands

This project uses Maven with the Maven wrapper (`mvnw`/`mvnw.cmd`):

### Essential Commands
- **Build the project**: `./mvnw clean compile`
- **Run tests**: `./mvnw test`
- **Run the application**: `./mvnw spring-boot:run`
- **Package the application**: `./mvnw clean package`
- **Run a single test**: `./mvnw test -Dtest=SerialProviderApplicationTests`

### Development Commands
- **Clean build**: `./mvnw clean install`
- **Skip tests during build**: `./mvnw clean package -DskipTests`
- **Run with specific profile**: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`

### Database Access
- **H2 Console**: Access at `http://localhost:8080/h2-console` when running
  - JDBC URL: `jdbc:h2:mem:testdb`
  - Username: `sa`
  - Password: `password`

## Architecture Overview

### Multi-Factor Authentication Flow
The system implements a **provider-based authentication architecture** where each authentication step is handled by a dedicated Spring Security AuthenticationProvider:

1. **PasswordAuthenticationProvider**: Validates credentials, generates OTP, updates session to STEP_1_COMPLETED
2. **OtpAuthenticationProvider**: Validates OTP code, updates session to STEP_2_COMPLETED  
3. **OnboardingAuthenticationProvider**: Validates profile data, marks as FULLY_AUTHENTICATED

### Key Components

#### Authentication Tokens (`src/main/java/com/example/serialprovider/auth/token/`)
- `UsernamePasswordAuthenticationToken`: Step 1 (credentials)
- `OtpAuthenticationToken`: Step 2 (OTP verification)
- `OnboardingAuthenticationToken`: Step 3 (profile completion)

#### Authentication Providers (`src/main/java/com/example/serialprovider/auth/provider/`)
- Each provider handles one authentication step with built-in step enforcement
- Providers check settings dynamically to determine enabled authentication factors
- Security validation prevents step jumping between authentication phases

#### Session Management (`src/main/java/com/example/serialprovider/auth/`)
- `AuthenticationSession`: Session-scoped bean tracking authentication progress
- `AuthenticationState`: Enum defining authentication states (UNAUTHENTICATED → FULLY_AUTHENTICATED)
- 30-minute session timeout with automatic cleanup

#### Configuration API (`src/main/java/com/example/serialprovider/controller/AdminController.java`)
- REST endpoints for enabling/disabling authentication factors
- Real-time flow adaptation without application restart
- Settings stored in H2 database with caching

## Key Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Security  
- Spring Boot Starter Data JPA
- H2 Database (in-memory for development)
- Spring Boot Starter Test
- Spring Security Test

## Package Structure

```
src/main/java/com/example/serialprovider/
├── auth/                          # Authentication framework
│   ├── AuthenticationState.java   # State tracking enum
│   ├── AuthenticationSession.java # Session management
│   ├── MultiFactorAuthenticationManager.java # Routes tokens to providers
│   ├── provider/                  # Authentication providers
│   ├── token/                     # Custom authentication tokens
│   └── handler/                   # Success/failure handlers
├── config/                        # Spring configuration
│   ├── SecurityConfig.java        # Security configuration
│   ├── CacheConfig.java          # Caching configuration
│   └── DataInitializer.java      # Database initialization
├── controller/                    # REST endpoints
│   ├── AuthController.java       # Authentication endpoints
│   ├── AdminController.java      # Configuration API
│   ├── ProtectedController.java  # Protected resources
│   └── ViewController.java       # Page routing
├── entity/                        # JPA entities
├── repository/                    # Data repositories
└── service/                       # Business logic
```

## Testing Authentication Flows

### Configuration API
```bash
# Check current settings
curl -X GET http://localhost:8080/admin/auth-settings/status

# Enable/disable authentication factors
curl -X POST http://localhost:8080/admin/auth-settings/enable-otp
curl -X POST http://localhost:8080/admin/auth-settings/disable-onboarding
```

### Test Complete 3-Factor Flow
```bash
# Step 1: Login (generates OTP in console)
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"password123"}' \
  --cookie-jar cookies.txt

# Step 2: OTP verification
curl -X POST http://localhost:8080/auth/otp \
  -H "Content-Type: application/json" \
  -d '{"otp":"123456"}' \
  --cookie cookies.txt

# Step 3: Profile onboarding
curl -X POST http://localhost:8080/auth/onboard \
  -H "Content-Type: application/json" \
  -d '{"fullName":"John Doe","email":"john@example.com"}' \
  --cookie cookies.txt

# Access protected resource
curl -X GET http://localhost:8080/api/dashboard --cookie cookies.txt
```

## Security Features

- **Step Jumping Prevention**: Each provider validates previous authentication steps
- **Session Timeout**: 30-minute automatic expiry with cleanup
- **State Validation**: Enforced progression through authentication states
- **Failure Handling**: Critical failures reset entire authentication session
- **Authority-Based Access**: API endpoints require `FULLY_AUTHENTICATED` authority

## Development Notes

- H2 database recreated on each startup (DDL auto: create-drop)
- Session data stored in HTTP sessions with custom cookie name
- OTP codes displayed in console output during development
- Static HTML pages served from `src/main/resources/static/`
- CSRF disabled for API endpoints, form login/basic auth disabled
- Admin endpoints use `.permitAll()` for demo purposes (should be secured in production)

## Architecture Documentation

Detailed architecture documentation is available in `architecture.md`, including:
- Complete authentication flow diagrams
- Provider implementation details
- Dynamic configuration mechanics
- Security model and session management
- Comparison with alternative approaches