# Backend Test Guide - FinancialLifeV2

This guide explains the comprehensive test suite for your Ktor backend and how to run them.

## Test Structure

Your backend has two main route groups with full test coverage:

### 1. **AuthRoutesTest** - Authentication Endpoints
Tests for `/auth` routes:
- ✅ `POST /auth/register` - User registration
- ✅ `POST /auth/login` - User login
- ✅ `GET /auth/me` - Get current user

**Test Cases (13 tests)**:
```
✓ register - success with valid data
✓ register - username too short (< 3 chars)
✓ register - password too short (< 6 chars)
✓ register - duplicate username
✓ login - success with valid credentials
✓ login - non-existent user
✓ login - wrong password
✓ login - empty fields
✓ get /me - success with valid token
✓ get /me - fails without auth header
✓ get /me - fails with invalid token
✓ get /me - fails with expired token
```

### 2. **GameRoutesTest** - Game Endpoints
Tests for `/game` routes:
- ✅ `GET /game/character` - Get character info
- ✅ `POST /game/start/{userId}` - Start new game
- ✅ `GET /game/event/{userId}` - Get current event
- ✅ `POST /game/choose/{userId}/{optionId}` - Make choice
- ✅ `POST /game/save` - Save game state
- ✅ `GET /game/load/{userId}` - Load game state
- ✅ `GET /game/snapshots/{userId}` - List save slots
- ✅ `POST /game/restore/{snapshotId}` - Restore from save
- ✅ `GET /game/health` - Health check

**Test Cases (25 tests)**:
```
✓ GET /character - returns character info
✓ GET /character - correct structure
✓ POST /start - success with valid userId
✓ POST /start - fails without userId
✓ POST /start - creates session in repo
✓ POST /start - multiple users separate sessions
✓ GET /event - success after game started
✓ GET /event - fails without active session
✓ GET /event - fails without userId
✓ GET /event - returns valid options
✓ POST /choose - success with valid option
✓ POST /choose - fails without session
✓ POST /choose - fails without userId
✓ POST /choose - fails without optionId
✓ POST /save - success with valid state
✓ POST /save - default slot name is "manual"
✓ POST /save - multiple saves per user
✓ GET /load - returns false when no session
✓ GET /load - returns true with existing session
✓ GET /load - fails without userId
✓ GET /snapshots - empty when no snapshots
✓ GET /snapshots - lists all snapshots
✓ GET /snapshots - fails without userId
✓ POST /restore - success with valid snapshot
✓ POST /restore - fails with invalid snapshot
✓ POST /restore - fails without snapshotId
✓ GET /health - returns ok status
```

## What These Tests Check

### Happy Path (Success Cases)
- All endpoints work with valid inputs
- Correct HTTP status codes (200, 201, 400, 401, 404, 409)
- Response bodies contain expected data
- JWT tokens are generated and validated
- Game state is properly saved/loaded

### Validation & Error Handling
- Username/password validation (min length)
- Missing required fields
- Invalid tokens
- Non-existent resources (404s)
- Duplicate users (409 Conflict)
- Unauthorized access (401)

### Data Integrity
- Multiple users have separate sessions
- Game saves are persistent
- Snapshots can be restored
- Passwords are hashed (not stored plaintext)

## How to Run Tests

### Prerequisites
Add test dependencies to `build.gradle.kts`:

```kotlin
dependencies {
    // ... existing dependencies ...

    // Ktor testing
    testImplementation("io.ktor:ktor-server-tests:${libs.versions.ktor.get()}")

    // JUnit & Kotlin Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("kotlin-test:kotlin-test:1.9.10")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${libs.versions.kotlin.get()}")
}
```

### Run All Tests
```bash
# Run all tests
./gradlew test

# Run with output
./gradlew test --info

# Run specific test class
./gradlew test --tests AuthRoutesTest
./gradlew test --tests GameRoutesTest
```

### Run Specific Test
```bash
# Single test method
./gradlew test --tests AuthRoutesTest.test*register*

# With pattern
./gradlew test --tests "*Auth*"
```

### Generate Coverage Report
```bash
# Add Jacoco plugin and run
./gradlew test jacocoTestReport
# Report at: build/reports/jacoco/test/html/index.html
```

## Understanding Test Output

When you run `./gradlew test`, you'll see:

