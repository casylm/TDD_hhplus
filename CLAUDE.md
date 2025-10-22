# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a TDD (Test-Driven Development) exercise project for implementing a point management system in Java with Spring Boot. The project is named "hhplus-tdd-jvm" and is part of the HangHae99 bootcamp curriculum (Week 1).

## Build System

- **Build Tool**: Gradle with Kotlin DSL
- **Java Version**: 17
- **Spring Boot Version**: 3.2.0
- **Spring Cloud Dependencies**: 2023.0.0

## Common Commands

### Build and Run
```bash
./gradlew build           # Build the project
./gradlew bootRun         # Run the Spring Boot application
./gradlew bootJar         # Create executable JAR (located in build/libs/)
```

### Testing
```bash
./gradlew test            # Run all tests (ignoreFailures = true in config)
./gradlew test --tests ClassName                    # Run specific test class
./gradlew test --tests ClassName.testMethodName     # Run specific test method
./gradlew jacocoTestReport                          # Generate test coverage report
```

### Code Quality
```bash
./gradlew clean           # Clean build artifacts
```

## Architecture and Structure

### Package Organization
```
io.hhplus.tdd/
├── point/                 # Point domain layer
│   ├── PointController    # REST API endpoints for point operations
│   ├── UserPoint          # Record: user point data (id, point, updateMillis)
│   ├── PointHistory       # Record: point transaction history
│   └── TransactionType    # Enum: CHARGE, USE
├── database/              # In-memory database layer (DO NOT MODIFY)
│   ├── UserPointTable     # Thread-safe point storage with throttling (200-300ms)
│   └── PointHistoryTable  # Transaction history storage with throttling
├── ApiControllerAdvice    # Global exception handler
├── ErrorResponse          # Error response DTO
└── TddApplication         # Spring Boot main class
```

### Key Constraints

**DO NOT MODIFY** the `database` package classes (`UserPointTable` and `PointHistoryTable`):
- These are provided database implementations that simulate latency (200-300ms random delays)
- Only use their public APIs: `selectById()`, `insertOrUpdate()`, `insert()`, `selectAllByUserId()`
- The tables use in-memory storage (HashMap and ArrayList)

### Implementation Requirements

The `PointController` has 4 TODO endpoints that need implementation:

1. **GET /point/{id}** - Query user's point balance
2. **GET /point/{id}/histories** - Query user's point transaction history
3. **PATCH /point/{id}/charge** - Charge points (add to balance)
4. **PATCH /point/{id}/use** - Use points (deduct from balance)

### Domain Model

- **UserPoint**: Immutable record with `id`, `point` (current balance), `updateMillis` (timestamp)
- **PointHistory**: Immutable record with `id`, `userId`, `amount`, `type` (CHARGE/USE), `updateMillis`
- **TransactionType**: Enum with `CHARGE` (charge points) and `USE` (use points)

### Error Handling

Global exception handling is configured in `ApiControllerAdvice`:
- All exceptions return HTTP 500 with `ErrorResponse` containing code "500" and message "에러가 발생했습니다."
- Custom validation logic and specific error handling should be implemented as needed

## Development Approach

This is a TDD project. Follow these principles:

1. **Write tests first** before implementing features
2. Implement **service layer** logic between controller and database tables
3. Handle **edge cases**: negative amounts, insufficient balance, concurrent access
4. Consider **validation**: user existence, amount ranges, business rules
5. Use the provided database tables' throttling as a realistic latency simulation

## Testing Strategy

- Use **JUnit 5** (Jupiter) - already configured via `useJUnitPlatform()`
- Spring Boot Test dependencies are available via `spring-boot-starter-test`
- Test coverage reporting available via **JaCoCo** (toolVersion 0.8.7)
- Tests are configured with `ignoreFailures = true` to allow partial test runs

## Additional Notes

- Project uses **Lombok** for reducing boilerplate (available at compile time)
- Spring Boot Configuration Processor enabled for type-safe configuration properties
- The project name in settings is "hhplus-tdd-jvm" but directory may differ
- No database persistence layer - all data is in-memory
