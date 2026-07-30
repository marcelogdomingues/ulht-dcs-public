# Issuer App Implementation Guide

This document explains what has been created and what backend components are needed to fully implement the issuer app functionality.

## What Has Been Created ✅

### Frontend App (Flutter)
- **Complete Flutter app structure** in `/mobile-apps/issuer-app/`
- **Models**: Session and RegisteredStudent data models
- **Services**: API service with all endpoint definitions
- **Providers**: SessionProvider for state management
- **Screens**:
  - Home screen with session list
  - Create session screen
  - Session detail screen with QR code
  - Registered students screen
- **UI**: Material Design 3 theme with orange color scheme

### Documentation
- README.md with setup instructions
- Updated main mobile-apps README.md

## What Needs to Be Implemented 🔨

### Backend API Endpoints

The issuer app expects the following endpoints to be implemented in the **Credential Service**:

#### 1. Session Management Endpoints

**POST** `/api/v1/issuer/sessions`
- Create a new conference session
- Request body:
  ```json
  {
    "title": "Keynote: Future of Technology",
    "description": "Optional description",
    "conferenceName": "International Tech Conference 2024",
    "startTime": "2024-10-15T10:00:00Z",
    "endTime": "2024-10-15T11:30:00Z",
    "location": "Main Hall, Room 101"
  }
  ```
- Response: Session object with generated ID and QR code URL

**GET** `/api/v1/issuer/sessions`
- Get all sessions
- Response: Array of Session objects

**GET** `/api/v1/issuer/sessions/{id}`
- Get a specific session by ID
- Response: Session object

**PUT** `/api/v1/issuer/sessions/{id}`
- Update a session
- Request body: Session object
- Response: Updated Session object

**DELETE** `/api/v1/issuer/sessions/{id}`
- Delete a session
- Response: 200 OK or 204 No Content

**POST** `/api/v1/issuer/sessions/{id}/qr-code`
- Generate or regenerate QR code for a session
- Response:
  ```json
  {
    "qrCodeUrl": "openid4vci://credential-offer?session_id=xxx&..."
  }
  ```

#### 2. Registration Endpoints

**GET** `/api/v1/issuer/sessions/{id}/registrations`
- Get all registered students for a session
- Response: Array of RegisteredStudent objects

**POST** `/api/v1/issuer/sessions/{id}/issue`
- Issue a session credential to a student
- Request body:
  ```json
  {
    "studentId": "a12345678"
  }
  ```
- Response: Credential offer URL

**GET** `/api/v1/issuer/sessions/{id}/check-registration/{studentId}`
- Check if a student is registered for a session
- Response:
  ```json
  {
    "isRegistered": true
  }
  ```

### Backend Components Needed

#### 1. Domain Models
Create in `credential-service/src/main/java/pt/ulusofona/ulht/credential/domain/issuer/`:

- `Session.java` - Represents a conference session
- `RegisteredStudent.java` - Represents a student registered for a session
- `SessionCredentialRequest.java` - Request to issue session credential

#### 2. Repository/Storage Layer
Create in `credential-service/src/main/java/pt/ulusofona/ulht/credential/repository/issuer/`:

- `SessionRepository.java` - Interface for session storage
- `SessionRepositoryImpl.java` - In-memory implementation (or database-backed)

**Note**: For initial implementation, use in-memory storage with a `ConcurrentHashMap`. For production, use a database (PostgreSQL, MongoDB, etc.).

#### 3. Service Layer
Create in `credential-service/src/main/java/pt/ulusofona/ulht/credential/service/issuer/`:

- `SessionService.java` - Business logic for session management
- `SessionCredentialService.java` - Logic for issuing session credentials

#### 4. Controller
Create in `credential-service/src/main/java/pt/ulusofona/ulht/credential/controller/`:

- `IssuerController.java` - REST endpoints for issuer functionality

### Session Credential Type

You'll need to define a new credential template for session credentials in `application.yml`:

```yaml
credential-templates:
  templates:
    - type: ConferenceSessionCredential
      displayName: Conference Session Credential
      enabled: true
      waltidConfigId: "ConferenceSessionCredential"
      priority: 25
      fields:
        - name: sessionId
          source: sessionId
        - name: sessionTitle
          source: sessionTitle
        - name: conferenceName
          source: conferenceName
        - name: studentId
          source: studentId
        - name: registrationDate
          source: registrationDate
```

## Implementation Approach

### Phase 1: Basic Session Management
1. Create domain models (Session, RegisteredStudent)
2. Create in-memory repository
3. Create SessionService with basic CRUD operations
4. Create IssuerController with session endpoints
5. Test with issuer app

### Phase 2: QR Code Generation
1. Generate QR codes with session information
2. QR code should contain: `openid4vci://credential-offer?session_id={id}&...`
3. Or use a URL that the student app can call: `https://localhost:8086/api/v1/issuer/sessions/{id}/register?studentId={studentId}`

### Phase 3: Credential Issuance
1. When student scans QR code, student app calls registration endpoint
2. Backend issues a session credential via the existing credential issuance flow
3. Store registration record
4. Return credential offer URL to student

### Phase 4: Integration
1. Update student app to handle issuer QR codes
2. Update verifier app to check for session credentials
3. Test complete workflow

## QR Code Format

The QR code should contain either:

**Option 1: Direct URL (Simpler)**
```
https://localhost:8086/api/v1/issuer/sessions/{sessionId}/register?studentId={studentId}
```

**Option 2: OpenID4VCI URL (Standards-based)**
```
openid4vci://credential-offer?issuer=https://localhost:8086&session_id={sessionId}
```

For now, Option 1 is simpler to implement. Option 2 follows standards but requires more setup.

## Database Schema (Future)

For production, consider this database schema:

```sql
CREATE TABLE sessions (
    id VARCHAR(255) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    conference_name VARCHAR(255) NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    location VARCHAR(255),
    qr_code_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE session_registrations (
    id VARCHAR(255) PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    student_id VARCHAR(255) NOT NULL,
    student_name VARCHAR(255),
    email VARCHAR(255),
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    credential_id VARCHAR(255),
    FOREIGN KEY (session_id) REFERENCES sessions(id),
    UNIQUE(session_id, student_id)
);

CREATE INDEX idx_session_registrations_session ON session_registrations(session_id);
CREATE INDEX idx_session_registrations_student ON session_registrations(student_id);
```

## Testing Workflow

Once backend is implemented:

1. **Start backend services**
2. **Run issuer app**
   - Create a session
   - Generate QR code
3. **Run student app**
   - Scan QR code from issuer app
   - Request session credential
4. **Verify**
   - Check registered students in issuer app
   - Verify student has session credential in wallet

## Next Steps

1. **Implement backend endpoints** following the structure above
2. **Update student app** to handle issuer QR codes (TODO #6)
3. **Update verifier app** to check session credentials (TODO #7)
4. **Test complete workflow**
5. **Replace in-memory storage** with database for production

## Questions?

If you need help implementing any of these components, the patterns can be found in:
- `VerifierController.java` - Example REST controller
- `CredentialWorkflowConsumer.java` - Example credential issuance flow
- Existing service implementations for reference