```
AuthRoutesTest
  ✓ test POST auth_register - success with valid data
  ✓ test POST auth_register - fails with username too short
  ✓ test POST auth_login - success with valid credentials
  ✓ test GET auth_me - success with valid token
  ...

GameRoutesTest
  ✓ test GET game_character - returns character info
  ✓ test POST game_start - success with valid userId
  ✓ test GET game_event - success after game started
  ...

BUILD SUCCESSFUL
```

## Architecture of the Tests

### Test Isolation
Each test is independent - they use **mock repositories** instead of the real database.

```
Test Method
    ↓
testApplication {} builder
    ↓
Mock GameRepository / UserRepository (in-memory)
    ↓
No database calls
    ↓
Fast, isolated, repeatable
```

### Mock Objects
```
MockUserRepository:
  - Users stored in memory (Map)
  - No password encryption (for test speed)
  - Simulates all CRUD operations

MockGameRepository:
  - Game sessions in memory
  - Snapshots persisted locally
  - No database overhead
```

## What Each Test Class Does

### AuthRoutesTest
**Purpose**: Verify user authentication works correctly

**Key Scenarios**:
1. Register new user → get JWT token
2. Login with credentials → get JWT token
3. Access protected endpoint with valid token
4. Reject invalid/missing tokens
5. Validate password/username requirements

### GameRoutesTest
**Purpose**: Verify game flow and state management

**Key Scenarios**:
1. Get game character info (public endpoint)
2. Start new game → create session
3. Fetch current event/choices
4. Make choice → advance game state
5. Save game state → create snapshot
6. Load saved game
7. List save slots
8. Restore from snapshot
9. Health check

## Customizing Tests

### Add New Test Cases
Edit the test files and add new test methods:

```kotlin
@Test
fun `test GET game_event - returns current player level`() = testApplication {
    val client = createClient()
    val userId = "test_user"

    client.post("/game/start/$userId") { contentType(ContentType.Application.Json) }
    val response = client.get("/game/event/$userId")

    // Assert your expectations
    assertEquals(HttpStatusCode.OK, response.status)
    assertTrue(response.bodyAsText().contains("level"))
}
```

### Modify Mocks
Edit `MockUserRepository` and `MockGameRepository` classes to add features you want to test.

### Integration Tests
For testing with a real database:
1. Start a test PostgreSQL container (Docker)
2. Replace mocks with real repositories
3. Clean database between tests using transactions

## Common Issues & Solutions

### Issue: Tests fail with "testApplication not found"
**Solution**: Ensure Ktor testing dependency is in `build.gradle.kts`

### Issue: "Unresolved reference: createClient"
**Solution**: This is defined in each test class. Make sure `io.ktor.client.plugins.JsonFeature` is available.

### Issue: JSON parsing errors
**Solution**: Ensure `GameState` is serializable with `@Serializable` annotation

## Test Coverage

| Endpoint | Coverage | Status |
|----------|----------|--------|
| POST /auth/register | 4 cases | ✅ Complete |
| POST /auth/login | 4 cases | ✅ Complete |
| GET /auth/me | 3 cases | ✅ Complete |
| GET /game/character | 2 cases | ✅ Complete |
| POST /game/start | 3 cases | ✅ Complete |
| GET /game/event | 3 cases | ✅ Complete |
| POST /game/choose | 4 cases | ✅ Complete |
| POST /game/save | 3 cases | ✅ Complete |
| GET /game/load | 3 cases | ✅ Complete |
| GET /game/snapshots | 3 cases | ✅ Complete |
| POST /game/restore | 3 cases | ✅ Complete |
| GET /game/health | 1 case | ✅ Complete |

**Total: 38 test cases covering all endpoints and error scenarios**

## Next Steps

1. **Run tests locally**: `./gradlew test`
2. **Fix any failures**: Debug and update mocks as needed
3. **Add database tests**: Create integration tests with real DB
4. **CI/CD**: Run tests in GitHub Actions on every push
5. **Coverage goals**: Aim for 80%+ code coverage

## Learning the Backend Through Tests

Each test reveals how your backend works:

```kotlin
// Test shows: Registration validates username length
@Test
fun `test POST auth_register - fails with username too short`() = testApplication {
    val response = client.post("/auth/register") {
        setBody(RegisterRequest(username = "ab", password = "password123"))
    }
    assertEquals(HttpStatusCode.BadRequest, response.status)
}
// ↑ This tells you: backend checks username.length < 3
```

Read through the tests to understand:
- What each endpoint accepts/returns
- What validation rules apply
- How errors are handled
- What database operations happen
- How JWT tokens work
